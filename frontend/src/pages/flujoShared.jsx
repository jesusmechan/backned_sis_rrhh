import { ArrowDown, CornerDownLeft, FileUp, GitBranch, Scale, User, UserCog, Users } from 'lucide-react';
import { useConfig } from '../auth/ConfigContext';
import { Badge } from '../components/ui';
import { fmtShortDateTime } from '../lib/format';

/** Icono por tipo de aprobador; la etiqueta sale del catálogo TIPO_APROBADOR. */
export const APROBADOR_ICONO = {
  JEFE_INMEDIATO: Users,
  ROL: UserCog,
  USUARIO: User,
  SOLICITANTE: User
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
    esObligatorio: true,
    requiereAdjunto: false,
    tipoActividad: 'DECISION',
    accionNoConforme: 'CERRAR',
    numeroPasoDevolver: ''
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
      esObligatorio: p.esObligatorio !== false,
      requiereAdjunto: Boolean(p.requiereAdjunto),
      tipoActividad: p.tipoActividad || 'DECISION',
      accionNoConforme: p.tipoActividad && p.tipoActividad !== 'DECISION'
        ? ''
        : (p.accionNoConforme || 'CERRAR'),
      numeroPasoDevolver: p.numeroPasoDevolver || ''
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
  if (paso.tipoAprobador === 'SOLICITANTE') return 'Dueño de la solicitud';
  return 'Jefe del solicitante';
}

