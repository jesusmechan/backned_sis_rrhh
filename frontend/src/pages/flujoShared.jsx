import { User, UserCog, Users } from 'lucide-react';

export const ORIGEN = { PERMISO: 'Permiso', HORA_EXTRA: 'Horas extras' };
export const APROBADOR = {
  JEFE_INMEDIATO: { label: 'Jefe inmediato', icon: Users },
  ROL: { label: 'Perfil', icon: UserCog },
  USUARIO: { label: 'Usuario', icon: User }
};

let pasoSeq = 1;

export function emptyPaso(n = 1) {
  return {
    key: `p-${pasoSeq++}`,
    numeroPaso: n,
    nombrePaso: '',
    tipoAprobador: 'JEFE_INMEDIATO',
    idRol: '',
    idUsuario: '',
    esObligatorio: true
  };
}

export function emptyForm() {
  return {
    codigo: '',
    nombre: '',
    tipoOrigen: 'PERMISO',
    idTipoPermiso: '',
    descripcion: '',
    activo: true,
    pasos: [emptyPaso(1)]
  };
}

export function toForm(row) {
  return {
    codigo: row.codigo,
    nombre: row.nombre,
    tipoOrigen: row.tipoOrigen,
    idTipoPermiso: row.idTipoPermiso || '',
    descripcion: row.descripcion || '',
    activo: row.activo !== false,
    pasos: (row.pasos || []).map((p, i) => ({
      key: `e-${row.idConfiguracion}-${p.numeroPaso || i}`,
      numeroPaso: p.numeroPaso || i + 1,
      nombrePaso: p.nombrePaso,
      tipoAprobador: p.tipoAprobador,
      idRol: p.idRol || '',
      idUsuario: p.idUsuario || '',
      esObligatorio: p.esObligatorio !== false
    }))
  };
}

export function asignado(paso, roles, usuarios) {
  if (paso.tipoAprobador === 'ROL') {
    if (paso.rol) return paso.rol;
    return roles.find((r) => String(r.id) === String(paso.idRol))?.nombre || 'Sin perfil';
  }
  if (paso.tipoAprobador === 'USUARIO') {
    if (paso.usuario) return paso.usuario;
    const u = usuarios.find((x) => String(x.idUsuario) === String(paso.idUsuario));
    return u?.nombreCompleto || u?.nombreUsuario || 'Sin usuario';
  }
  return 'Jefe del solicitante';
}

function Down({ label, tone = 'neutral' }) {
  const line = tone === 'ok' ? 'bg-emerald-400' : 'bg-slate-300';
  const tip = tone === 'ok' ? 'border-t-emerald-400' : 'border-t-slate-300';
  const text = tone === 'ok' ? 'text-emerald-700' : 'text-slate-500';
  return (
    <div className="flex flex-col items-center py-0.5">
      {label && <span className={`text-[10px] font-semibold ${text}`}>{label}</span>}
      <div className={`h-6 w-px ${line}`} />
      <div className={`h-0 w-0 border-x-[5px] border-t-[6px] border-x-transparent ${tip}`} />
    </div>
  );
}

function Terminal({ children, tone = 'start' }) {
  const skin = {
    start: 'border-line bg-white text-navy',
    ok: 'border-emerald-200 bg-emerald-50 text-ok',
    no: 'border-red-200 bg-red-50 text-danger'
  }[tone];
  return (
    <div className={`min-w-[9.5rem] rounded-full border px-4 py-2 text-center text-xs font-semibold ${skin}`}>
      {children}
    </div>
  );
}

function Diamond() {
  return (
    <div className="relative h-16 w-16 shrink-0">
      <div className="absolute left-1/2 top-1/2 h-10 w-10 -translate-x-1/2 -translate-y-1/2 rotate-45 rounded-[3px] border border-slate-300 bg-white" />
      <p className="absolute inset-0 grid place-items-center text-center text-[10px] font-semibold leading-tight text-navy">
        ¿Aprueba?
      </p>
    </div>
  );
}

