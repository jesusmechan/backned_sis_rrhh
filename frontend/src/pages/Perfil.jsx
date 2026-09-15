import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { http } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Badge, Button, Field, Panel } from '../components/ui';

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
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function Item({ label, value }) {
  return (
    <div>
      <p className="text-xs text-slate-500">{label}</p>
      <p className="mt-0.5 text-sm font-medium text-navy">{value || '—'}</p>
    </div>
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

  return (
    <div className="mx-auto max-w-4xl">
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Cuenta</p>
      <h1 className="page-title mt-1">Mi perfil</h1>
      <p className="mt-2 mb-5 text-sm text-muted">Ficha de personal, saldo de vacaciones y cambio de contraseña.</p>
      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <Panel className="mb-4">
        <div className="flex flex-wrap items-center gap-4">
          <Avatar name={nombre} className="h-14 w-14 text-sm" />
          <div className="min-w-0 flex-1">
            <p className="text-lg font-semibold text-navy">{nombre}</p>
            <p className="text-sm text-muted">
              {ficha?.cargo || 'Sin cargo'}
              {ficha?.area ? ` · ${ficha.area}` : ''}
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
      </Panel>

      {ficha ? (
        <>
          <Panel title="Identidad" className="mb-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Item label="Nombres" value={ficha.nombres} />
              <Item label="Apellido paterno" value={ficha.apellidoPaterno} />
              <Item label="Apellido materno" value={ficha.apellidoMaterno} />
              <Item label="Sexo" value={SEXO[ficha.sexo] || ficha.sexo} />
              <Item label="Documento" value={ficha.tipoDocumento && ficha.numeroDocumento ? `${ficha.tipoDocumento} ${ficha.numeroDocumento}` : ficha.numeroDocumento} />
              <Item label="Fecha de nacimiento" value={formatDate(ficha.fechaNacimiento)} />
            </div>
          </Panel>

          <Panel title="Contacto" className="mb-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Item label="Correo institucional" value={ficha.correoInstitucional} />
              <Item label="Correo personal" value={ficha.correoPersonal} />
              <Item label="Teléfono" value={ficha.telefono} />
              <Item label="Dirección" value={ficha.direccion} />
            </div>
          </Panel>

          <Panel title="Datos laborales" className="mb-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Item label="Área" value={ficha.area} />
              <Item label="Cargo" value={ficha.cargo} />
              <Item label="Horario" value={ficha.horario} />
              <Item label="Jefe inmediato" value={ficha.jefeInmediato} />
              <Item label="Tipo de contrato" value={CONTRATO[ficha.tipoContrato] || ficha.tipoContrato} />
              <Item label="Modalidad" value={vacaciones?.modalidad === 'PRACTICANTE' ? 'Practicante' : vacaciones?.modalidad === 'COLABORADOR' ? 'Colaborador' : '—'} />
              <Item label="Estado" value={ficha.estado} />
              <Item label="Fecha de ingreso" value={formatDate(ficha.fechaIngreso)} />
              <Item label="Fecha de cese" value={formatDate(ficha.fechaCese)} />
            </div>
          </Panel>

          {vacaciones && (
            <Panel title="Vacaciones" className="mb-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <Item label="Días disponibles" value={`${vacaciones.diasDisponibles}`} />
                <Item label="Días ganados" value={`${vacaciones.diasGanados} (${vacaciones.tasaMensual} por mes)`} />
                <Item label="Días usados" value={`${vacaciones.diasUsados}`} />
                <Item label="Meses de vínculo" value={`${vacaciones.mesesCompletos}`} />
                <Item label="Acumula desde" value={formatDate(vacaciones.fechaInicioAcumulacion)} />
                <Item label="Modalidad" value={vacaciones.modalidad === 'PRACTICANTE' ? 'Practicante' : 'Colaborador'} />
              </div>
              {puedePermiso && Number(vacaciones.diasDisponibles) > 0 && (
                <div className="mt-4">
                  <Link to="/permisos/nuevo" state={{ vacaciones: true }}>
                    <Button type="button">Solicitar vacaciones</Button>
                  </Link>
                </div>
              )}
            </Panel>
          )}
        </>
      ) : !error && (
        <Panel className="mb-4">
          <p className="text-sm text-muted">Esta cuenta no tiene ficha de personal asociada.</p>
        </Panel>
      )}

      <Panel title="Acceso al sistema" className="mb-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Item label="Usuario" value={cuenta?.nombreUsuario || usuario?.nombreUsuario} />
          <Item label="Correo de cuenta" value={cuenta?.correo || usuario?.correo} />
          <Item label="Perfil" value={cuenta?.perfil || usuario?.perfil || cuenta?.rol || usuario?.rol} />
          <Item label="Código de perfil" value={cuenta?.rol || usuario?.rol} />
          <Item label="Último acceso" value={formatDateTime(cuenta?.ultimoAcceso)} />
        </div>
      </Panel>

      <Panel title="Cambiar contraseña">
        <form className="grid gap-4 sm:grid-cols-2" onSubmit={cambiarPassword}>
          <Field label="Contraseña actual" full>
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
          <Field label="Nueva contraseña">
            <input
              type={showPass ? 'text' : 'password'}
              value={nueva}
              onChange={(e) => setNueva(e.target.value)}
              required
              minLength={6}
              autoComplete="new-password"
            />
          </Field>
          <Field label="Confirmar nueva">
            <input
              type={showPass ? 'text' : 'password'}
              value={confirma}
              onChange={(e) => setConfirma(e.target.value)}
              required
              minLength={6}
              autoComplete="new-password"
            />
          </Field>
          <div className="flex items-end sm:col-span-2">
            <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Actualizar contraseña'}</Button>
          </div>
        </form>
      </Panel>
    </div>
  );
}
