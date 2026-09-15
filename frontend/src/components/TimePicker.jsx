import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { Clock3, X } from 'lucide-react';

function cn(...parts) {
  return parts.filter(Boolean).join(' ');
}

function pad(n) {
  return String(n).padStart(2, '0');
}

function parseTime(value) {
  const m = /^(\d{1,2}):(\d{2})/.exec(String(value || ''));
  if (!m) return null;
  const h = Number(m[1]);
  const min = Number(m[2]);
  if (h > 23 || min > 59) return null;
  return { h, min };
}

function formatTime(value) {
  const t = parseTime(value);
  if (!t) return '';
  return `${pad(t.h)}:${pad(t.min)}`;
}

const HOURS = Array.from({ length: 24 }, (_, i) => i);
const MINUTES = Array.from({ length: 12 }, (_, i) => i * 5);

export function TimePicker({
  value = '',
  onChange,
  required,
  disabled,
  name,
  className = '',
  placeholder = 'hh:mm'
}) {
  const [open, setOpen] = useState(false);
  const [mobile, setMobile] = useState(() => typeof window !== 'undefined' && window.innerWidth < 640);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  const parsed = parseTime(value);
  const [hour, setHour] = useState(parsed?.h ?? 9);
  const [minute, setMinute] = useState(parsed ? Math.round(parsed.min / 5) * 5 % 60 : 0);
  const btnRef = useRef(null);
  const popRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    const t = parseTime(value);
    setHour(t?.h ?? 9);
    setMinute(t ? (MINUTES.includes(t.min) ? t.min : Math.round(t.min / 5) * 5 % 60) : 0);
  }, [open]);

  useLayoutEffect(() => {
    if (!open || !btnRef.current) return;
    const isMobile = window.innerWidth < 640;
    setMobile(isMobile);
    if (isMobile) return;
    const rect = btnRef.current.getBoundingClientRect();
    const width = 280;
    const left = Math.min(Math.max(8, rect.left), window.innerWidth - width - 8);
    const below = rect.bottom + 8;
    const top = below + 280 > window.innerHeight ? Math.max(8, rect.top - 288) : below;
    setPos({ top, left });
  }, [open]);

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

  function apply(h, m) {
    onChange?.(`${pad(h)}:${pad(m)}`);
    setOpen(false);
  }

  const picker = (
    <div className="w-full">
      <div className="grid grid-cols-2 gap-3">
        <div>
          <p className="mb-1 text-[11px] font-semibold uppercase tracking-wide text-slate-400">Hora</p>
          <div className="grid max-h-48 grid-cols-4 gap-1 overflow-y-auto pr-1">
            {HOURS.map((h) => (
              <button
                key={h}
                type="button"
                onClick={() => setHour(h)}
                className={cn(
                  'h-9 rounded-md text-sm font-medium',
                  hour === h ? 'bg-navy text-white' : 'bg-slate-50 text-slate-700 hover:bg-slate-100'
                )}
              >
                {pad(h)}
              </button>
            ))}
          </div>
        </div>
        <div>
          <p className="mb-1 text-[11px] font-semibold uppercase tracking-wide text-slate-400">Minutos</p>
          <div className="grid max-h-48 grid-cols-3 gap-1 overflow-y-auto pr-1">
            {MINUTES.map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => setMinute(m)}
                className={cn(
                  'h-9 rounded-md text-sm font-medium',
                  minute === m ? 'bg-navy text-white' : 'bg-slate-50 text-slate-700 hover:bg-slate-100'
                )}
              >
                {pad(m)}
              </button>
            ))}
          </div>
        </div>
      </div>
      <div className="mt-3 flex items-center justify-between gap-2 border-t border-line pt-3">
        <button
          type="button"
          className="text-sm font-medium text-navy hover:underline"
          onClick={() => {
            const n = new Date();
            apply(n.getHours(), Math.round(n.getMinutes() / 5) * 5 % 60);
          }}
        >
          Ahora
        </button>
        <div className="flex gap-2">
          {!required && value && (
            <button type="button" className="text-sm text-slate-500 hover:text-navy" onClick={() => { onChange?.(''); setOpen(false); }}>
              Limpiar
            </button>
          )}
          <button type="button" className="rounded-lg bg-navy px-3 py-1.5 text-sm font-medium text-white" onClick={() => apply(hour, minute)}>
            Listo
          </button>
        </div>
      </div>
    </div>
  );

  const popup = !open ? null : createPortal(
    mobile ? (
      <div className="fixed inset-0 z-[70] flex items-end bg-slate-900/40">
        <div ref={popRef} className="w-full rounded-t-xl bg-white px-4 pb-[max(1rem,env(safe-area-inset-bottom))] pt-3 shadow-lg">
          <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-slate-200" />
          <p className="mb-3 text-sm font-semibold text-navy">Seleccione hora</p>
          {picker}
        </div>
      </div>
    ) : (
      <div
        ref={popRef}
        className="fixed z-[70] w-[280px] rounded-xl border border-line bg-white p-3 shadow-lg"
        style={{ top: pos.top, left: pos.left }}
      >
        {picker}
      </div>
    ),
    document.body
  );

  return (
    <div className={cn('time-picker relative', className)}>
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
        <Clock3 size={16} className="shrink-0 text-slate-400" />
        <span className="min-w-0 flex-1 truncate">{formatTime(value) || placeholder}</span>
        {!required && value && !disabled && (
          <span
            role="button"
            tabIndex={-1}
            className="grid h-5 w-5 place-items-center rounded text-slate-400 hover:text-navy"
            onClick={(e) => { e.stopPropagation(); onChange?.(''); }}
            aria-label="Quitar hora"
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

TimePicker.isFieldControl = true;
