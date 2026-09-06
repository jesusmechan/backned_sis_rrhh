import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Button, Panel } from '../components/ui';

export function AccesoRestringido() {
  const { usuario, perfil } = useAuth();
  return (
    <Panel className="max-w-xl">
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Acceso restringido</p>
      <h2 className="mt-1 text-2xl font-bold text-navy">Esta opción no está en su perfil</h2>
      <p className="mt-2 text-sm text-muted">
        Sesión actual: <b>{usuario?.nombreUsuario}</b> · {perfil || usuario?.rol}.
        El menú se carga según el perfil asignado a su cuenta.
      </p>
      <div className="mt-5 flex gap-2">
        <Link to="/"><Button variant="secondary">Volver al inicio</Button></Link>
      </div>
    </Panel>
  );
}
