import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';

const COLORS = {
  pend: '#b45309',
  apro: '#047857',
  rech: '#dc2626',
  primary: '#0f172a',
  muted: '#94a3b8',
  info: '#0369a1',
  soft: '#e2e8f0'
};

const tooltipStyle = {
  borderRadius: 8,
  border: '1px solid #e2e8f0',
  fontSize: 12,
  boxShadow: '0 4px 12px rgba(15,23,42,0.08)'
};

export function ChartCard({ title, subtitle, action, children, className = '' }) {
  return (
    <section className={`overflow-hidden rounded-xl border border-line bg-white shadow-sm ${className}`}>
      <div className="flex items-start justify-between gap-3 border-b border-line px-5 py-4">
        <div className="min-w-0">
          <h3 className="text-sm font-semibold text-navy">{title}</h3>
          {subtitle && <p className="mt-0.5 text-xs text-muted">{subtitle}</p>}
        </div>
        {action}
      </div>
      <div className="px-3 pb-4 pt-3 sm:px-5">{children}</div>
    </section>
  );
}

export function EstadoDonut({ pendientes = 0, aprobados = 0, rechazados = 0, height = 220 }) {
  const data = [
    { name: 'Pendientes', value: pendientes, color: COLORS.pend },
    { name: 'Aprobados', value: aprobados, color: COLORS.apro },
    { name: 'Rechazados', value: rechazados, color: COLORS.rech }
  ].filter((d) => d.value > 0);

  const total = pendientes + aprobados + rechazados;

  if (total === 0) {
    return (
      <div className="grid h-[220px] place-items-center text-sm text-muted">
        Sin datos para graficar
      </div>
    );
  }

  return (
    <div className="relative" style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            innerRadius={58}
            outerRadius={82}
            paddingAngle={2}
            stroke="#fff"
            strokeWidth={2}
          >
            {data.map((entry) => (
              <Cell key={entry.name} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip
            contentStyle={tooltipStyle}
            formatter={(value, name) => [`${value}`, name]}
          />
          <Legend
            verticalAlign="bottom"
            height={28}
            iconType="circle"
            wrapperStyle={{ fontSize: 12 }}
          />
        </PieChart>
      </ResponsiveContainer>
      <div className="pointer-events-none absolute inset-0 flex items-center justify-center pb-7">
        <div className="text-center">
          <p className="text-2xl font-bold tabular-nums text-navy">{total}</p>
          <p className="text-[11px] text-muted">Total</p>
        </div>
      </div>
    </div>
  );
}

export function ComparativoBarras({ permisos, hextras, height = 240 }) {
  const showPermisos = Boolean(permisos);
  const showHextras = Boolean(hextras);
  const data = [
    {
      estado: 'Pendientes',
      ...(showPermisos ? { Permisos: permisos.pend ?? 0 } : {}),
      ...(showHextras ? { 'Horas extras': hextras.pend ?? 0 } : {})
    },
    {
      estado: 'Aprobados',
      ...(showPermisos ? { Permisos: permisos.apro ?? 0 } : {}),
      ...(showHextras ? { 'Horas extras': hextras.apro ?? 0 } : {})
    },
    {
      estado: 'Rechazados',
      ...(showPermisos ? { Permisos: permisos.rech ?? 0 } : {}),
      ...(showHextras ? { 'Horas extras': hextras.rech ?? 0 } : {})
    }
  ];
  const hasData = data.some((d) => (d.Permisos || 0) > 0 || (d['Horas extras'] || 0) > 0);
  if (!hasData) {
    return (
      <div className="grid h-[240px] place-items-center text-sm text-muted">
        Sin trámites para comparar
      </div>
    );
  }

  return (
    <div style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} barGap={6} barCategoryGap="28%">
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
          <XAxis dataKey="estado" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
          <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} width={32} />
          <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#f8fafc' }} />
          <Legend wrapperStyle={{ fontSize: 12 }} iconType="circle" />
          {showPermisos && <Bar dataKey="Permisos" fill={COLORS.info} radius={[6, 6, 0, 0]} maxBarSize={28} />}
          {showHextras && <Bar dataKey="Horas extras" fill={COLORS.primary} radius={[6, 6, 0, 0]} maxBarSize={28} />}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function PlanillaBarras({ rows = [], height = 220 }) {
  const data = [...rows]
    .slice(0, 6)
    .reverse()
    .map((p) => ({
      periodo: p.periodo || `${p.mes}/${p.anio}`,
      Neto: Number(p.totalNeto) || 0,
      Bruto: Number(p.totalBruto) || 0
    }));

  if (data.length === 0) {
    return (
      <div className="grid h-[220px] place-items-center text-sm text-muted">
        Aún no hay planillas
      </div>
    );
  }

  return (
    <div style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} barGap={4}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
          <XAxis dataKey="periodo" tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} />
          <YAxis
            tick={{ fontSize: 11, fill: '#64748b' }}
            axisLine={false}
            tickLine={false}
            width={48}
            tickFormatter={(v) => (v >= 1000 ? `${Math.round(v / 1000)}k` : String(v))}
          />
          <Tooltip
            contentStyle={tooltipStyle}
            formatter={(value) =>
              Number(value).toLocaleString('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 })
            }
          />
          <Legend wrapperStyle={{ fontSize: 12 }} iconType="circle" />
          <Bar dataKey="Bruto" fill={COLORS.soft} radius={[4, 4, 0, 0]} maxBarSize={22} />
          <Bar dataKey="Neto" fill={COLORS.apro} radius={[4, 4, 0, 0]} maxBarSize={22} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export { COLORS as DASHBOARD_COLORS };
