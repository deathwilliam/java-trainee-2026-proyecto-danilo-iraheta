# AnticipaPYME

**Video:** https://www.youtube.com/watch?v=AlaEzb5vGto

API REST de factoraje de facturas electrónicas (DTE) para PYME. Una PYME que le vendió a una empresa grande y todavía no cobra le cede sus facturas pendientes al banco; el banco le adelanta el dinero con un descuento y después le cobra a la empresa pagadora. El banco no evalúa a la PYME sino al pagador, mediante un cupo aprobado por cada empresa pagadora.

Incluye empresas, facturas, cupos de pagadores, anticipos, desembolsos y cobros, con login, roles y bitácora de cada operación.

Es el proyecto integrador del Programa Digital Trainee 2026.

**Tecnologías:** Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security con JWT · PostgreSQL 17 · Swagger (springdoc) · JUnit 5, Mockito y MockMvc · Docker Compose.

---

## Levantarlo desde cero

Solo necesita Docker.

**1. Cree el archivo `.env`** en la carpeta del proyecto, junto a `docker-compose.yaml`. Parta de `.env.example` y cambie `JWT_CLAVE` por una clave de 32 bytes o más. Créelo desde el editor, no con `>` en PowerShell.

```
DB_NOMBRE=anticipapyme
DB_USUARIO=banco
DB_CLAVE=banco123
JWT_CLAVE=una-clave-larga-de-32-caracteres-o-mas-solo-para-su-maquina
```

`DB_PUERTO_HOST` es opcional: es el puerto de su máquina con el que se publica PostgreSQL. Si no lo define, usa el **5432**. Si ese puerto ya lo ocupa otro PostgreSQL, agregue al `.env` una línea como `DB_PUERTO_HOST=5433`. La aplicación y las pruebas leen esa misma variable (ver abajo).

**2. Levante la API y la base:**

```
docker compose up --build -d
docker compose logs -f api
```

La primera vez tarda más. Está lista cuando aparece `Started ApiApplication`. Si falta `JWT_CLAVE`, Docker Compose se detiene y lo avisa.

**3. Abra Swagger:** http://localhost:8080/swagger-ui.html

**4. Para terminar:**

```
docker compose down      # apaga, y conserva el volumen
docker compose down -v   # apaga, y borra también el volumen
```

> Cada vez que la API arranca, la base vuelve a los datos de `import.sql`, porque `ddl-auto` está en `create`. Los datos de prueba usan fechas relativas (`current_date + 45`), así que las facturas y el cupo siempre están vigentes cuando arranca. Los correlativos de operaciones, desembolsos y cobros salen de secuencias (`schema.sql`) y **no** se reinician con cada arranque.

---

## Usuarios de prueba

| Usuario | Clave | Rol | Qué puede hacer |
| :--- | :--- | :--- | :--- |
| `admin1` | `Admin2026` | ADMIN | Aprobar, renovar y suspender cupos; ejecutar "marcar vencidas"; consultar todo |
| `asesor1` | `Asesor2026` | ASESOR | Registrar empresas y facturas; anticipar; registrar cobros; consultar todo |
| `panaderia` | `Cliente2026` | CLIENTE | Consultar los catálogos, sus facturas y sus operaciones |

`asesor1` es el ejecutivo `EMP-001` (Carla Mejía). `panaderia` es el usuario de la empresa cedente *Panadería La Espiga* (id 1).

**Para probar en Swagger:** use `POST /api/auth/login`, copie el `token`, haga clic en **Authorize** y péguelo sin la palabra `Bearer`. El token dura 2 horas.

### Datos que ya trae la base

