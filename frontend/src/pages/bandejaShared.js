import { fmtDate, fmtTime } from '../lib/format';

export { fmtDate, fmtDateTime, fmtTime, hhmm } from '../lib/format';

export const TIPO = { PERMISO: 'Permiso', HORA_EXTRA: 'Horas extras' };

export function solicitudPath(tipoSolicitud, id) {
  return tipoSolicitud === 'HORA_EXTRA' ? `/api/horas-extras/${id}` : `/api/permisos/${id}`;
}

export function factsOf(detalle, tipoSolicitud) {
  if (!detalle) return [];
  if (tipoSolicitud === 'HORA_EXTRA' || detalle.cantidadHoras != null) {
    return [
      ['Fecha', fmtDate(detalle.fecha)],
      ['Horario', `${fmtTime(detalle.horaInicio)} – ${fmtTime(detalle.horaFin)}`],
      ['Horas', detalle.cantidadHoras],
      ['Flujo', detalle.flujo]
    ];
  }
  return [
    ['Desde', fmtDate(detalle.fechaInicio)],
    ['Hasta', fmtDate(detalle.fechaFin)],
    ['Horario', detalle.horaInicio ? `${fmtTime(detalle.horaInicio)} – ${fmtTime(detalle.horaFin)}` : 'Jornada'],
    ['Flujo', detalle.flujo]
  ];
}

export function asResueltaPermiso(p) {
  return {
    key: `PERMISO-${p.idSolicitudPermiso}`,
    tipoSolicitud: 'PERMISO',
    idSolicitud: p.idSolicitudPermiso,
    solicitante: p.empleado,
    tipoTramite: p.tipoPermiso,
    estado: p.estado,
    motivo: p.motivo,
    fecha: p.fechaCreacion
  };
}

export function asResueltaHora(h) {
  return {
    key: `HORA_EXTRA-${h.idSolicitudHoraExtra}`,
    tipoSolicitud: 'HORA_EXTRA',
    idSolicitud: h.idSolicitudHoraExtra,
    solicitante: h.empleado,
    tipoTramite: 'Horas extras',
    estado: h.estado,
    motivo: h.motivo,
    fecha: h.fechaCreacion
  };
}
