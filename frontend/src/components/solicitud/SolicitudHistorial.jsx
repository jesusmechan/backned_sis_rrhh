import { fmtDateTime, fmtRelative } from '../../lib/format';

const ACCION = {
  REGISTRAR: { label: 'Registró la solicitud', tone: 'bg-info' },
  APROBAR: { label: 'Aprobó un paso', tone: 'bg-ok' },
  RECHAZAR: { label: 'Rechazó la solicitud', tone: 'bg-danger' },
  CANCELAR: { label: 'Canceló la solicitud', tone: 'bg-danger' },
  DEVOLVER: { label: 'Devolvió la solicitud', tone: 'bg-warn' }
};

export function SolicitudHistorial({ historial = [] }) {
  if (historial.length === 0) {
    return <p className="text-sm text-muted">Sin movimientos todavía.</p>;
  }

  return (
    <ol className="relative border-l-2 border-line pl-5">
      {historial.map((h, i) => {
        const acc = ACCION[h.accion] || { label: h.accion, tone: 'bg-slate-400' };
        const last = i === historial.length - 1;
        return (
          <li key={h.idHistorial} className={`relative ${last ? 'pb-0' : 'pb-6'}`}>
            <span className={`absolute -left-[1.6rem] top-1.5 h-3 w-3 rounded-full ring-4 ring-white ${acc.tone}`} />
            <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
              <p className="text-sm font-semibold text-navy">{acc.label}</p>
              <p className="text-xs tabular-nums text-muted">
                {fmtDateTime(h.fechaHora)}
                <span className="mx-1 text-slate-300">·</span>
                {fmtRelative(h.fechaHora)}
              </p>
            </div>
            <p className="mt-1 text-xs text-muted">
              {h.usuario || 'Sistema'}
              {h.estadoNuevo ? ` · Estado: ${h.estadoNuevo}` : ''}
            </p>
            {h.comentario && (
              <p className="mt-2 rounded-lg bg-surface px-3 py-2 text-sm text-slate-700 ring-1 ring-line">
                {h.comentario}
              </p>
            )}
          </li>
        );
      })}
    </ol>
  );
}
