# AnticipaPYME · Factoraje de facturas electrónicas para PYME

## 1. De qué se trata

Las PYME que le venden a empresas grandes (supermercados, fábricas, cadenas) esperan varios días para cobrar, pero necesitan el dinero hoy para materia prima y planilla. AnticipaPYME es una API para que el banco le adelante a la PYME el dinero de sus facturas electrónicas (DTE) pendientes de cobro, a cambio de un descuento, y después le cobre a la empresa grande que las debe. El banco no evalúa a la PYME sino al pagador, y la factura electrónica permite verificar cada documento de forma ágil, lo que hace viable el factoraje incluso para facturas pequeñas.

## 2. Quién lo usa

| Rol | Quién es | Qué puede hacer |
| --- | --- | --- |
| ADMIN | jefatura del área de factoraje | configura `sector_economico` y `categoria_riesgo` (tasas y plazos); aprueba, suspende y renueva cada `cupo_pagador`; gestiona usuarios y ejecutivos; ejecuta "marcar vencidas" (R15) |
| ASESOR | ejecutivo de banca empresas | registra empresas; ejecuta el anticipo de facturas (la operación principal); registra los cobros; consulta operaciones |
| CLIENTE | una empresa | como cedente: registra sus facturas, solicita anticipos y consulta sus operaciones y desembolsos. Como pagadora: confirma las facturas que le emitieron y consulta lo que debe |

El campo `rol.nombre` almacena el nombre del rol (ADMIN, ASESOR o CLIENTE) sin ningún prefijo. Cuando se carga el usuario, el `UserDetailsService` le agrega `ROLE_` al nombre para crear la autoridad correspondiente. Esto es necesario porque, al usar `hasRole("CLIENTE")`, Spring Security busca una autoridad llamada `ROLE_CLIENTE`. Es decir, Spring Security no agrega el prefijo automáticamente al cargar al usuario, sino que lo da por hecho al comprobar sus permisos.

Cada empresa está ligada a su usuario por `empresa.usuario_id`, y cada ejecutivo por `ejecutivo.usuario_id`. Así un CLIENTE solo ve las facturas donde su empresa es cedente o pagadora, y las operaciones donde es la cedente.

Una misma empresa puede ser cedente en unas facturas y pagadora en otras: el rol lo define la factura, con `empresa_cedente_id` y `empresa_pagadora_id`.

## 3. Las entidades

| Entidad | Qué representa |
| --- | --- |
| Empresa | toda empresa del sistema, sea cedente, pagadora o ambas, con su NIT, su nombre legal y su cuenta bancaria para recibir desembolsos |
| Ejecutivo | el empleado del banco que ejecuta los anticipos y registra los cobros |
| SectorEconomico | catálogo de actividades económicas con que se clasifica cada empresa (ej. COMERCIO) |
| CategoriaRiesgo | catálogo de categorías de riesgo de los pagadores, cada una con su tasa de descuento, su tasa de mora y el plazo máximo anticipable |
| Factura | una factura electrónica (DTE) que una empresa cedente le emitió a una empresa pagadora; recorre los estados REGISTRADA, CONFIRMADA, CEDIDA, COBRADA o pasa a VENCIDA si se cedió y el pagador no pagó al vencimiento, o a INVALIDADA si Hacienda la anuló |
| CupoPagador | el límite que el banco le aprueba a una empresa pagadora: cuánto acepta tener anticipado en facturas suyas al mismo tiempo; puede estar ACTIVO, SUSPENDIDO o VENCIDO |
| OperacionFactoraje | el acuerdo de un anticipo: qué facturas de una cedente se anticiparon, con qué tasas y por cuánto; está VIGENTE, pasa a EN_MORA si alguna factura queda VENCIDA y a LIQUIDADA cuando todas están COBRADAS |
| DetalleOperacion | cada factura incluida en una operación, con su propio descuento |
| Desembolso | el pago a la empresa cedente por una operación, con su número de comprobante y la cuenta de destino de ese día |
| Cobro | el pago que hace la empresa pagadora por una factura cedida, desglosado en capital y mora |
| BitacoraOperacion | quién hizo cada operación (anticipo o cobro), cuándo y cómo terminó |
| Usuario | la cuenta de acceso al sistema |
| Rol | catálogo de roles del sistema: ADMIN, ASESOR y CLIENTE |
| UsuarioRol | la asignación de roles a cada usuario (resuelve el muchos a muchos entre usuario y rol) |

