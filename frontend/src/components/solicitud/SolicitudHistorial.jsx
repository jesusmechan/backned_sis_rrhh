import { fmtDateTime } from '../../lib/format';

export function SolicitudHistorial({ historial = [] }) {
  return (
    <section className="rounded-xl border border-line bg-white p-5">
      <p className="mb-3 text-sm font-semibold text-navy">Historial</p>
      {historial.length === 0 ? (
        <p className="text-sm text-muted">Sin movimientos.</p>
      ) : (
        <ul className="space-y-3">
          {historial.map((h) => (
            <li key={h.idHistorial} className="text-sm text-slate-600">
              <p>
                <span className="font-medium text-navy">{h.accion}</span>
                {h.estadoNuevo ? ` → ${h.estadoNuevo}` : ''}
                {h.usuario ? ` · ${h.usuario}` : ''}
              </p>
              <p className="text-xs text-muted">{fmtDateTime(h.fechaHora)}</p>
              {h.comentario && <p className="mt-0.5 text-xs">{h.comentario}</p>}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
