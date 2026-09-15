import { useNavigate, useParams } from 'react-router-dom';
import { BackLink } from '../components/ui';
import { SolicitudAcciones, SolicitudVista } from '../components/solicitud/SolicitudVista';
import { useSolicitudDetalle } from '../lib/useSolicitudDetalle';
import { hhmm } from '../lib/format';

export function PermisoDetalle() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { detalle, historial, loading, error, ok, saving, cancelar } = useSolicitudDetalle('/api/permisos', id);

  return (
    <div>
      <BackLink to="/permisos">Permisos</BackLink>

      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Trámites</p>
          <h1 className="page-title mt-1">Solicitud de permiso</h1>
          {detalle && (
            <p className="mt-2 text-sm text-muted">{detalle.tipoPermiso} · #{detalle.idSolicitudPermiso}</p>
          )}
        </div>
        <SolicitudAcciones
          detalle={detalle}
          saving={saving}
          onCancel={cancelar}
          onDuplicar={() => navigate('/permisos/nuevo', {
            state: {
              duplicar: {
                idEmpleado: detalle.idEmpleado ? String(detalle.idEmpleado) : '',
                idTipoPermiso: detalle.idTipoPermiso ? String(detalle.idTipoPermiso) : '',
                fechaInicio: detalle.fechaInicio || '',
                fechaFin: detalle.fechaFin || '',
                horaInicio: hhmm(detalle.horaInicio),
                horaFin: hhmm(detalle.horaFin),
                motivo: detalle.motivo || ''
              }
            }
          })}
        />
      </div>

      <SolicitudVista
        detalle={detalle}
        historial={historial}
        tipoSolicitud="PERMISO"
        subtitle={detalle?.tipoPermiso}
        loading={loading}
        error={error}
        ok={ok}
      />
    </div>
  );
}
