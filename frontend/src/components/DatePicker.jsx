import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { CalendarDays, ChevronLeft, ChevronRight, X } from 'lucide-react';

function cn(...parts) {
  return parts.filter(Boolean).join(' ');
}

const WEEKDAYS = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá', 'Do'];
const MONTHS = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
];

function pad(n) {
  return String(n).padStart(2, '0');
}

export function toISODate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function todayISO() {
  return toISODate(new Date());
}

function parseISO(iso) {
  if (!iso) return null;
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(iso).slice(0, 10));
  if (!m) return null;
  const date = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatPE(iso) {
  const date = parseISO(iso);
  if (!date) return '';
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
}

function monthCells(year, month) {
  const first = new Date(year, month, 1);
  const start = (first.getDay() + 6) % 7;
  const lastDay = new Date(year, month + 1, 0).getDate();
  const cells = Array.from({ length: start }, () => null);
  for (let day = 1; day <= lastDay; day += 1) cells.push(day);
  while (cells.length % 7 !== 0) cells.push(null);
  return cells;
}

function clampView(date, min, max) {
  let next = date;
  const minDate = parseISO(min);
  const maxDate = parseISO(max);
  if (minDate && next < minDate) next = minDate;
  if (maxDate && next > maxDate) next = maxDate;
  return { year: next.getFullYear(), month: next.getMonth() };
}