| Qué            | Detalle                                                                                                                                          |
|:---------------|:-------------------------------------------------------------------------------------------------------------------------------------------------|
| Empresas       | id 1 *La Espiga* (cedente, con cuenta bancaria y usuario) · id 2 *SuperValle* (pagadora) · id 3 *Empresa Externa* (cedente de una factura ajena) |
| Cupo           | `CUP-2026-001` de SuperValle: \$50,000.00, categoría A (descuento 18 % anual, mora 24 % anual, plazo máximo 120 días), vence en 180 días         |
| Facturas 1 a 3 | CONFIRMADAS, de \$5,000.00, \$3,000.00 y \$2,000.00; vencen en 60, 30 y 90 días                                                                  |
| Factura 4      | REGISTRADA (\$1,500.00): sirve para probar R1                                                                                                    |
| Facturas 5 y 6 | CONFIRMADAS, de \$4,000.00 y \$1,000.00; vencen en 45 y 20 días: son el caso bueno (descuentos de \$90.00 y \$10.00)                             |
| Factura 7      | CONFIRMADA, de La Externa (id 3) hacia SuperValle: sirve para probar que un CLIENTE no ve facturas ajenas                                        |

---

## La operación principal: anticipar facturas

`POST /api/anticipos` le adelanta a una empresa cedente el dinero de varias facturas confirmadas de una misma pagadora. Es **todo o nada** (`@Transactional`): la operación, un detalle por factura, el cambio de estado de las facturas, el cupo, el desembolso y una fila en la bitácora. Quién la hizo **sale del token**, nunca del JSON.

```json
{ "empresaCedenteId": 1, "facturaIds": [5, 6] }
```

Responde **201** con el comprobante:

```json
{
  "numeroOperacion": "OPF-2026-00001",
  "numeroComprobante": "DES-2026-00001",
  "estado": "VIGENTE",
  "facturas": [
    { "codigoGeneracion": "DTE-0001-0001-0001-000000000000005", "montoTotal": 4000.00, "montoDescuento": 90.00 },
    { "codigoGeneracion": "DTE-0001-0001-0001-000000000000006", "montoTotal": 1000.00, "montoDescuento": 10.00 }
  ],
  "montoNominalTotal": 5000.00,
  "montoDescuentoTotal": 100.00,
  "montoDesembolsado": 4900.00
}
```

El `montoDesembolsado` no se guarda: es `montoNominalTotal − montoDescuentoTotal`.

### Qué hace, en orden

1. Valida todo antes de tocar nada: la lista no está vacía ni repetida, las facturas existen, el cupo está ACTIVO y vigente, cada factura cumple R1, R8 y R9, la cedente tiene cuenta bancaria y el monto cabe en el cupo (R7).
2. Inserta la operación en estado VIGENTE, con las tasas copiadas de la categoría de riesgo del cupo.
3. Inserta un detalle por factura con su descuento (R10) y guarda los totales (R11 y R12).
4. Marca cada factura como CEDIDA.
5. Descuenta el total del `monto_disponible` del cupo (R13).
6. Inserta el desembolso, con la `cuenta_destino` copiada de la cuenta bancaria de la cedente.
7. Registra la operación en la bitácora como EXITOSO.

**Si falla a la mitad**, se deshace todo: no queda la operación ni sus detalles, las facturas siguen CONFIRMADAS, el cupo queda como estaba y no hay desembolso. El intento sí queda en la bitácora como FALLIDO, porque `BitacoraServicio.registrarFallido` usa `REQUIRES_NEW` y vive en **otro bean**: si estuviera dentro de `AnticipoServicio`, la llamada no pasaría por el proxy de Spring, el `REQUIRES_NEW` se ignoraría y el rollback se llevaría también el FALLIDO.

Los números `OPF-…`, `DES-…` y `COB-…` salen de **secuencias de PostgreSQL**, no de `count() + 1`, para que dos operaciones simultáneas no repitan número.

### El otro movimiento de dinero: el cobro

`POST /api/cobros` registra el pago que hace la empresa pagadora por **una** factura cedida.

```json
{ "facturaId": 5, "montoPagado": 4000.00, "metodoPago": "TRANSFERENCIA" }
```

`metodoPago` puede ser `TRANSFERENCIA`, `CHEQUE` o `DEPOSITO`. El monto tiene que ser **exacto**: capital más mora; si falta o sobra, se rechaza con 400. Al cobrar: la factura pasa a COBRADA, el cupo recupera el monto de la factura (R13), la operación se recalcula (R16) y queda un EXITOSO en la bitácora. Si falla, queda un FALLIDO en la bitácora, igual que en el anticipo.

