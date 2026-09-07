export function slugCuenta(nombres = '', apellido = '') {
  const fold = (value) => String(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z]/g, '');
  const first = fold(String(nombres).trim().split(/\s+/)[0] || '');
  const last = fold(String(apellido).trim().split(/\s+/)[0] || '');
  if (!first || !last) return first || last;
  return `${first}.${last}`;
}

export function correoAndina(nombres, apellido) {
  const slug = slugCuenta(nombres, apellido);
  return slug ? `${slug}@andina.pe` : '';
}

export function siguienteCodigo(empleados = []) {
  const nums = empleados
    .map((e) => Number(String(e.codigoEmpleado || '').replace(/\D/g, '')))
    .filter((n) => n > 0);
  const next = (nums.length ? Math.max(...nums) : 0) + 1;
  return `AND-${String(next).padStart(3, '0')}`;
}

export function hoyISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
