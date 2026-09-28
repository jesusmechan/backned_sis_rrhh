// Genera DICCIONARIO_DATOS.md a partir de database/01_install.sql.
// Uso (desde la raíz del repo): node avance2-integrador1/02-base-de-datos/generar-diccionario.mjs
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const sqlPath = join(here, '..', '..', 'database', '01_install.sql');
const outPath = join(here, 'DICCIONARIO_DATOS.md');
const sql = readFileSync(sqlPath, 'utf8');

const modulos = [
  ['Catálogos organizacionales', ['area', 'cargo', 'horario_laboral', 'tipo_permiso', 'parametro_sistema']],
  ['Seguridad y menú', ['rol', 'permiso_funcional', 'rol_permiso', 'menu_item', 'menu_rol', 'usuario', 'refresh_token']],
  ['Personal y asistencia', ['empleado', 'contrato', 'marcacion']],
  ['Flujos de aprobación y trámites', ['configuracion_aprobacion', 'configuracion_aprobacion_detalle', 'solicitud_permiso', 'solicitud_hora_extra', 'solicitud_paso_aprobacion', 'historial_solicitud']],
  ['Operación y control', ['auditoria', 'notificacion', 'carga_masiva', 'carga_masiva_detalle', 'reporte_generado']],
  ['Planilla y contabilidad', ['cuenta_contable', 'planilla', 'planilla_detalle', 'asiento_contable', 'asiento_linea']],
  ['Desempeño y reclutamiento', ['evaluacion_desempeno', 'convocatoria', 'postulacion']],
];

const descripciones = {
  area: 'Áreas de la consultora (Gerencia, Contabilidad, RR. HH., …).',
  cargo: 'Puestos de trabajo asignables a un colaborador.',
  horario_laboral: 'Jornadas con hora de ingreso, salida y minutos de refrigerio.',
  tipo_permiso: 'Catálogo de permisos (salud, vacaciones, duelo, …).',
  parametro_sistema: 'Diccionario clave/valor: tasas de planilla, topes de horas extras, zona horaria.',
  rol: 'Perfiles de acceso: ADMIN, GERENCIA, RRHH, JEFE, EMPLEADO.',
  permiso_funcional: 'Acciones autorizables del sistema agrupadas por módulo.',
  rol_permiso: 'Puente N:M entre rol y permiso funcional.',
  menu_item: 'Opciones del menú lateral (ruta, icono, grupo y orden).',
  menu_rol: 'Puente N:M: qué opciones de menú ve cada rol.',
  usuario: 'Cuenta de acceso; como máximo una por empleado. Contraseña con hash BCrypt.',
  refresh_token: 'Tokens de renovación JWT (7 días) con marca de revocación.',
  empleado: 'Ficha del colaborador. id_jefe_inmediato define el organigrama.',
  contrato: 'Vínculo laboral; a lo sumo uno VIGENTE por empleado. Base de planilla y vacaciones.',
  marcacion: 'Registros de ingreso y salida. fecha se calcula en hora de Lima.',
  configuracion_aprobacion: 'Cabecera del circuito de aprobación por tipo de trámite.',
  configuracion_aprobacion_detalle: 'Pasos configurados del circuito (jefe, rol o usuario).',
  solicitud_permiso: 'Pedido de permiso. No guarda al aprobador: el circuito vive en los pasos.',
  solicitud_hora_extra: 'Pedido de horas extras (máx. 8 h por registro; topes diario y semanal por parámetro).',
  solicitud_paso_aprobacion: 'Instancia real de cada paso por solicitud (XOR permiso u hora extra).',
  historial_solicitud: 'Trazabilidad de cada acción sobre una solicitud.',
  auditoria: 'Bitácora general de operaciones (quién, qué, cuándo, IP).',
  notificacion: 'Avisos en campana/STOMP: BANDEJA, APROBADA, RECHAZADA.',
  carga_masiva: 'Cabecera de una importación Excel de empleados.',
  carga_masiva_detalle: 'Resultado por fila de la importación.',
  reporte_generado: 'Registro de cada exportación Excel/PDF con sus filtros.',
  cuenta_contable: 'Plan de cuentas usado por el asiento de planilla.',
  planilla: 'Cabecera mensual de remuneraciones (única por año y mes).',
  planilla_detalle: 'Boleta de un colaborador dentro de la planilla.',
  asiento_contable: 'Asiento de partida doble generado al cerrar la planilla.',
  asiento_linea: 'Líneas debe/haber del asiento.',
  evaluacion_desempeno: 'Evaluación 1–5 en puntualidad, calidad, cooperación e iniciativa.',
  convocatoria: 'Proceso de reclutamiento para un puesto.',
  postulacion: 'Candidato registrado en una convocatoria y su estado.',
};

