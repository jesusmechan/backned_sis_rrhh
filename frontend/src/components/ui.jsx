import { Children, isValidElement } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Search } from 'lucide-react';

function findControl(children) {
  let found = null;
  Children.forEach(children, (child) => {
    if (found || !isValidElement(child)) return;
    if (child.type?.isFieldControl) {
      found = child;
      return;
    }
    if (typeof child.type === 'string' && ['input', 'select', 'textarea'].includes(child.type)) {
      found = child;
      return;
    }
    if (child.props?.children) found = findControl(child.props.children);
  });
  return found;
}

function isBlank(value) {
  return value == null || String(value).trim() === '';
}

export function cn(...parts) {
  return parts.filter(Boolean).join(' ');
}

export function initials(name = '') {
  return name.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase() || 'A';
}

const buttonVariants = {
  primary: 'bg-primary text-white hover:bg-primary-hover',
  secondary: 'border border-line bg-white text-slate-700 hover:bg-surface',
  danger: 'bg-danger-soft text-danger hover:bg-danger-hover',
  ghost: 'text-muted hover:bg-surface'
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

export function BackLink({ to, children }) {
  const navigate = useNavigate();
  return (
    <button
      type="button"
      onClick={() => navigate(to)}
      className="mb-4 inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-navy"
    >
      <ArrowLeft size={16} /> {children}
    </button>
  );
}

export function PageHeader({ kicker, title, subtitle, actions }) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        {kicker && (
          <p className="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{kicker}</p>
        )}
        <h2 className="page-title">{title}</h2>
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
    PENDIENTE: 'bg-warn-soft text-warn',
    EN_CURSO: 'bg-warn-soft text-warn',
    APROBADO: 'bg-ok-soft text-ok',
    ACTIVO: 'bg-ok-soft text-ok',
    COMPLETADA: 'bg-ok-soft text-ok',
    INGRESO: 'bg-info-soft text-info',
    SALIDA: 'bg-slate-100 text-slate-600',
    RECHAZADO: 'bg-danger-soft text-danger',
    CANCELADO: 'bg-danger-soft text-danger',
    INACTIVO: 'bg-danger-soft text-danger',
    CESADO: 'bg-danger-soft text-danger',
    OBLIGATORIO: 'bg-slate-100 text-navy',
    OPCIONAL: 'bg-warn-soft text-warn',
    BORRADOR: 'bg-slate-100 text-muted',
    CALCULADA: 'bg-info-soft text-info',
    CERRADA: 'bg-ok-soft text-ok',
    CONTABILIZADO: 'bg-ok-soft text-ok',
    ABIERTA: 'bg-ok-soft text-ok',
    POSTULADO: 'bg-slate-100 text-muted',
    ENTREVISTA: 'bg-warn-soft text-warn',
    SELECCIONADO: 'bg-info-soft text-info',
    CONTRATADO: 'bg-ok-soft text-ok',
    DESCARTADO: 'bg-danger-soft text-danger',
    ANULADA: 'bg-danger-soft text-danger',
    ANULADO: 'bg-danger-soft text-danger'
  }[value] || 'bg-slate-100 text-muted';

  return <span className={cn('inline-block rounded-md px-2 py-0.5 text-xs font-medium', tone)}>{value || '—'}</span>;
}

export function Alert({ children, ok }) {
  if (!children) return null;
  return (
    <div className={cn('mb-4 rounded-lg px-3 py-2 text-sm', ok ? 'bg-ok-soft text-ok' : 'bg-danger-soft text-danger')}>
      {children}
    </div>
  );
}

export function Modal({ title, children, onClose }) {
  return (
    <div className="fixed inset-0 z-50 grid place-items-end bg-slate-900/40 p-0 sm:place-items-center sm:p-4">
      <div className="flex max-h-[92dvh] w-full max-w-3xl flex-col overflow-hidden rounded-t-xl bg-white shadow-lg sm:rounded-xl">
        <div className="flex shrink-0 items-start justify-between gap-3 border-b border-line px-4 py-4 sm:px-6">
          <h3 className="min-w-0 text-base font-semibold text-navy sm:text-lg">{title}</h3>
          <Button variant="secondary" type="button" className="shrink-0 px-3 py-2" onClick={onClose}>Cerrar</Button>
        </div>
        <div className="overflow-y-auto px-4 py-4 sm:px-6 sm:py-5">{children}</div>
      </div>
    </div>
  );
}