### Las reglas

| Regla | Qué dice                                                                                                                                                                   | Dónde se cumple                                                            | Si no se cumple            |
|:------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------|:---------------------------|
| R1    | Solo se anticipa una factura CONFIRMADA                                                                                                                                    | Servicio                                                                   | 400                        |
| R2    | Solo se anticipa contra un cupo ACTIVO cuya fecha de vencimiento no haya pasado                                                                                            | Servicio                                                                   | 400                        |
| R3    | Una empresa pagadora solo puede tener un cupo ACTIVO a la vez                                                                                                              | Servicio, al aprobar un cupo y al reactivarlo                              | 400                        |
| R4    | Una factura no se cede dos veces: el `codigo_generacion` no se repite y el `factura_id` no se repite en `detalle_operacion`                                                | `UNIQUE` en la tabla (y el servicio rechaza ids repetidos en la solicitud) | 409                        |
| R5    | El NIT tiene el formato `0000-000000-000-0` y no se repite                                                                                                                 | DTO (`@Pattern`) y `UNIQUE` en la tabla                                    | 400 · 409                  |
| R6    | Los montos de dinero son mayores que cero                                                                                                                                  | DTO (`@Positive`); el resto de montos los calcula el servicio              | 400                        |
| R7    | La suma de los montos de las facturas no supera el disponible del cupo                                                                                                     | Servicio                                                                   | 400                        |
| R8    | Las facturas son de la cedente de la operación y de la pagadora del cupo, y la cedente tiene cuenta bancaria                                                               | Servicio                                                                   | 400                        |
| R9    | A cada factura le faltan entre 15 días y el plazo máximo de la categoría de riesgo para vencer                                                                             | Servicio                                                                   | 400                        |
| R10   | Descuento de cada factura = monto × tasa de descuento × días hasta su vencimiento ÷ 360, redondeado a 2 decimales                                                          | Servicio, dentro de la transacción                                         | Se deshace todo            |
| R11   | `monto_nominal_total` = suma de los montos de las facturas                                                                                                                 | Servicio, dentro de la transacción                                         | Se deshace todo            |
| R12   | `monto_descuento_total` = suma de los descuentos                                                                                                                           | Servicio, dentro de la transacción                                         | Se deshace todo            |
| R13   | El cupo baja al anticipar y recupera el monto al cobrar                                                                                                                    | Servicio, en el anticipo y en el cobro                                     | Se deshace todo            |
| R14   | Solo se cobra una factura CEDIDA o VENCIDA, sin pagos parciales: capital = monto de la factura; mora = monto × tasa de mora × días de atraso ÷ 360 (cero si paga a tiempo) | Servicio, al cobrar                                                        | 400                        |
| R15   | Una factura CEDIDA ya vencida pasa a VENCIDA y su operación a EN_MORA; un cupo ACTIVO ya vencido pasa a VENCIDO                                                            | Servicio, `POST /api/mantenimiento/marcar-vencidas`                        | No rechaza: solo actualiza |
| R16   | Al cobrar se recalcula la operación: LIQUIDADA si todas sus facturas están COBRADAS; si no, EN_MORA si alguna está VENCIDA; si no, VIGENTE                                 | Servicio, al cobrar                                                        | No rechaza: solo actualiza |

### Cómo responde cuando algo falla

Todos los errores responden con `ProblemDetail`, y un solo criterio decide el código:

| Código  | Cuándo                                                                                                                                                                                                                          |
|:--------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **400** | Una regla de negocio dice que no (`OperacionRechazadaException` y sus hijas: R1, R2, R3, R7, R8, R9, R14…), o el cuerpo trae un dato inválido o ilegible. Si falla `@Valid`, la respuesta trae el campo que falló en `errores`. |
| **401** | Falta el token, es falso o venció; o el login trae una clave mala.                                                                                                                                                              |
| **403** | Hay token, pero el rol no tiene permiso.                                                                                                                                                                                        |
| **404** | Se pide por la URL un recurso que no existe (`GET /api/.../{id}`).                                                                                                                                                              |
| **409** | La operación choca con los datos: un `UNIQUE` (factura ya cedida, NIT repetido) o algo con dependencias activas (empresa con cupo ACTIVO, cupo con operaciones VIGENTES, factura ya cedida o cobrada).                          |

