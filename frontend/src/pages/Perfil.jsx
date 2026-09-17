import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { http } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Badge, Button, Field, PageHeader, Panel } from '../components/ui';

const SEXO = { M: 'Masculino', F: 'Femenino' };
const CONTRATO = {
  PLANILLA: 'Planilla',
  RECIBO_HONORARIOS: 'Recibo por honorarios',
  PRACTICAS: 'Prácticas'
};

function formatDate(value) {
  if (!value) return '—';
  return new Date(`${value}T00:00:00`).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  });
}

function formatDateTime(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function modalidadLabel(value) {
  if (value === 'PRACTICANTE') return 'Practicante';
  if (value === 'COLABORADOR') return 'Colaborador';
  return value || '—';
}

function Item({ label, value }) {
  return (
    <div>
      <p className="text-[11px] font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-0.5 break-any text-sm font-medium text-navy">{value || '—'}</p>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <section>
      <h4 className="mb-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">{title}</h4>
      <div className="grid gap-4 sm:grid-cols-2">{children}</div>
    </section>
  );
}

export function Perfil() {
  const { usuario, canAccess } = useAuth();
  const [cuenta, setCuenta] = useState(null);
  const [ficha, setFicha] = useState(null);
  const [vacaciones, setVacaciones] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [confirma, setConfirma] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function load() {
      const me = await http.get('/api/usuarios/me');
      setCuenta(me);
      if (me.idEmpleado) {
        setFicha(await http.get(`/api/empleados/${me.idEmpleado}`));
        try {
          setVacaciones(await http.get(`/api/contratos/saldo-vacaciones?idEmpleado=${me.idEmpleado}`));
        } catch {
          setVacaciones(null);
        }
      }
    }
    load().catch((e) => setError(e.message));
  }, []);

  async function cambiarPassword(e) {
    e.preventDefault();
    setError('');
    setOk('');
    if (nueva.length < 6) {
      setError('La nueva contraseña debe tener al menos 6 caracteres.');
      return;
    }
    if (nueva !== confirma) {
      setError('La confirmación no coincide con la nueva contraseña.');
      return;
    }
    setSaving(true);
    try {
      await http.put('/api/usuarios/me/password', { actual, nueva });
      setOk('Contraseña actualizada.');
      setActual('');
      setNueva('');
      setConfirma('');
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  const nombre = ficha?.nombreCompleto || usuario?.nombreCompleto || usuario?.nombreUsuario;
  const puedePermiso = canAccess('/permisos');
  const ganados = Number(vacaciones?.diasGanados || 0);
  const usados = Number(vacaciones?.diasUsados || 0);
  const disponibles = Number(vacaciones?.diasDisponibles || 0);
  const vacPct = ganados > 0 ? Math.min(100, Math.round((usados / ganados) * 100)) : 0;

  return (
    <div>
      <PageHeader
        kicker="Cuenta"
        title="Mi perfil"
        subtitle="Ficha de personal, saldo de vacaciones y acceso al sistema."
      />
      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <Panel className="mb-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <Avatar name={nombre} className="h-16 w-16 text-base" />
          <div className="min-w-0 flex-1">
            <p className="text-lg font-semibold text-navy">{nombre}</p>
            <p className="text-sm text-muted">
              {ficha?.cargo || 'Sin cargo'}
              {ficha?.area ? ` · ${ficha.area}` : ''}
            </p>
            <p className="mt-1 text-xs text-muted">
              {cuenta?.nombreUsuario || usuario?.nombreUsuario}
              {cuenta?.correo ? ` · ${cuenta.correo}` : ''}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {ficha?.codigoEmpleado && (
              <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-navy">
                {ficha.codigoEmpleado}
              </span>
            )}
            <Badge value={cuenta?.perfil || cuenta?.rol || usuario?.perfil || usuario?.rol} />
            <Badge value={ficha?.estado || (cuenta?.activo === false ? 'INACTIVO' : 'ACTIVO')} />
          </div>
        </div>
        <dl className="mt-5 grid gap-4 border-t border-line pt-4 sm:grid-cols-3">
          <Item label="Ingreso" value={formatDate(ficha?.fechaIngreso)} />
          <Item label="Último acceso" value={formatDateTime(cuenta?.ultimoAcceso)} />
          <Item
            label="Vacaciones"
            value={vacaciones ? `${disponibles} día${disponibles === 1 ? '' : 's'} disponible${disponibles === 1 ? '' : 's'}` : '—'}
          />
        </dl>
      </Panel>

      <div className="grid items-start gap-5 lg:grid-cols-12">
        <div className="space-y-5 lg:col-span-8">
          {ficha ? (
            <Panel>
              <h3 className="mb-5 text-sm font-semibold text-navy">Ficha de personal</h3>
              <div className="space-y-6">
                <Section title="Identidad">
                  <Item label="Nombres" value={ficha.nombres} />
                  <Item label="Apellido paterno" value={ficha.apellidoPaterno} />
                  <Item label="Apellido materno" value={ficha.apellidoMaterno} />
                  <Item label="Sexo" value={SEXO[ficha.sexo] || ficha.sexo} />
                  <Item
                    label="Documento"
                    value={ficha.tipoDocumento && ficha.numeroDocumento
                      ? `${ficha.tipoDocumento} ${ficha.numeroDocumento}`
                      : ficha.numeroDocumento}
                  />
                  <Item label="Fecha de nacimiento" value={formatDate(ficha.fechaNacimiento)} />
                </Section>
                <div className="border-t border-line" />
                <Section title="Contacto">
                  <Item label="Correo institucional" value={ficha.correoInstitucional} />
                  <Item label="Correo personal" value={ficha.correoPersonal} />
                  <Item label="Teléfono" value={ficha.telefono} />
                  <Item label="Dirección" value={ficha.direccion} />
                </Section>
                <div className="border-t border-line" />
                <Section title="Laboral">
                  <Item label="Área" value={ficha.area} />
                  <Item label="Cargo" value={ficha.cargo} />
                  <Item label="Horario" value={ficha.horario} />
                  <Item label="Jefe inmediato" value={ficha.jefeInmediato} />
                  <Item label="Tipo de contrato" value={CONTRATO[ficha.tipoContrato] || ficha.tipoContrato} />
                  <Item label="Modalidad" value={modalidadLabel(vacaciones?.modalidad)} />
                  <Item label="Estado" value={ficha.estado} />
                  <Item label="Fecha de cese" value={formatDate(ficha.fechaCese)} />
                </Section>
              </div>
            </Panel>
          ) : !error && (
            <Panel>
              <p className="text-sm text-muted">Esta cuenta no tiene ficha de personal asociada.</p>
            </Panel>
          )}
        </div>

        <div className="space-y-5 lg:col-span-4">
          {vacaciones && (
            <Panel>
              <h3 className="mb-3 text-sm font-semibold text-navy">Vacaciones</h3>
              <p className="text-4xl font-bold tabular-nums text-navy">{disponibles}</p>
              <p className="mt-1 text-sm text-muted">días disponibles</p>
              <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-slate-100">
                <div className="h-full rounded-full bg-navy" style={{ width: `${vacPct}%` }} />
              </div>
              <p className="mt-2 text-xs text-muted">
                {usados} usados de {ganados} ganados
                {vacaciones.tasaMensual != null ? ` · ${vacaciones.tasaMensual} por mes` : ''}
              </p>
              <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-line pt-4">
                <Item label="Meses de vínculo" value={`${vacaciones.mesesCompletos}`} />
                <Item label="Acumula desde" value={formatDate(vacaciones.fechaInicioAcumulacion)} />
              </dl>
              {puedePermiso && disponibles > 0 && (
                <Link to="/permisos/nuevo" state={{ vacaciones: true }} className="mt-4 block">
                  <Button type="button" className="w-full">Solicitar vacaciones</Button>
                </Link>
              )}
            </Panel>
          )}

          <Panel>
            <h3 className="mb-4 text-sm font-semibold text-navy">Acceso</h3>
            <div className="grid gap-4">
              <Item label="Usuario" value={cuenta?.nombreUsuario || usuario?.nombreUsuario} />
              <Item label="Correo de cuenta" value={cuenta?.correo || usuario?.correo} />
              <Item label="Perfil" value={cuenta?.perfil || usuario?.perfil || cuenta?.rol || usuario?.rol} />
            </div>
            <form className="mt-5 grid gap-4 border-t border-line pt-5" onSubmit={cambiarPassword}>
              <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">Cambiar contraseña</p>
              <Field label="Contraseña actual" required={false}>
                <div className="relative">
                  <input
                    type={showPass ? 'text' : 'password'}
                    className="pr-11"
                    value={actual}
                    onChange={(e) => setActual(e.target.value)}
                    required
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-navy"
                    onClick={() => setShowPass((v) => !v)}
                    aria-label={showPass ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  >
                    {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </Field>
              <Field label="Nueva contraseña" required={false}>
                <input
                  type={showPass ? 'text' : 'password'}
                  value={nueva}
                  onChange={(e) => setNueva(e.target.value)}
                  required
                  minLength={6}
                  autoComplete="new-password"
                />
              </Field>
              <Field label="Confirmar nueva" required={false}>
                <input
                  type={showPass ? 'text' : 'password'}
                  value={confirma}
                  onChange={(e) => setConfirma(e.target.value)}
                  required
                  minLength={6}
                  autoComplete="new-password"
                />
              </Field>
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Actualizar contraseña'}</Button>
            </form>
          </Panel>
        </div>
      </div>
    </div>
  );
}
