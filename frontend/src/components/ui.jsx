export function cn(...parts) {
  return parts.filter(Boolean).join(' ');
}

export function initials(name = '') {
  return name.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase() || 'A';
}

const buttonVariants = {
  primary: 'bg-navy text-white hover:bg-slate-800',
  secondary: 'border border-line bg-white text-slate-700 hover:bg-slate-50',
  danger: 'bg-red-50 text-red-700 hover:bg-red-100',
  ghost: 'text-slate-600 hover:bg-slate-100'
};

export function Button({ children, variant = 'primary', className = '', ...props }) {
  return (
    <button
      className={cn(
        'inline-flex cursor-pointer items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-50',
        buttonVariants[variant],
        className
      )}
      {...props}
    >
      {children}
    </button>
  );
}

export function PageHeader({ kicker, title, subtitle, actions }) {
  return (
    <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div>
        {kicker && (
          <p className="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{kicker}</p>
        )}
        <h2 className="text-3xl font-bold text-navy">{title}</h2>
        {subtitle && <p className="mt-2 max-w-2xl text-sm text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function Panel({ children, className = '', padded = true, title }) {
  return (
    <div className={cn('rounded-xl border border-line bg-white shadow-sm', padded && 'p-5', className)}>
      {title && <h3 className="mb-4 text-sm font-semibold text-navy">{title}</h3>}
      {children}
    </div>
  );
}

export function Avatar({ name, className = '' }) {
  return (
    <div className={cn('grid h-10 w-10 shrink-0 place-items-center rounded-full bg-slate-200 text-xs font-semibold text-navy', className)}>
      {initials(name)}
    </div>
  );
}

export function Badge({ value }) {
  const tone = {
    PENDIENTE: 'bg-amber-50 text-warn',
    EN_CURSO: 'bg-amber-50 text-warn',
    APROBADO: 'bg-emerald-50 text-ok',
    ACTIVO: 'bg-emerald-50 text-ok',
    COMPLETADA: 'bg-emerald-50 text-ok',
    INGRESO: 'bg-sky-50 text-sky-700',
    SALIDA: 'bg-slate-100 text-slate-600',
    RECHAZADO: 'bg-red-50 text-danger',
    CANCELADO: 'bg-red-50 text-danger',
    INACTIVO: 'bg-red-50 text-danger',
    CESADO: 'bg-red-50 text-danger',
    OBLIGATORIO: 'bg-slate-100 text-navy',
    OPCIONAL: 'bg-amber-50 text-warn'
  }[value] || 'bg-slate-100 text-muted';

  return <span className={cn('inline-block rounded-md px-2 py-0.5 text-xs font-medium', tone)}>{value || '—'}</span>;
}

export function Alert({ children, ok }) {
  if (!children) return null;
  return (
    <div className={cn('mb-4 rounded-lg px-3 py-2 text-sm', ok ? 'bg-emerald-50 text-ok' : 'bg-red-50 text-danger')}>
      {children}
    </div>
  );
}

export function Modal({ title, children, onClose }) {
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-900/40 p-4" onClick={onClose}>
      <div className="max-h-[90vh] w-full max-w-3xl overflow-auto rounded-xl bg-white p-6 shadow-lg" onClick={(e) => e.stopPropagation()}>
        <div className="mb-5 flex items-start justify-between gap-4">
          <h3 className="text-lg font-semibold text-navy">{title}</h3>
          <Button variant="secondary" type="button" onClick={onClose}>Cerrar</Button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Field({ label, children, full }) {
  return (
    <label className={cn('flex flex-col gap-1.5', full && 'md:col-span-2')}>
      <span className="text-xs font-medium text-slate-500">{label}</span>
      {children}
    </label>
  );
}

export function FormGrid({ children, onSubmit }) {
  return <form className="grid gap-4 md:grid-cols-2" onSubmit={onSubmit}>{children}</form>;
}

export function Empty({ text }) {
  return <p className="px-4 py-12 text-center text-sm text-muted">{text}</p>;
}

export function Pager({ page = 1, totalPages = 1, totalElements = 0, size = 10, onPage }) {
  if (totalElements === 0) return null;
  const from = (page - 1) * size + 1;
  const to = Math.min(page * size, totalElements);
  const start = Math.max(1, page - 2);
  const end = Math.min(totalPages, start + 4);
  const nums = [];
  for (let n = start; n <= end; n += 1) nums.push(n);
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line px-4 py-3 text-sm text-muted">
      <p>Mostrando {from}–{to} de {totalElements}</p>
      <div className="flex gap-1">
        <button type="button" disabled={page <= 1} onClick={() => onPage(page - 1)} className="h-8 rounded-md px-2 hover:bg-slate-100 disabled:opacity-40">‹</button>
        {nums.map((n) => (
          <button
            key={n}
            type="button"
            onClick={() => onPage(n)}
            className={`h-8 w-8 rounded-md text-sm ${n === page ? 'bg-navy text-white' : 'hover:bg-slate-100'}`}
          >
            {n}
          </button>
        ))}
        <button type="button" disabled={page >= totalPages} onClick={() => onPage(page + 1)} className="h-8 rounded-md px-2 hover:bg-slate-100 disabled:opacity-40">›</button>
      </div>
    </div>
  );
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
