export function fmtDate(value, empty = '—') {
  if (!value) return empty;
  const d = String(value).length <= 10 ? new Date(`${value}T00:00:00`) : new Date(value);
  if (Number.isNaN(d.getTime())) return empty;
  return d.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function fmtDateTime(value, empty = '—') {
  if (!value) return empty;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return empty;
  return d.toLocaleString('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
}

export function fmtRelative(value, empty = '—') {
  if (!value) return empty;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return empty;
  const s = (Date.now() - d.getTime()) / 1000;
  if (s < 45) return 'Ahora';
  if (s < 3600) return `Hace ${Math.max(1, Math.floor(s / 60))} min`;
  if (s < 86400) return `Hace ${Math.floor(s / 3600)} h`;
  if (s < 86400 * 7) return `Hace ${Math.floor(s / 86400)} d`;
  return fmtDateTime(value);
}

export function fmtTime(value, empty = '—') {
  return value ? String(value).slice(0, 5) : empty;
}

export function fmtMoney(value, empty = '—') {
  if (value == null || value === '') return empty;
  const n = Number(value);
  if (Number.isNaN(n)) return empty;
  return n.toLocaleString('es-PE', { style: 'currency', currency: 'PEN' });
}

export function hhmm(value) {
  return value ? String(value).slice(0, 5) : '';
}

/** Edad en años cumplidos a partir de ISO `YYYY-MM-DD`. */
export function calcAge(fechaISO, empty = '') {
  if (!fechaISO) return empty;
  const birth = new Date(`${String(fechaISO).slice(0, 10)}T00:00:00`);
  if (Number.isNaN(birth.getTime())) return empty;
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age -= 1;
  return age >= 0 ? age : empty;
}