Una regla de negocio nunca termina en 500; ese código queda para lo que nunca debería pasar (`IllegalStateException`). Si en un anticipo falta un id de factura, se responde **400** (viene en el cuerpo, no en la URL), no 404.

Ejemplo, al anticipar una factura que ya está cedida:

```json
{
  "type": "about:blank",
  "title": "Operacion rechazada",
  "status": 400,
  "detail": "La factura DTE-0001-0001-0001-000000000000005 tiene estado CEDIDA, no esta CONFIRMADA",
  "instance": "/api/anticipos"
}
```

### Una prueba rápida en Swagger

1. Entre como `asesor1` y haga clic en **Authorize**.
2. `POST /api/anticipos` con `{ "empresaCedenteId": 1, "facturaIds": [5, 6] }` → **201**, con \$5,000.00 de nominal, \$100.00 de descuento y \$4,900.00 desembolsados.
3. Repita el mismo `POST` → **400**: las facturas ya están CEDIDAS.
4. `POST /api/cobros` con `{ "facturaId": 5, "montoPagado": 4000.00, "metodoPago": "TRANSFERENCIA" }` → **201**, sin mora. La operación sigue VIGENTE porque falta la factura 6.
5. Entre como `panaderia`: `GET /api/facturas` → **200** (solo ve las suyas), `GET /api/facturas/7` → **404** (es ajena) y `POST /api/anticipos` → **403**.
6. Mire la bitácora, con el nombre de base y el usuario de su `.env`:

```
docker compose exec db psql -U banco -d anticipapyme -c "SELECT id, tipo_operacion, resultado, detalle FROM bitacora_operacion ORDER BY id DESC;"
```

---

## Los endpoints

Sin token, todo responde **401**, salvo el login y Swagger. Con un rol sin permiso, **403**. Son 31 endpoints.