function PasoBox({ paso, index, roles, usuarios }) {
  const Icon = APROBADOR[paso.tipoAprobador]?.icon || User;
  return (
    <div className="w-[220px] rounded-lg border border-line bg-white px-3 py-3">
      <div className="flex items-start gap-2">
        <span className="grid h-7 w-7 shrink-0 place-items-center rounded-md bg-navy text-[11px] font-semibold text-white">
          {paso.numeroPaso || index + 1}
        </span>
        <div className="min-w-0">
          <p className="text-sm font-semibold leading-snug text-navy">{paso.nombrePaso || 'Sin nombre'}</p>
          <p className="mt-1 flex items-center gap-1 text-xs text-muted">
            <Icon size={12} />
            {APROBADOR[paso.tipoAprobador]?.label}
          </p>
          <p className="truncate text-xs text-slate-500">{asignado(paso, roles, usuarios)}</p>
        </div>
      </div>
    </div>
  );
}

export function Flujograma({ pasos, roles, usuarios }) {
  if (!pasos?.length) {
    return <p className="text-sm text-muted">Agregue un paso para ver el flujograma.</p>;
  }
  return (
    <div className="overflow-x-auto rounded-lg bg-slate-50 px-4 py-6">
      <div className="mx-auto flex w-max min-w-[28rem] flex-col items-center px-36">
        <Terminal>Solicitud registrada</Terminal>
        <Down />
        {pasos.map((p, i) => (
          <div key={p.key || p.numeroPaso || i} className="flex flex-col items-center">
            <PasoBox paso={p} index={i} roles={roles} usuarios={usuarios} />
            <Down />
            <div className="relative flex flex-col items-center">
              <Diamond />
              <div className="absolute left-full top-1/2 flex -translate-y-1/2 items-center gap-2 pl-2">
                <span className="text-[10px] font-semibold text-red-700">No</span>
                <div className="h-px w-8 bg-red-300" />
                <Terminal tone="no">Rechazado</Terminal>
              </div>
            </div>
            <Down label="Sí" tone="ok" />
          </div>
        ))}
        <Terminal tone="ok">Aprobado</Terminal>
      </div>
      <p className="mx-auto mt-5 max-w-lg text-center text-xs text-muted">
        Al registrar la solicitud el primer paso queda en curso. Si aprueba, avanza al siguiente;
        si rechaza, la solicitud se cierra y los pasos pendientes se omiten.
      </p>
    </div>
  );
}

export function FlujogramaCompact({ pasos, roles, usuarios }) {
  if (!pasos?.length) return <p className="text-sm text-muted">Sin pasos.</p>;
  return (
    <div>
      <div className="flex flex-wrap items-center gap-x-1 gap-y-2">
        <span className="rounded-full border border-line bg-slate-50 px-2.5 py-1 text-[11px] font-medium text-navy">Solicitud</span>
        <span className="text-slate-300">→</span>
        {pasos.map((p, i) => {
          const Icon = APROBADOR[p.tipoAprobador]?.icon || User;
          return (
            <div key={p.key || p.numeroPaso || i} className="flex items-center gap-1">
              <div
                className="flex items-center gap-1.5 rounded-md border border-line bg-white px-2 py-1"
                title={`${APROBADOR[p.tipoAprobador]?.label || ''} · ${asignado(p, roles, usuarios)}`}
              >
                <span className="grid h-5 w-5 place-items-center rounded bg-navy text-[10px] font-semibold text-white">
                  {p.numeroPaso || i + 1}
                </span>
                <Icon size={12} className="text-slate-400" />
                <span className="max-w-[8rem] truncate text-[11px] font-medium text-navy">{p.nombrePaso || 'Sin nombre'}</span>
              </div>
              <span className="text-slate-300">→</span>
            </div>
          );
        })}
        <span className="rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-[11px] font-medium text-ok">Aprobado</span>
      </div>
      <p className="mt-2 text-[11px] text-muted">Si un paso rechaza, la solicitud termina rechazada.</p>
    </div>
  );
}

export function toPayload(form) {
  return {
    codigo: form.codigo.trim().toUpperCase(),
    nombre: form.nombre.trim(),
    tipoOrigen: form.tipoOrigen,
    idTipoPermiso: form.tipoOrigen === 'PERMISO' && form.idTipoPermiso ? Number(form.idTipoPermiso) : null,
    descripcion: form.descripcion || null,
    activo: form.activo !== false,
    pasos: form.pasos.map((p, i) => ({
      numeroPaso: i + 1,
      nombrePaso: p.nombrePaso,
      tipoAprobador: p.tipoAprobador,
      idRol: p.tipoAprobador === 'ROL' && p.idRol ? Number(p.idRol) : null,
      idUsuario: p.tipoAprobador === 'USUARIO' && p.idUsuario ? Number(p.idUsuario) : null,
      esObligatorio: p.esObligatorio !== false
    }))
  };
}