const tablas = {};
const re = /CREATE TABLE (\w+) \(([\s\S]*?)\n\);/g;
let m;
while ((m = re.exec(sql))) {
  const [, nombre, cuerpo] = m;
  const lineas = cuerpo.split('\n').map((l) => l.trim().replace(/,$/, '')).filter(Boolean);
  const columnas = [];
  const restricciones = [];
  let pkCompuesta = [];
  let buffer = '';
  for (const linea of lineas) {
    if (linea.startsWith('--')) continue;
    buffer = buffer ? `${buffer} ${linea}` : linea;
    const abiertos = (buffer.match(/\(/g) || []).length - (buffer.match(/\)/g) || []).length;
    if (abiertos > 0) continue;
    const def = buffer;
    buffer = '';
    if (/^CONSTRAINT/i.test(def)) {
      restricciones.push(def.replace(/\s+/g, ' '));
      continue;
    }
    const pk = def.match(/^PRIMARY KEY \(([^)]+)\)/i);
    if (pk) {
      pkCompuesta = pk[1].split(',').map((s) => s.trim());
      continue;
    }
    const col = def.match(/^(\w+)\s+(.+)$/);
    if (!col) continue;
    const [, nombreCol, resto] = col;
    const tipo = resto.match(/^([A-Za-z_]+(?:\s*\([\d,\s]+\))?(?:\s+GENERATED ALWAYS AS[\s\S]+?STORED)?)/)?.[1] ?? resto;
    const flags = [];
    if (/PRIMARY KEY/i.test(resto)) flags.push('PK');
    const ref = resto.match(/REFERENCES (\w+) \((\w+)\)/i);
    if (ref) flags.push(`FK → ${ref[1]}.${ref[2]}`);
    if (/GENERATED BY DEFAULT AS IDENTITY/i.test(resto)) flags.push('IDENTITY');
    const def2 = resto.match(/(?<!BY )DEFAULT ('[^']*'|[^\s,]+)/i);
    columnas.push({
      nombre: nombreCol,
      tipo: tipo.replace(/\s+GENERATED ALWAYS AS[\s\S]+STORED/i, ' (calculada)').trim(),
      nulo: /NOT NULL|PRIMARY KEY/i.test(resto) ? 'No' : 'Sí',
      defecto: def2 ? def2[1] : '',
      flags,
    });
  }
  for (const c of columnas) if (pkCompuesta.includes(c.nombre)) c.flags.unshift('PK');
  for (const r of restricciones) {
    const uq = r.match(/CONSTRAINT \w+ UNIQUE \(([^)]+)\)/i);
    if (!uq) continue;
    const cols = uq[1].split(',').map((s) => s.trim());
    for (const c of columnas) if (cols.includes(c.nombre)) c.flags.push(cols.length > 1 ? `UK (${cols.join(', ')})` : 'UK');
  }
  tablas[nombre] = { columnas, restricciones };
}

const enums = [...sql.matchAll(/CREATE TYPE (\w+) AS ENUM \(([\s\S]*?)\);/g)]
  .map(([, n, vals]) => [n, vals.replace(/--.*$/gm, '').replace(/\s+/g, ' ').replace(/'/g, '').trim()]);

const indices = [...sql.matchAll(/CREATE (UNIQUE )?INDEX (\w+)\s+ON (\w+) \(([^)]+)\)([^;]*);/g)]
  .map(([, u, n, t, c, w]) => ({ n, t, c: c.trim(), unico: !!u, where: w.replace(/\s+/g, ' ').trim() }));

const esc = (s) => String(s).replace(/\|/g, '\\|');
let md = `# Diccionario de datos — rrhh_andina\n\n`;
md += `> Documento **generado** desde \`database/01_install.sql\` con \`generar-diccionario.mjs\`. No editar a mano: modificar el script SQL y volver a generar.\n\n`;
md += `Motor: PostgreSQL 16+. Tablas: **${Object.keys(tablas).length}**. Tipos enumerados: **${enums.length}**. Índices explícitos: **${indices.length}**.\n\n`;
md += `Convenciones: nombres en \`snake_case\` y singular; PK sustituta \`id_<tabla>\` con \`IDENTITY\`; fechas de auditoría en \`TIMESTAMPTZ\`; montos en \`NUMERIC(12,2)\`.\n\n`;
md += `## Índice\n\n`;
for (const [mod, ts] of modulos) md += `- **${mod}**: ${ts.map((t) => `[\`${t}\`](#${t})`).join(', ')}\n`;

for (const [mod, ts] of modulos) {
  md += `\n## ${mod}\n`;
  for (const t of ts) {
    const info = tablas[t];
    if (!info) continue;
    md += `\n### ${t}\n\n${descripciones[t] ?? ''}\n\n`;
    md += `| Columna | Tipo | Nulo | Por defecto | Clave / notas |\n|---|---|---|---|---|\n`;
    for (const c of info.columnas) md += `| \`${c.nombre}\` | ${esc(c.tipo)} | ${c.nulo} | ${esc(c.defecto)} | ${esc(c.flags.join(', '))} |\n`;
    const checks = info.restricciones.filter((r) => /CHECK/i.test(r));
    if (checks.length) {
      md += `\nReglas (CHECK):\n\n`;
      for (const r of checks) md += `- \`${esc(r.replace(/^CONSTRAINT /, ''))}\`\n`;
    }
    const idx = indices.filter((i) => i.t === t);
    if (idx.length) {
      md += `\nÍndices:\n\n`;
      for (const i of idx) md += `- \`${i.n}\` (${i.c})${i.unico ? ' — único' : ''}${i.where ? ` — parcial: \`${esc(i.where)}\`` : ''}\n`;
    }
  }
}

md += `\n## Tipos enumerados\n\n| Tipo | Valores |\n|---|---|\n`;
for (const [n, v] of enums) md += `| \`${n}\` | ${esc(v)} |\n`;

writeFileSync(outPath, md, 'utf8');
console.log(`Diccionario generado: ${outPath} (${Object.keys(tablas).length} tablas)`);