| Método y ruta                             | Qué hace                                             | Quién                               | Responde        |
|:------------------------------------------|:-----------------------------------------------------|:------------------------------------|:----------------|
| `POST /api/auth/login`                    | Entrega el token                                     | Abierto                             | 200 · 400 · 401 |
| `POST /api/anticipos`                     | **Anticipa facturas**                                | ASESOR                              | 201 · 400       |
| `POST /api/cobros`                        | Registra el cobro de una factura                     | ASESOR                              | 201 · 400       |
| `GET /api/cobros`                         | Lista los cobros                                     | ADMIN, ASESOR                       | 200             |
| `GET /api/cobros/{id}`                    | Un cobro                                             | ADMIN, ASESOR                       | 200 · 404       |
| `GET /api/operaciones`                    | Lista las operaciones                                | Con token (CLIENTE: solo las suyas) | 200             |
| `GET /api/operaciones/{id}`               | Una operación                                        | Con token (CLIENTE: solo las suyas) | 200 · 404       |
| `GET /api/desembolsos`                    | Lista los desembolsos                                | ADMIN, ASESOR                       | 200             |
| `GET /api/desembolsos/{id}`               | Un desembolso                                        | ADMIN, ASESOR                       | 200 · 404       |
| `GET /api/facturas`                       | Lista las facturas                                   | Con token (CLIENTE: las suyas)      | 200             |
| `GET /api/facturas/{id}`                  | Una factura                                          | Con token (CLIENTE: las suyas)      | 200 · 404       |
| `POST /api/facturas`                      | Registra una factura                                 | ASESOR                              | 201 · 400 · 409 |
| `PATCH /api/facturas/{id}/confirmacion`   | La pasa de REGISTRADA a CONFIRMADA                   | ASESOR                              | 200 · 400 · 404 |
| `DELETE /api/facturas/{id}`               | La anula (INVALIDADA)                                | ASESOR                              | 204 · 404 · 409 |
| `GET /api/empresas`                       | Lista las empresas                                   | ADMIN, ASESOR                       | 200             |
| `GET /api/empresas/{id}`                  | Una empresa                                          | ADMIN, ASESOR                       | 200 · 404       |
| `POST /api/empresas`                      | Registra una empresa                                 | ASESOR                              | 201 · 400 · 409 |
| `PATCH /api/empresas/{id}`                | Edita sus datos de contacto, cuenta y sector         | ASESOR                              | 200 · 400 · 404 |
| `DELETE /api/empresas/{id}`               | La desactiva                                         | ASESOR                              | 204 · 404 · 409 |
| `GET /api/cupos`                          | Lista los cupos                                      | ADMIN, ASESOR                       | 200             |
| `GET /api/cupos/{id}`                     | Un cupo                                              | ADMIN, ASESOR                       | 200 · 404       |
| `POST /api/cupos`                         | Aprueba un cupo                                      | ADMIN                               | 201 · 400       |
| `PATCH /api/cupos/{id}`                   | Reactiva un cupo suspendido o renueva su vencimiento | ADMIN                               | 200 · 400 · 404 |
| `DELETE /api/cupos/{id}`                  | Lo suspende                                          | ADMIN                               | 204 · 404 · 409 |
| `GET /api/ejecutivos`                     | Lista los ejecutivos                                 | ADMIN, ASESOR                       | 200             |
| `GET /api/ejecutivos/{id}`                | Un ejecutivo                                         | ADMIN, ASESOR                       | 200 · 404       |
| `GET /api/sectores-economicos`            | Lista el catálogo de sectores                        | Con token                           | 200             |
| `GET /api/sectores-economicos/{id}`       | Un sector                                            | Con token                           | 200 · 404       |
| `GET /api/categorias-riesgo`              | Lista el catálogo de categorías de riesgo            | Con token                           | 200             |
| `GET /api/categorias-riesgo/{id}`         | Una categoría                                        | Con token                           | 200 · 404       |
| `POST /api/mantenimiento/marcar-vencidas` | Ejecuta R15                                          | ADMIN                               | 200             |

**Cómo funcionan los roles.** `rol.nombre` guarda ADMIN, ASESOR o CLIENTE, sin prefijo. El login pone los roles en el claim `roles` del token, y al leerlo `ConfiguracionSeguridad` les agrega `ROLE_`, porque `hasRole("ASESOR")` busca la autoridad `ROLE_ASESOR`. Las rutas se protegen en `ConfiguracionSeguridad` y las operaciones principales además llevan `@PreAuthorize`.

---

## Las pruebas

Se corren en su máquina y necesitan PostgreSQL con la base `anticipapyme_test` en el puerto **5432** (o el de `DB_PUERTO_HOST`, ver abajo). La primera vez, con la base de Docker Compose encendida:

```
docker compose up -d db
docker compose exec db psql -U banco -d anticipapyme -c "CREATE DATABASE anticipapyme_test;"
```

> **Importante:** Docker Compose solo crea la base `anticipapyme`; la `anticipapyme_test` hay que crearla con el comando de arriba, dentro de la misma instancia de PostgreSQL. Sin ella, las pruebas que usan la base fallan con `database "anticipapyme_test" does not exist`. Si usó `docker compose down -v`, el volumen se borró y con él esta base: vuelva a crearla.

Después, en la carpeta del proyecto:

```
.\mvnw.cmd test
```

No necesitan `JWT_CLAVE`: traen su propia clave de prueba (`jwt.clave=...` dentro de cada `@SpringBootTest`). Se conectan con el usuario `banco` y la clave `banco123`; si cambió `DB_USUARIO` o `DB_CLAVE` en su `.env`, defina las mismas variables antes de correrlas. Si publicó la base en otro puerto, defina también `DB_PUERTO_HOST` en la terminal antes de correrlas (en PowerShell: `$env:DB_PUERTO_HOST=5433`); el `.env` solo lo lee Docker Compose. Son **39 pruebas**.

