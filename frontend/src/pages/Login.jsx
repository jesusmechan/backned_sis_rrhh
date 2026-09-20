import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Field } from '../components/ui';
import { username } from '../lib/input';

export function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [nombreUsuario, setNombreUsuario] = useState('juan.espinoza');
  const [password, setPassword] = useState('Andina2026');
  const [showPass, setShowPass] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(nombreUsuario, password);
      navigate('/');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid min-h-screen place-items-center bg-surface px-4">
      <form className="w-full max-w-md rounded-xl border border-line bg-white p-5 shadow-sm sm:p-8" onSubmit={onSubmit}>
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Consultora Contable Andina</p>
        <h1 className="mt-1 text-2xl font-bold text-navy">Ingreso al sistema</h1>
        <p className="mt-1 mb-6 text-sm text-muted">Gestión de RR. HH.</p>
        <Alert>{error}</Alert>
        <div className="grid gap-4">
          <Field label="Usuario">
            <input value={nombreUsuario} onChange={(e) => setNombreUsuario(username(e.target.value))} required autoComplete="username" />
          </Field>
          <Field label="Contraseña">
            <div className="relative">
              <input
                type={showPass ? 'text' : 'password'}
                className="pr-11"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
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
          <Button className="w-full" disabled={loading}>{loading ? 'Ingresando…' : 'Ingresar'}</Button>
        </div>
        <p className="mt-4 break-any text-xs text-muted">
          Prueba: juan.espinoza, jesus.pantoja, carla.reyes, jesus.mechan. Contraseña: Andina2026.
        </p>
      </form>
    </div>
  );
}