export function Field({ label, children, full, hint, required: requiredProp }) {
  const control = findControl(children);
  const required = requiredProp ?? Boolean(control?.props?.required);
  const select = control?.type === 'select' || control?.type?.isFieldControl;
  const empty = required && isBlank(control?.props?.value);
  const requiredText = select ? 'Debe seleccionar este campo' : 'Debe ingresar este campo';

  const Tag = control?.type?.isFieldControl ? 'div' : 'label';

  return (
    <Tag className={cn('flex flex-col gap-1.5', full && 'md:col-span-2')}>
      <span className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
        <span className="text-xs font-medium text-slate-500">
          {label}
          {required && <span className="ml-1 text-danger">*</span>}
        </span>
        {empty && <span className="text-[11px] font-medium text-danger">{requiredText}</span>}
      </span>
      {children}
      {hint && <span className="text-[11px] text-muted">{hint}</span>}
    </Tag>
  );
}

export function FormGrid({ children, onSubmit }) {
  return <form className="grid gap-4 md:grid-cols-2" onSubmit={onSubmit}>{children}</form>;
}

export function StackTable({ cards, table }) {
  return (
    <>
      <div className="divide-y divide-line md:hidden">{cards}</div>
      <div className="hidden overflow-x-auto md:block">{table}</div>
    </>
  );
}

/** Listado estándar: un panel, tabla en desktop y filas compactas en móvil. */
export function DataList({ empty, emptyText = 'Sin registros.', cards, table, footer }) {
  return (
    <Panel padded={false}>
      {empty ? <Empty text={emptyText} /> : <StackTable cards={cards} table={table} />}
      {footer}
    </Panel>
  );
}

export function PersonCell({ name, meta, leading, showAvatar = true }) {
  const left = leading !== undefined ? leading : (showAvatar ? <Avatar name={name} /> : null);
  return (
    <div className="flex min-w-0 items-center gap-3">
      {left}
      <div className="min-w-0">
        <p className="truncate font-medium text-navy">{name || '—'}</p>
        {meta ? <p className="truncate text-xs text-muted">{meta}</p> : null}
      </div>
    </div>
  );
}

export function MobileRow({ leading, title, meta, badge, actions }) {
  return (
    <div className="flex gap-3 px-4 py-3">
      {leading}
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <p className="font-medium text-navy">{title}</p>
            {meta ? <p className="mt-0.5 text-xs text-muted">{meta}</p> : null}
          </div>
          {badge ? <div className="shrink-0">{badge}</div> : null}
        </div>
        {actions ? <div className="mt-2 flex flex-wrap gap-2">{actions}</div> : null}
      </div>
    </div>
  );
}

export function ListActions({ children }) {
  return <div className="flex flex-wrap items-center justify-end gap-2">{children}</div>;
}

export function Kpi({ value, label, hint, active, onClick }) {
  const className = cn(
    'w-full rounded-xl border bg-white p-4 text-left',
    onClick && 'cursor-pointer hover:border-slate-300',
    active ? 'border-navy ring-1 ring-navy' : 'border-line'
  );
  const body = (
    <>
      <p className="text-2xl font-bold text-navy">{value}</p>
      <p className="mt-1 text-sm font-medium text-navy">{label}</p>
      {hint && <p className="text-xs text-muted">{hint}</p>}
    </>
  );
  if (onClick) {
    return <button type="button" className={className} onClick={onClick} aria-pressed={Boolean(active)}>{body}</button>;
  }
  return <div className={className}>{body}</div>;
}

export function KpiRow({ children, cols = 3 }) {
  const grid = cols === 2 ? 'sm:grid-cols-2' : cols === 4 ? 'sm:grid-cols-2 xl:grid-cols-4' : 'sm:grid-cols-3';
  return <div className={cn('mb-4 grid gap-3', grid)}>{children}</div>;
}

export function FilterBar({ children }) {
  return (
    <div className="filter-bar mb-4 flex flex-wrap items-center gap-2">
      {children}
    </div>
  );
}

export function SearchField({ value, onChange, placeholder }) {
  return (
    <div className="relative min-w-0 flex-1">
      <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
      <input className="pl-9" placeholder={placeholder} value={value} onChange={onChange} />
    </div>
  );
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
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line px-3 py-3 text-sm text-muted sm:px-4">
      <p className="text-xs sm:text-sm">{from}–{to} de {totalElements}</p>
      <div className="flex gap-1">
        <button type="button" disabled={page <= 1} onClick={() => onPage(page - 1)} className="h-8 rounded-md px-2 hover:bg-slate-100 disabled:opacity-40">‹</button>
        {nums.map((n) => (
          <button
            key={n}
            type="button"
            onClick={() => onPage(n)}
            className={`h-8 w-8 rounded-md text-sm ${n === page ? 'bg-primary text-white' : 'hover:bg-surface'}`}
          >
            {n}
          </button>
        ))}
        <button type="button" disabled={page >= totalPages} onClick={() => onPage(page + 1)} className="h-8 rounded-md px-2 hover:bg-slate-100 disabled:opacity-40">›</button>
      </div>
    </div>
  );
}

export { DatePicker } from './DatePicker';
export { TimePicker } from './TimePicker';

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
