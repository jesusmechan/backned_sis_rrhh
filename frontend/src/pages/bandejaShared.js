export const TIPO = { PERMISO: 'Permiso', HORA_EXTRA: 'Horas extras' };

export function fmtDate(value) {
  if (!value) return '—';
  const d = String(value).length <= 10 ? new Date(`${value}T00:00:00`) : new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function fmtDateTime(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
}

export function fmtTime(value) {
  return value ? String(value).slice(0, 5) : '—';
}

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