| Clase                   | Qué comprueba                                                                                                                                                      | Pruebas | Necesita la base |
|:------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-------:|:----------------:|
| `AnticipoReglasTest`    | Cada regla del anticipo (R1, R2, R7, R8, R9, cuenta bancaria, listas vacías y repetidas), con Mockito                                                              |   13    |        No        |
| `CobroReglasTest`       | R14 y R16: mora, monto exacto, sobrepago, estados de la operación                                                                                                  |    8    |        No        |
| `CupoReglasTest`        | R3, fechas del cupo y suspensión                                                                                                                                   |    4    |        No        |
| `VencimientoReglasTest` | R15: facturas, operaciones y cupos vencidos                                                                                                                        |    3    |        No        |
| `AnticipoServicioTest`  | Que la caída a la mitad hace rollback, que una regla rechazada no toca nada, que el intento fallido queda en la bitácora y que el caso bueno deja todas las partes |    3    |        Sí        |
| `SeguridadApiTest`      | 401, 403, roles, que un CLIENTE no ve facturas ajenas y que el usuario sale del token, con MockMvc                                                                 |    7    |        Sí        |
| `ApiApplicationTests`   | Que la aplicación arranca entera                                                                                                                                   |    1    |        Sí        |

---

## Correrla desde IntelliJ

Con la base de Docker Compose encendida (`docker compose up -d db`), agregue la variable `JWT_CLAVE` en *Run → Edit Configurations → Modify options → Environment variables*, y arranque `ApiApplication`:

```
JWT_CLAVE=una-clave-larga-de-32-caracteres-o-mas-solo-para-su-maquina
```

Se conecta a la base `anticipapyme` en el puerto **5432**. Si publicó la base en otro puerto (`DB_PUERTO_HOST` en el `.env`), agregue la misma variable aquí, por ejemplo `DB_PUERTO_HOST=5433`. Y si su base está en otro servidor, defina `DB_URL` completa.

Si hay algo usando el puerto 8080, apáguelo antes de usar Docker Compose. Y si la API corre en IntelliJ, no levante también el servicio `api` de Compose.

---

## Cómo está organizado

```
src/main/java/sv/anticipapyme/
├── controlador/   recibe HTTP, llama al servicio y responde con DTOs. ManejadorErrores: todos los errores. ConfiguracionOpenApi: Swagger con Authorize
├── dto/           lo que entra y sale por la API, con record (…Solicitud, …Actualizacion, …Respuesta)
├── servicio/      las reglas y las transacciones
├── repositorio/   el acceso a la base
├── entidad/       las tablas: empresa, factura, cupo_pagador, operacion_factoraje, detalle_operacion, desembolso, cobro, bitacora_operacion, usuario, rol… (y enums/)
├── excepcion/     las reglas que dicen que no
└── seguridad/     la cadena de filtros, los roles y el token
src/main/resources/
├── application.properties
├── schema.sql     las secuencias de los correlativos
└── import.sql     catálogos, usuarios, empresas, cupo y facturas de prueba
```

Un controlador nunca usa un repositorio, y ninguna entidad entra ni sale por la API.

La clave de firma nunca está en el repositorio: va en `JWT_CLAVE` (`jwt.clave=${JWT_CLAVE}`, sin valor por defecto) y el `.env` está en `.gitignore`.

---

## Lo que no hace

1. No se conecta con el Ministerio de Hacienda: la verificación del DTE se simula. Tampoco acepta facturas físicas.
2. No admite pagos parciales: cada operación tiene un solo desembolso y cada factura se cobra en un solo pago.
3. No mueve dinero real: el desembolso y el cobro solo se registran.
4. No gestiona el recobro a la empresa cedente si el pagador nunca paga.
5. No marca los vencimientos de forma automática: el ADMIN ejecuta "marcar vencidas" a mano. Programarlo con `@Scheduled` queda como mejora.
