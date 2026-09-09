/** Filtros de ingreso: recortan caracteres que no corresponden al campo. */

export function letters(value, max = 80) {
  return String(value ?? '')
    .replace(/[^\p{L}\s'-]/gu, '')
    .replace(/\s{2,}/g, ' ')
    .slice(0, max);
}

export function digits(value, max = 20) {
  return String(value ?? '').replace(/\D/g, '').slice(0, max);
}

export function code(value, max = 40) {
  return String(value ?? '').toUpperCase().replace(/[^A-Z0-9_-]/g, '').slice(0, max);
}

export function username(value, max = 60) {
  return String(value ?? '').toLowerCase().replace(/[^a-z0-9._]/g, '').slice(0, max);
}

export function email(value, max = 120) {
  return String(value ?? '').toLowerCase().replace(/\s/g, '').replace(/[^a-z0-9.@_+-]/g, '').slice(0, max);
}

export function phone(value) {
  return digits(value, 9);
}

export function address(value, max = 200) {
  return String(value ?? '').replace(/[^\p{L}0-9\s.,#\-/°]/gu, '').slice(0, max);
}

export function text(value, max = 400) {
  return String(value ?? '').replace(/[<>]/g, '').slice(0, max);
}

export function routePath(value, max = 120) {
  let next = String(value ?? '').toLowerCase().replace(/[^a-z0-9/_-]/g, '').slice(0, max);
  if (next && !next.startsWith('/')) next = `/${next}`;
  return next;
}

export function decimal(value, max = 8) {
  let next = String(value ?? '').replace(/[^\d.]/g, '');
  const parts = next.split('.');
  if (parts.length > 2) next = `${parts[0]}.${parts.slice(1).join('')}`;
  const num = Number(next);
  if (next !== '' && !Number.isNaN(num) && num > max) return String(max);
  return next;
}

export function documentNumber(tipo, value) {
  if (tipo === 'DNI') return digits(value, 8);
  return String(value ?? '').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 12);
}

export function label(value, max = 80) {
  return String(value ?? '').replace(/[^\p{L}0-9\s._-]/gu, '').replace(/\s{2,}/g, ' ').slice(0, max);
}

export function isEmail(value) {
  return /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i.test(String(value || '').trim());
}

export function isLetters(value) {
  return /^\p{L}+(?:[ '\-]\p{L}+)*$/u.test(String(value || '').trim());
}

export function isUsername(value) {
  return /^[a-z][a-z0-9._]{2,59}$/.test(String(value || ''));
}

export function isEmployeeCode(value) {
  return /^[A-Z]{2,8}-\d{3,6}$/.test(String(value || ''));
}
