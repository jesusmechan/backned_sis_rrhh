import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { AppShell } from './layout/AppShell';
import { Login } from './pages/Login';
import { Inicio } from './pages/Inicio';
import { Bandeja } from './pages/Bandeja';
import { BandejaDecision } from './pages/BandejaDecision';
import { Permisos } from './pages/Permisos';
import { PermisoNuevo } from './pages/PermisoNuevo';
import { PermisoDetalle } from './pages/PermisoDetalle';
import { HorasExtras } from './pages/HorasExtras';
import { HoraExtraNueva } from './pages/HoraExtraNueva';
import { HoraExtraDetalle } from './pages/HoraExtraDetalle';
import { Asistencia } from './pages/Asistencia';
import { Marcar } from './pages/Marcar';
import { Empleados } from './pages/Empleados';
import { Usuarios } from './pages/Usuarios';
import { Flujos } from './pages/Flujos';
import { FlujoConfig } from './pages/FlujoConfig';
import { Reportes } from './pages/Reportes';
import { Auditoria } from './pages/Auditoria';
import { AccesoRestringido } from './pages/AccesoRestringido';
import { Perfil } from './pages/Perfil';
import { Menus } from './pages/Menus';

function Private({ children, path }) {
  const { isAuth, canAccess, hasAnyRole } = useAuth();
  if (!isAuth) return <Navigate to="/login" replace />;
  if (path === '/menu' && hasAnyRole('ADMIN')) return children;
  if (path && !canAccess(path)) return <AccesoRestringido />;
  return children;
}

export default function App() {
  const { isAuth } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={isAuth ? <Navigate to="/" replace /> : <Login />} />
      <Route path="/" element={<Private><AppShell /></Private>}>
        <Route index element={<Inicio />} />
        <Route path="perfil" element={<Private path="/perfil"><Perfil /></Private>} />
        <Route path="bandeja" element={<Private path="/bandeja"><Bandeja /></Private>} />
        <Route path="bandeja/ver/:tipo/:id" element={<Private path="/bandeja"><BandejaDecision /></Private>} />
        <Route path="bandeja/:idPaso" element={<Private path="/bandeja"><BandejaDecision /></Private>} />
        <Route path="permisos" element={<Private path="/permisos"><Permisos /></Private>} />
        <Route path="permisos/nuevo" element={<Private path="/permisos"><PermisoNuevo /></Private>} />
        <Route path="permisos/:id" element={<Private path="/permisos"><PermisoDetalle /></Private>} />
        <Route path="horas-extras" element={<Private path="/horas-extras"><HorasExtras /></Private>} />
        <Route path="horas-extras/nuevo" element={<Private path="/horas-extras"><HoraExtraNueva /></Private>} />
        <Route path="horas-extras/:id" element={<Private path="/horas-extras"><HoraExtraDetalle /></Private>} />
        <Route path="marcar" element={<Private path="/marcar"><Marcar /></Private>} />
        <Route path="asistencia" element={<Private path="/asistencia"><Asistencia /></Private>} />
        <Route path="empleados" element={<Private path="/empleados"><Empleados /></Private>} />
        <Route path="usuarios" element={<Private path="/usuarios"><Usuarios /></Private>} />
        <Route path="menu" element={<Private path="/menu"><Menus /></Private>} />
        <Route path="flujos" element={<Private path="/flujos"><Flujos /></Private>} />
        <Route path="flujos/nuevo" element={<Private path="/flujos"><FlujoConfig /></Private>} />
        <Route path="flujos/:id" element={<Private path="/flujos"><FlujoConfig /></Private>} />
        <Route path="reportes" element={<Private path="/reportes"><Reportes /></Private>} />
        <Route path="auditoria" element={<Private path="/auditoria"><Auditoria /></Private>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
