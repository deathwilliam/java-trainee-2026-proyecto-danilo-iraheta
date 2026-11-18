package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.EmpresaActualizacion;
import sv.anticipapyme.dto.EmpresaRespuesta;
import sv.anticipapyme.dto.EmpresaSolicitud;
import sv.anticipapyme.entidad.Empresa;
import sv.anticipapyme.entidad.SectorEconomico;
import sv.anticipapyme.entidad.enums.EstadoCupo;
import sv.anticipapyme.excepcion.DependenciasActivasException;
import sv.anticipapyme.excepcion.OperacionRechazadaException;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import sv.anticipapyme.repositorio.CupoPagadorRepositorio;
import sv.anticipapyme.repositorio.EmpresaRepositorio;
import sv.anticipapyme.repositorio.SectorEconomicoRepositorio;

import java.util.List;

@Service
public class EmpresaServicio {

    private final EmpresaRepositorio empresas;
    private final SectorEconomicoRepositorio sectores;
    private final CupoPagadorRepositorio cupos;

    public EmpresaServicio(EmpresaRepositorio empresas, SectorEconomicoRepositorio sectores, CupoPagadorRepositorio cupos) {
        this.empresas = empresas;
        this.sectores = sectores;
        this.cupos = cupos;
    }

    @Transactional(readOnly = true)
    public List<EmpresaRespuesta> listar() {
        return empresas.findAll()
                .stream()
                .map(this::armarRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmpresaRespuesta buscarPorId(Long id) {
        return armarRespuesta(buscarEntidad(id));
    }

    @Transactional
    public EmpresaRespuesta crear(EmpresaSolicitud solicitud) {
        SectorEconomico sector = buscarSector(solicitud.sectorEconomicoId());

        Empresa empresa = new Empresa(
                solicitud.nit(),
                solicitud.nrc(),
                solicitud.nombreLegal(),
                solicitud.correo(),
                solicitud.telefono(),
                solicitud.direccion(),
                sector);

        empresa.setNombreComercial(solicitud.nombreComercial());
        empresa.setCuentaBancaria(solicitud.cuentaBancaria());

        return armarRespuesta(empresas.save(empresa));
    }

    @Transactional
    public EmpresaRespuesta actualizar(Long id, EmpresaActualizacion cambios) {
        Empresa empresa = buscarEntidad(id);

        if (cambios.nombreComercial() != null) empresa.setNombreComercial(cambios.nombreComercial());
        if (cambios.correo() != null) empresa.setCorreo(cambios.correo());
        if (cambios.telefono() != null) empresa.setTelefono(cambios.telefono());
        if (cambios.direccion() != null) empresa.setDireccion(cambios.direccion());
        if (cambios.cuentaBancaria() != null) empresa.setCuentaBancaria(cambios.cuentaBancaria());
        if (cambios.sectorEconomicoId() != null) empresa.setSectorEconomico(buscarSector(cambios.sectorEconomicoId()));

        return armarRespuesta(empresas.save(empresa));
    }

    @Transactional
    public void desactivar(Long id) {
        Empresa empresa = buscarEntidad(id);
        if (cupos.existsByEmpresaPagadoraIdAndEstado(id, EstadoCupo.ACTIVO)) {
            throw new DependenciasActivasException(
                    String.format("La empresa %s tiene un cupo ACTIVO y no se puede desactivar", empresa.getNombreLegal()));
        }
        empresa.setActivo(false);
        empresas.save(empresa);
    }

    private Empresa buscarEntidad(Long id) {
        return empresas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa", id));
    }

    private SectorEconomico buscarSector(Long sectorId) {
        return sectores.findById(sectorId)
                .orElseThrow(() -> new OperacionRechazadaException(
                        String.format("No existe el sector economico con id %d", sectorId)));
    }

    private EmpresaRespuesta armarRespuesta(Empresa e) {
        return new EmpresaRespuesta(
                e.getId(), e.getNit(), e.getNrc(), e.getNombreLegal(), e.getNombreComercial(),
                e.getCorreo(), e.getTelefono(), e.getDireccion(), e.getCuentaBancaria(),
                e.getSectorEconomico().getNombre(), e.getActivo());
    }
}