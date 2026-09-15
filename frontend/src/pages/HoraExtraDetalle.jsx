import { useNavigate, useParams } from 'react-router-dom';
import { BackLink } from '../components/ui';
import { SolicitudAcciones, SolicitudVista } from '../components/solicitud/SolicitudVista';
import { useSolicitudDetalle } from '../lib/useSolicitudDetalle';
import { hhmm } from '../lib/format';

export function HoraExtraDetalle() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { detalle, historial, loading, error, ok, saving, cancelar } = useSolicitudDetalle('/api/horas-extras', id);

  return (
    <div>
      <BackLink to="/horas-extras">Horas extras</BackLink>

      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Trámites</p>
          <h1 className="page-title mt-1">Solicitud de horas extras</h1>
          {detalle && (
            <p className="mt-2 text-sm text-muted">#{detalle.idSolicitudHoraExtra}</p>
          )}
        </div>
        <SolicitudAcciones
          detalle={detalle}
          saving={saving}
          onCancel={cancelar}
          onDuplicar={() => navigate('/horas-extras/nuevo', {
            state: {
              duplicar: {
                idEmpleado: detalle.idEmpleado ? String(detalle.idEmpleado) : '',
                fecha: detalle.fecha || '',
                horaInicio: hhmm(detalle.horaInicio),
                horaFin: hhmm(detalle.horaFin),
                cantidadHoras: detalle.cantidadHoras != null ? String(detalle.cantidadHoras) : '',
                motivo: detalle.motivo || ''
              }
            }
          })}
        />
      </div>

      <SolicitudVista
        detalle={detalle}
        historial={historial}
        tipoSolicitud="HORA_EXTRA"
        subtitle="Horas extras"
        loading={loading}
        error={error}
        ok={ok}
      />
    </div>
  );
}