## 4. La operación principal: anticipar facturas

Entrada: la empresa cedente (`empresa_cedente_id`), la lista de facturas a anticipar (`factura_id`) y el ejecutivo que la ejecuta (el usuario autenticado).

Validaciones previas: R1, R2, R4, R7, R8 y R9.

Los cambios, en orden:

1. Inserta la operación en `operacion_factoraje`, en estado VIGENTE, con `tasa_descuento_aplicada` y `tasa_mora_aplicada` copiadas de la `categoria_riesgo` del cupo.
2. Inserta un registro en `detalle_operacion` por cada factura, con su `monto_descuento` (R10).
3. Guarda los totales de la operación: `monto_nominal_total` (R11) y `monto_descuento_total` (R12).
4. Marca cada factura como CEDIDA en `factura`.
5. Descuenta el total del `monto_disponible` en `cupo_pagador` (R13).
6. Inserta el pago a la empresa cedente en `desembolso`, con `cuenta_destino` copiada de `empresa.cuenta_bancaria`.
7. Registra la operación en `bitacora_operacion` con resultado EXITOSO.

Si falla a la mitad: se deshace todo lo que se hizo en esa transacción. No queda la operación ni sus detalles, las facturas siguen CONFIRMADAS, el `monto_disponible` del cupo queda como estaba y no hay desembolso. El intento se registra en `bitacora_operacion` como FALLIDO, en una transacción aparte para que no se pierda con el rollback.

Salida: el comprobante, con el `numero_operacion`, el `numero_comprobante` del desembolso, cada factura con su descuento, el monto nominal total, el descuento total y el monto desembolsado. El monto desembolsado no se guarda: es `monto_nominal_total − monto_descuento_total`.

## 5. Las reglas de negocio

