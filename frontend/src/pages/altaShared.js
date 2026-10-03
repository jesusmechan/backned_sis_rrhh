import { isoDateEnZona } from '../lib/format';

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

/** nombre.apellido@dominio con el dominio de Maestros › Parámetros (empresa_dominio_correo). */
export function correoInstitucional(nombres, apellido, dominio) {
  const slug = slugCuenta(nombres, apellido);
  return slug && dominio ? `${slug}@${dominio}` : '';
}

/** Siguiente código de trabajador con el prefijo y los dígitos configurados (codigo_empleado_*). */
export function siguienteCodigo(empleados = [], prefijo = '', digitos = 3) {
  const nums = empleados
    .map((e) => Number(String(e.codigoEmpleado || '').replace(/\D/g, '')))
    .filter((n) => n > 0);
  const next = (nums.length ? Math.max(...nums) : 0) + 1;
  const numero = String(next).padStart(digitos, '0');
  return prefijo ? `${prefijo}-${numero}` : numero;
}

export function hoyISO() {
  return isoDateEnZona();
}
