/**
 * Formato regional (idioma, zona horaria y moneda). Se inicializa con los valores del navegador y
 * ConfigProvider lo sobrescribe con los parámetros públicos del sistema antes de pintar la app.
 */
export const regional = {
  locale: navigator.language || 'es',
  zona: Intl.DateTimeFormat().resolvedOptions().timeZone,
  moneda: '',
  simboloMoneda: ''
};

export function setRegional({ locale, zona, moneda, simboloMoneda } = {}) {
  if (locale) regional.locale = locale;
  if (zona) regional.zona = zona;
  if (moneda) regional.moneda = moneda;
  if (simboloMoneda) regional.simboloMoneda = simboloMoneda;
}

function enZona() {
  return { timeZone: regional.zona };
}

function asDate(value) {
  if (!value) return null;
  const d = String(value).length <= 10 ? new Date(`${value}T00:00:00`) : new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

/** Fecha completa: 21 de septiembre de 2026 */
export function fmtDate(value, empty = '—') {
  const d = asDate(value);
  if (!d) return empty;
  return d.toLocaleDateString(regional.locale, {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    ...enZona()
  });
}

/** Día, mes, año y hora con AM/PM: 21 de septiembre de 2026, 10:41 a. m. */
export function fmtDateTime(value, empty = '—') {
  if (!value) return empty;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return empty;
  return d.toLocaleString(regional.locale, {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
    ...enZona()
  });
}

/** Fecha y hora corta en la zona oficial: 21 sept, 10:41 a. m. */
export function fmtShortDateTime(value, empty = '—') {
  if (!value) return empty;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return empty;
  return d.toLocaleString(regional.locale, { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', ...enZona() });
}

/** Cualquier formato Intl en el idioma y la zona oficiales. */
export function fmtIntl(value, options = {}) {
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleString(regional.locale, { ...enZona(), ...options });
}

/** Fecha ISO (YYYY-MM-DD) de un instante en la zona oficial. */
export function isoDateEnZona(value = new Date()) {
  return new Intl.DateTimeFormat('en-CA', { ...enZona(), year: 'numeric', month: '2-digit', day: '2-digit' })
    .format(value instanceof Date ? value : new Date(value));
}

/** Hora HH:mm (24 h) de un instante en la zona oficial. */
export function hhmmEnZona(value) {
  return new Intl.DateTimeFormat('en-GB', { ...enZona(), hour: '2-digit', minute: '2-digit', hour12: false })
    .format(value instanceof Date ? value : new Date(value));
}

/** Fecha y hora locales de la zona oficial como ISO con desfase: 2026-10-03T08:00:00-05:00 */
export function isoConZona(fecha, hora) {
  const parte = new Intl.DateTimeFormat('en-US', { ...enZona(), timeZoneName: 'longOffset' })
    .formatToParts(new Date(`${fecha}T12:00:00Z`))
    .find((p) => p.type === 'timeZoneName')?.value || 'GMT';
  const desfase = parte.replace('GMT', '') || '+00:00';
  return `${fecha}T${hora.length === 5 ? `${hora}:00` : hora}${desfase}`;
}

/** Día ISO de la semana (1 = lunes … 7 = domingo) en la zona oficial. */
export function diaIsoEnZona(value = new Date()) {
  const corto = new Intl.DateTimeFormat('en-US', { weekday: 'short', ...enZona() }).format(value);
  return ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].indexOf(corto) + 1;
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

/** Hora con AM/PM a partir de "HH:mm" o "HH:mm:ss". */
export function fmtTime(value, empty = '—') {
  if (!value) return empty;
  const raw = String(value).slice(0, 8);
  const parts = raw.split(':').map(Number);
  const hh = parts[0];
  const mm = parts[1] || 0;
  if (Number.isNaN(hh)) return String(value).slice(0, 5);
  const d = new Date();
  d.setHours(hh, mm, 0, 0);
  return d.toLocaleTimeString(regional.locale, {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  });
}

export function fmtMoney(value, empty = '—', options = {}) {
  if (value == null || value === '') return empty;
  const n = Number(value);
  if (Number.isNaN(n)) return empty;
  if (regional.moneda) {
    return n.toLocaleString(regional.locale, { style: 'currency', currency: regional.moneda, ...options });
  }
  const numero = n.toLocaleString(regional.locale, { minimumFractionDigits: 2, maximumFractionDigits: 2, ...options });
  return regional.simboloMoneda ? `${regional.simboloMoneda} ${numero}` : numero;
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