| # | Regla | Dónde se cumple | Si no se cumple |
| --- | --- | --- | --- |
| R1 | Solo se anticipa una `factura` en estado CONFIRMADA | servicio, al anticipar | se rechaza: "La factura X no está CONFIRMADA" |
| R2 | Solo se anticipa contra un `cupo_pagador` en estado ACTIVO cuya `fecha_vencimiento` no haya pasado | servicio, al anticipar | se rechaza: "El cupo X no está ACTIVO" |
| R3 | Una empresa pagadora solo puede tener un `cupo_pagador` ACTIVO a la vez | servicio, al aprobar un cupo | se rechaza: "La empresa X ya tiene un cupo ACTIVO" |
| R4 | Una factura no se cede dos veces: el `codigo_generacion` no se repite en `factura`, y el `factura_id` no se repite en `detalle_operacion` | tabla (UNIQUE) | la base rechaza el INSERT |
| R5 | El `nit` de la empresa tiene el formato 0000-000000-000-0 y no se repite | tabla (UNIQUE) + servicio (formato) | se rechaza: "Ya existe una empresa con ese NIT" |
| R6 | Todos los montos de dinero son mayores que cero, salvo `monto_mora`, que es mayor o igual que cero; el `monto_disponible` está entre 0 y el `monto_aprobado` | tabla (CHECK) | la base rechaza el INSERT |
| R7 | La suma de los `monto_total` de las facturas a anticipar no supera el `monto_disponible` del cupo | servicio, al anticipar | se rechaza: "El monto supera el cupo disponible de $X" |
| R8 | Todas las facturas de una operación tienen `empresa_cedente_id` a la empresa cedente de la operación y como `empresa_pagadora_id` a la empresa del cupo; y la cedente tiene `cuenta_bancaria` registrada | servicio, al anticipar | se rechaza: "La factura X no pertenece a la empresa cedente" |
| R9 | A cada factura le faltan entre 15 días y el `plazo_maximo_dias` de la `categoria_riesgo` del cupo para su `fecha_vencimiento` | servicio, al anticipar | se rechaza: "La factura X vence en menos de 15 días" |
| R10 | `detalle_operacion.monto_descuento` = `monto_total` de la factura × `tasa_descuento_aplicada` × días hasta su `fecha_vencimiento` / 360, redondeado a 2 decimales | servicio, en la operación principal | se deshace toda la operación |
| R11 | `monto_nominal_total` = la suma de los `monto_total` de las facturas de sus `detalle_operacion` | servicio, en la operación principal | se deshace toda la operación |
| R12 | `monto_descuento_total` = la suma de los `monto_descuento` de sus `detalle_operacion` | servicio, en la operación principal | se deshace toda la operación |
| R13 | `cupo_pagador.monto_disponible` = `monto_aprobado` − la suma de los `monto_total` de las facturas CEDIDAS o VENCIDAS de las operaciones respaldadas por ese cupo | servicio, al anticipar y al cobrar | se deshace la operación |
| R14 | Solo se cobra una factura CEDIDA o VENCIDA, y sin pagos parciales: `monto_capital` = `monto_total` de la factura, y `monto_mora` = `monto_total` × `tasa_mora_aplicada` de su operación × días de atraso / 360 (cero si se paga a tiempo) | servicio, al cobrar | se rechaza: "El monto no cubre la factura más su mora" |
| R15 | Una factura CEDIDA cuya `fecha_vencimiento` ya pasó pasa a VENCIDA y su operación a EN_MORA; un `cupo_pagador` ACTIVO cuya `fecha_vencimiento` ya pasó pasa a VENCIDO | servicio, operación "marcar vencidas" del ADMIN | no rechaza: solo actualiza las que cumplen la condición |
| R16 | Al registrar un cobro se recalcula el estado de su `operacion_factoraje`: LIQUIDADA si todas sus facturas están COBRADAS; si no, EN_MORA si alguna está VENCIDA; si no, VIGENTE | servicio, al cobrar | no rechaza: solo actualiza el estado |

## 6. Lo que no hace

1. No se conecta con el Ministerio de Hacienda: la verificación del DTE se simula. Tampoco acepta facturas físicas ni quedan en papel.
2. No admite pagos parciales: cada operación tiene un solo desembolso y cada factura se cobra en un solo pago.
3. No mueve dinero real: el desembolso y el cobro solo se registran.
4. No gestiona el recobro a la empresa cedente si el pagador nunca paga.
5. No marca los vencimientos de forma automática: el ADMIN ejecuta "marcar vencidas" a mano. Programarlo con `@Scheduled` queda como mejora.

## 7. Códigos de respuesta HTTP

Criterio único para los errores de la API:

- **400 (Bad Request)** — una regla de negocio rechaza la operación, o el cuerpo
  trae un dato inválido o mal formado. Son las `OperacionRechazadaException` y sus
  hijas (R1, R2, R7, R8, R9), más los fallos de `@Valid` y el JSON ilegible.
- **404 (Not Found)** — se pide por la URL un recurso que no existe
  (`GET /api/.../{id}`).
- **409 (Conflict)** — la operación choca con una restricción de los datos: un
  `UNIQUE` o una llave foránea (`DataIntegrityViolationException`), por ejemplo una
  factura ya cedida (R4) o un NIT repetido (R5).

Ninguna regla de negocio termina en 500. El 500 queda para lo que nunca debería
pasar (`IllegalStateException`).