export function codigoDesdeNombre(nombre) {
  const slug = String(nombre || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return slug ? `CFG-${slug}`.slice(0, 40) : '';
}

const ACTIVIDAD_ICONO = {
  CARGA: FileUp,
  DERIVACION: GitBranch,
  DECISION: Scale
};

function Connector({ label, tone = 'neutral' }) {
  const line = tone === 'ok' ? 'bg-ok' : 'bg-slate-300';
  const text = tone === 'ok' ? 'text-ok' : 'text-slate-500';
  return (
    <div className="flex flex-col items-center py-1">
      {label && <span className={`mb-0.5 text-[10px] font-semibold uppercase tracking-wide ${text}`}>{label}</span>}
      <div className={`h-5 w-px ${line}`} />
      <ArrowDown size={14} className={tone === 'ok' ? 'text-ok' : 'text-slate-400'} />
    </div>
  );
}

function Terminal({ children, tone = 'start' }) {
  const skin = {
    start: 'border-line bg-white text-navy',
    ok: 'border-ok/30 bg-ok-soft text-ok',
    no: 'border-danger/30 bg-danger-soft text-danger',
    warn: 'border-warn/30 bg-warn-soft text-warn'
  }[tone];
  return (
    <div className={`rounded-full border px-4 py-1.5 text-center text-xs font-semibold ${skin}`}>
      {children}
    </div>
  );
}

function PasoBox({ paso, index, roles, usuarios }) {
  const { etiqueta } = useConfig();
  const tipo = paso.tipoActividad || 'DECISION';
  const TipoIcon = ACTIVIDAD_ICONO[tipo] || Scale;
  const Icon = APROBADOR_ICONO[paso.tipoAprobador] || User;
  const ring = {
    CARGA: 'border-info/25 bg-info-soft/40',
    DERIVACION: 'border-slate-200 bg-white',
    DECISION: 'border-warn/25 bg-warn-soft/30'
  }[tipo] || 'border-line bg-white';

  return (
    <div className={`w-full max-w-md rounded-xl border px-4 py-3 shadow-sm ${ring}`}>
      <div className="flex items-start gap-3">
        <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-primary text-xs font-semibold text-white">
          {paso.numeroPaso || index + 1}
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-sm font-semibold leading-snug text-navy">{paso.nombrePaso || 'Sin nombre'}</p>
            <Badge tipo="TIPO_ACTIVIDAD" value={tipo} />
            {paso.requiereAdjunto && (
              <span className="rounded-md bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-600">Adjunto</span>
            )}
          </div>
          <p className="mt-1.5 flex items-center gap-1.5 text-xs text-muted">
            <TipoIcon size={12} className="shrink-0" />
            {etiqueta('TIPO_ACTIVIDAD', tipo)}
            <span className="text-slate-300">·</span>
            <Icon size={12} className="shrink-0" />
            {etiqueta('TIPO_APROBADOR', paso.tipoAprobador)}
          </p>
          <p className="mt-0.5 truncate text-xs text-slate-600">{asignado(paso, roles, usuarios)}</p>
        </div>
      </div>
    </div>
  );
}

function RamaDecision({ paso }) {
  const devolver = paso.accionNoConforme === 'DEVOLVER';
  const destino = paso.numeroPasoDevolver;
  return (
    <div className="relative my-1 flex w-full max-w-md flex-col items-center">
      <div className="grid h-9 place-items-center rounded-lg border border-slate-300 bg-white px-3 text-[11px] font-semibold text-navy">
        ¿Conforme?
      </div>
      <div className="mt-2 flex w-full flex-wrap items-center justify-center gap-3 sm:justify-between">
        <div className="flex items-center gap-2 rounded-lg border border-danger/20 bg-danger-soft px-2.5 py-1.5">
          {devolver ? (
            <>
              <CornerDownLeft size={14} className="text-warn" />
              <span className="text-[11px] font-medium text-warn">
                No → vuelve al paso {destino || '?'}
              </span>
            </>
          ) : (
            <>
              <span className="text-[11px] font-semibold text-danger">No</span>
              <span className="text-[11px] text-danger">Solicitud rechazada</span>
            </>
          )}
        </div>
        <div className="flex items-center gap-1.5 text-[11px] font-semibold text-ok">
          Sí → continúa
          <ArrowDown size={12} />
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
    <div className="rounded-xl border border-line bg-slate-50/80 px-3 py-5 sm:px-6 sm:py-6">
      <div className="mx-auto flex w-full max-w-xl flex-col items-center">
        <Terminal>Solicitud registrada</Terminal>
        <Connector />
        {pasos.map((p, i) => {
          const tipo = p.tipoActividad || 'DECISION';
          const esDecision = tipo === 'DECISION';
          const labelNext = esDecision
            ? null
            : (tipo === 'CARGA' ? 'Completar' : 'Derivar');
          return (
            <div key={p.key || p.numeroPaso || i} className="flex w-full flex-col items-center">
              <PasoBox paso={p} index={i} roles={roles} usuarios={usuarios} />
              {esDecision ? (
                <>
                  <Connector />
                  <RamaDecision paso={p} />
                </>
              ) : (
                <Connector label={labelNext} tone="ok" />
              )}
              {esDecision && i < pasos.length - 1 && <div className="h-2" />}
              {esDecision && i === pasos.length - 1 && <Connector tone="ok" />}
            </div>
          );
        })}
        <Terminal tone="ok">Solicitud aprobada</Terminal>
      </div>
      <div className="mx-auto mt-6 grid max-w-xl gap-2 text-xs text-muted sm:grid-cols-3">
        <p className="rounded-lg border border-info/20 bg-info-soft/50 px-3 py-2">
          <span className="font-semibold text-info">Carga:</span> completa y avanza.
        </p>
        <p className="rounded-lg border border-line bg-white px-3 py-2">
          <span className="font-semibold text-navy">Derivación:</span> solo enruta.
        </p>
        <p className="rounded-lg border border-warn/20 bg-warn-soft/50 px-3 py-2">
          <span className="font-semibold text-warn">Decisión:</span> aprueba, cierra o devuelve.
        </p>
      </div>
    </div>
  );
}

export function FlujogramaCompact({ pasos, roles, usuarios }) {
  const { etiqueta } = useConfig();
  if (!pasos?.length) return <p className="text-sm text-muted">Sin pasos configurados.</p>;

  const skin = {
    CARGA: 'border-info/30 bg-info-soft text-info',
    DERIVACION: 'border-line bg-white text-navy',
    DECISION: 'border-warn/30 bg-warn-soft text-warn'
  };

  return (
    <div className="flex max-w-xl flex-wrap items-center gap-1.5">
      {pasos.map((p, i) => {
        const n = p.numeroPaso || i + 1;
        const tipo = p.tipoActividad || 'DECISION';
        const quien = asignado(p, roles, usuarios);
        const devolver = tipo === 'DECISION' && p.accionNoConforme === 'DEVOLVER';
        const TipoIcon = ACTIVIDAD_ICONO[tipo] || Scale;
        const tip = [
          p.nombrePaso || `Paso ${n}`,
          etiqueta('TIPO_ACTIVIDAD', tipo),
          etiqueta('TIPO_APROBADOR', p.tipoAprobador, ''),
          quien,
          p.requiereAdjunto ? 'Requiere adjunto' : null,
          devolver ? `Si no conforme → paso ${p.numeroPasoDevolver || '?'}` : null
        ].filter(Boolean).join(' · ');

        return (
          <div key={p.key || n} className="flex items-center gap-1.5">
            <div
              className={`inline-flex max-w-[11rem] items-center gap-1.5 rounded-lg border px-2 py-1 ${skin[tipo] || skin.DECISION}`}
              title={tip}
            >
              <span className="grid h-5 w-5 shrink-0 place-items-center rounded-md bg-primary text-[10px] font-semibold text-white">
                {n}
              </span>
              <TipoIcon size={12} className="shrink-0 opacity-70" />
              <span className="truncate text-[11px] font-medium">{p.nombrePaso || `Paso ${n}`}</span>
              {devolver && (
                <span className="inline-flex shrink-0 items-center gap-0.5 rounded bg-white/80 px-1 text-[10px] font-semibold text-warn" title={`Devuelve al paso ${p.numeroPasoDevolver}`}>
                  <CornerDownLeft size={10} />
                  {p.numeroPasoDevolver || '?'}
                </span>
              )}
              {p.requiereAdjunto && (
                <FileUp size={11} className="shrink-0 opacity-70" title="Requiere adjunto" />
              )}
            </div>
            {i < pasos.length - 1 && <span className="text-slate-300" aria-hidden>→</span>}
          </div>
        );
      })}
    </div>
  );
}

export function PasosInstancia({ pasos }) {
  const { etiqueta } = useConfig();
  if (!pasos?.length) return <p className="text-sm text-muted">Sin pasos instanciados.</p>;
  return (
    <ol className="space-y-3">
      {pasos.map((p) => {
        const quien = p.usuarioAsignado || p.rol || etiqueta('TIPO_APROBADOR', p.tipoAprobador);
        return (
          <li key={p.idPasoSolicitud || p.numeroPaso} className="rounded-lg border border-line bg-surface px-3 py-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-medium text-navy">Paso {p.numeroPaso}: {p.nombrePaso}</p>
              <Badge tipo="ESTADO_PASO" value={p.estado} />
            </div>
            <p className="mt-1 text-xs text-muted">Responsable: {quien}</p>
            {(p.usuarioDecision || p.fechaDecision) && (
              <p className="mt-1.5 text-xs text-slate-600">
                Decidió: <span className="font-medium text-navy">{p.usuarioDecision || '—'}</span>
                {p.fechaDecision ? ` · ${fmtShortDateTime(p.fechaDecision)}` : ''}
                {p.comentario ? ` · ${p.comentario}` : ''}
              </p>
            )}
          </li>
        );
      })}
    </ol>
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
    pasos: form.pasos.map((p, i) => {
      const numeroPaso = i + 1;
      const tipoActividad = p.tipoActividad || 'DECISION';
      const decision = tipoActividad === 'DECISION';
      const devolver = decision && p.accionNoConforme === 'DEVOLVER';
      return {
        numeroPaso,
        nombrePaso: (p.nombrePaso || '').trim(),
        tipoAprobador: p.tipoAprobador,
        idRol: p.tipoAprobador === 'ROL' && p.idRol ? Number(p.idRol) : null,
        idUsuario: p.tipoAprobador === 'USUARIO' && p.idUsuario ? Number(p.idUsuario) : null,
        esObligatorio: p.esObligatorio !== false,
        requiereAdjunto: Boolean(p.requiereAdjunto),
        tipoActividad,
        accionNoConforme: decision ? (p.accionNoConforme || 'CERRAR') : null,
        numeroPasoDevolver: devolver && p.numeroPasoDevolver ? Number(p.numeroPasoDevolver) : null
      };
    })
  };
}
