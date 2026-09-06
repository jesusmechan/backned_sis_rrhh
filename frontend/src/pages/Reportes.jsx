import { useState } from 'react';
import { http } from '../api/client';
import { Alert, Button, PageHeader, Panel, downloadBlob } from '../components/ui';

const TIPOS = [
  { id: 'TRABAJADORES', label: 'Trabajadores' },
  { id: 'PERMISOS', label: 'Permisos' },
  { id: 'HORAS_EXTRAS', label: 'Horas extras' },
  { id: 'ASISTENCIA', label: 'Asistencia' },
  { id: 'USUARIOS', label: 'Usuarios' }
];

export function Reportes() {
  const [error, setError] = useState('');

  async function bajar(tipo, formato) {
    setError('');
    try {
      const blob = await http.download(`/api/reportes/${tipo}/${formato}`);
      downloadBlob(blob, `${tipo.toLowerCase()}.${formato === 'excel' ? 'xlsx' : 'pdf'}`);
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div>
      <PageHeader
        kicker="Exportación"
        title="Reportes"
        subtitle="Exportación Excel y PDF. Queda registrada en auditoría."
      />
      <Alert>{error}</Alert>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {TIPOS.map((t) => (
          <Panel key={t.id}>
            <h3 className="text-base font-semibold text-navy">{t.label}</h3>
            <div className="mt-3 flex gap-2">
              <Button onClick={() => bajar(t.id, 'excel')}>Excel</Button>
              <Button variant="secondary" onClick={() => bajar(t.id, 'pdf')}>PDF</Button>
            </div>
          </Panel>
        ))}
      </div>
    </div>
  );
}