export function DatePicker({
  value = '',
  onChange,
  required,
  disabled,
  min,
  max,
  name,
  className = '',
  placeholder = 'dd/mm/aaaa'
}) {
  const [open, setOpen] = useState(false);
  const [view, setView] = useState(() => clampView(parseISO(value) || new Date(), min, max));
  const [mobile, setMobile] = useState(() => typeof window !== 'undefined' && window.innerWidth < 640);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  const btnRef = useRef(null);
  const popRef = useRef(null);

  const selected = parseISO(value);
  const today = todayISO();
  const years = useMemo(() => {
    const minYear = parseISO(min)?.getFullYear() ?? 1950;
    const maxYear = parseISO(max)?.getFullYear() ?? new Date().getFullYear() + 10;
    const list = [];
    for (let y = maxYear; y >= minYear; y -= 1) list.push(y);
    return list;
  }, [min, max]);

  useEffect(() => {
    if (!open) return;
    setView(clampView(selected || new Date(), min, max));
  }, [open]);

  useLayoutEffect(() => {
    if (!open || !btnRef.current) return;
    const isMobile = window.innerWidth < 640;
    setMobile(isMobile);
    if (isMobile) return;
    const rect = btnRef.current.getBoundingClientRect();
    const width = 288;
    const left = Math.min(Math.max(8, rect.left), window.innerWidth - width - 8);
    const below = rect.bottom + 8;
    const top = below + 340 > window.innerHeight ? Math.max(8, rect.top - 348) : below;
    setPos({ top, left });
  }, [open, view]);

  useEffect(() => {
    if (!open) return;
    function onKey(e) {
      if (e.key === 'Escape') setOpen(false);
    }
    function onDown(e) {
      if (btnRef.current?.contains(e.target) || popRef.current?.contains(e.target)) return;
      setOpen(false);
    }
    document.addEventListener('keydown', onKey);
    document.addEventListener('pointerdown', onDown);
    const prev = document.body.style.overflow;
    if (window.innerWidth < 640) document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.removeEventListener('pointerdown', onDown);
      document.body.style.overflow = prev;
    };
  }, [open]);

  function emit(next) {
    onChange?.(next);
  }

  function pick(day) {
    const iso = toISODate(new Date(view.year, view.month, day));
    if ((min && iso < min) || (max && iso > max)) return;
    emit(iso);
    setOpen(false);
  }

  function shiftMonth(delta) {
    const date = new Date(view.year, view.month + delta, 1);
    setView(clampView(date, min, max));
  }

  const cells = monthCells(view.year, view.month);
  const todayBlocked = (min && today < min) || (max && today > max);

  const calendar = (
    <div className={cn(mobile ? 'w-full' : 'w-72')}>
      <div className="mb-3 flex items-center gap-2">
        <button type="button" className="grid h-9 w-9 place-items-center rounded-md text-slate-600 hover:bg-slate-100" onClick={() => shiftMonth(-1)} aria-label="Mes anterior">
          <ChevronLeft size={18} />
        </button>
        <select
          className="min-w-0 flex-1"
          value={view.month}
          onChange={(e) => setView((v) => ({ ...v, month: Number(e.target.value) }))}
        >
          {MONTHS.map((label, i) => <option key={label} value={i}>{label}</option>)}
        </select>
        <select
          className="w-[5.5rem]"
          value={view.year}
          onChange={(e) => setView((v) => ({ ...v, year: Number(e.target.value) }))}
        >
          {years.map((y) => <option key={y} value={y}>{y}</option>)}
        </select>
        <button type="button" className="grid h-9 w-9 place-items-center rounded-md text-slate-600 hover:bg-slate-100" onClick={() => shiftMonth(1)} aria-label="Mes siguiente">
          <ChevronRight size={18} />
        </button>
      </div>
      <div className="grid grid-cols-7 gap-1 text-center">
        {WEEKDAYS.map((d) => (
          <span key={d} className="py-1 text-[11px] font-semibold uppercase tracking-wide text-slate-400">{d}</span>
        ))}
        {cells.map((day, i) => {
          if (!day) return <span key={`e-${i}`} />;
          const iso = toISODate(new Date(view.year, view.month, day));
          const isSel = value === iso;
          const isToday = iso === today;
          const blocked = (min && iso < min) || (max && iso > max);
          return (
            <button
              key={iso}
              type="button"
              disabled={blocked}
              onClick={() => pick(day)}
              className={cn(
                'h-10 touch-manipulation rounded-md text-sm font-medium disabled:cursor-not-allowed disabled:opacity-30',
                isSel ? 'bg-navy text-white' : isToday ? 'bg-slate-100 text-navy' : 'text-slate-700 hover:bg-slate-100'
              )}
            >
              {day}
            </button>
          );
        })}
      </div>
      <div className="mt-3 flex items-center justify-between gap-2 border-t border-line pt-3">
        <button
          type="button"
          className="text-sm font-medium text-navy hover:underline disabled:text-slate-400"
          disabled={todayBlocked}
          onClick={() => { emit(today); setOpen(false); }}
        >
          Hoy
        </button>
        {!required && value && (
          <button type="button" className="text-sm text-slate-500 hover:text-navy" onClick={() => { emit(''); setOpen(false); }}>
            Limpiar
          </button>
        )}
      </div>
    </div>
  );

  const popup = !open ? null : createPortal(
    mobile ? (
      <div className="fixed inset-0 z-[70] flex items-end bg-slate-900/40">
        <div
          ref={popRef}
          className="w-full rounded-t-xl bg-white px-4 pb-[max(1rem,env(safe-area-inset-bottom))] pt-3 shadow-lg"
        >
          <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-slate-200" />
          <p className="mb-3 text-sm font-semibold text-navy">Seleccione fecha</p>
          {calendar}
        </div>
      </div>
    ) : (
      <div
        ref={popRef}
        className="fixed z-[70] rounded-xl border border-line bg-white p-3 shadow-lg"
        style={{ top: pos.top, left: pos.left }}
      >
        {calendar}
      </div>
    ),
    document.body
  );

  return (
    <div className={cn('date-picker relative', className)}>
      <button
        ref={btnRef}
        type="button"
        disabled={disabled}
        onClick={() => setOpen((v) => !v)}
        className={cn(
          'flex w-full touch-manipulation items-center gap-2 rounded-lg border border-line bg-white px-3 py-2.5 text-left text-base md:text-sm',
          disabled && 'cursor-not-allowed opacity-50',
          value ? 'text-slate-800' : 'text-slate-400'
        )}
        aria-haspopup="dialog"
        aria-expanded={open}
      >
        <CalendarDays size={16} className="shrink-0 text-slate-400" />
        <span className="min-w-0 flex-1 truncate">{formatPE(value) || placeholder}</span>
        {!required && value && !disabled && (
          <span
            role="button"
            tabIndex={-1}
            className="grid h-5 w-5 place-items-center rounded text-slate-400 hover:text-navy"
            onClick={(e) => { e.stopPropagation(); emit(''); }}
            aria-label="Quitar fecha"
          >
            <X size={14} />
          </span>
        )}
      </button>
      {name && <input type="hidden" name={name} value={value || ''} />}
      {popup}
    </div>
  );
}

DatePicker.isFieldControl = true;
