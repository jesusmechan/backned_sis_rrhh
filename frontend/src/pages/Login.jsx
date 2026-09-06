import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Field } from '../components/ui';

export function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [nombreUsuario, setNombreUsuario] = useState('cmendoza');
  const [password, setPassword] = useState('Andina2026');
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
      <form className="w-full max-w-md rounded-xl border border-line bg-white p-8 shadow-sm" onSubmit={onSubmit}>
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Consultora Contable Andina</p>
        <h1 className="mt-1 text-2xl font-bold text-navy">Ingreso al sistema</h1>
        <p className="mt-1 mb-6 text-sm text-muted">Gestión de RR. HH.</p>
        <Alert>{error}</Alert>
        <div className="grid gap-4">
          <Field label="Usuario">
            <input value={nombreUsuario} onChange={(e) => setNombreUsuario(e.target.value)} required autoComplete="username" />
          </Field>
          <Field label="Contraseña">
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
          </Field>
          <Button className="w-full" disabled={loading}>{loading ? 'Ingresando…' : 'Ingresar'}</Button>
        </div>
        <p className="mt-4 text-xs text-muted">
          Prueba: ediaz, mquispe, cmendoza, lbenavides. Contraseña: Andina2026.
        </p>
      </form>
    </div>
  );
}
