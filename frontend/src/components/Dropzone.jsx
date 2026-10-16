import { useId, useRef, useState } from 'react';
import { FileText, Image, UploadCloud, X } from 'lucide-react';
import { cn } from './ui';

function extensionDe(nombre) {
  const i = String(nombre || '').lastIndexOf('.');
  return i < 0 ? '' : nombre.slice(i + 1).toLowerCase();
}

export function peso(bytes) {
  const n = Number(bytes) || 0;
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${Math.round(n / 1024)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
}

const claveDe = (file) => `${file.name}-${file.size}-${file.lastModified}`;

export function Dropzone({
  files = [],
  onChange,
  extensiones = [],
  maxArchivos = 1,
  maxBytes,
  label = 'Arrastre sus archivos aquí',
  hint,
  disabled = false
}) {
  const inputId = useId();
  const inputRef = useRef(null);
  const profundidad = useRef(0);
  const [activo, setActivo] = useState(false);
  const [aviso, setAviso] = useState('');
  const lleno = files.length >= maxArchivos;
  const bloqueado = disabled || lleno;
  const accept = extensiones.map((e) => `.${e}`).join(',');

  function agregar(lista) {
    const entrantes = Array.from(lista || []);
    if (!entrantes.length) return;
    const actuales = new Set(files.map(claveDe));
    const aceptados = [];
    const rechazos = [];
    for (const file of entrantes) {
      if (extensiones.length && !extensiones.includes(extensionDe(file.name))) {
        rechazos.push(`"${file.name}" no tiene un formato permitido.`);
      } else if (maxBytes && file.size > maxBytes) {
        rechazos.push(`"${file.name}" supera ${peso(maxBytes)}.`);
      } else if (file.size === 0) {
        rechazos.push(`"${file.name}" está vacío.`);
      } else if (!actuales.has(claveDe(file))) {
        actuales.add(claveDe(file));
        aceptados.push(file);
      }
    }
    const disponibles = Math.max(0, maxArchivos - files.length);
    if (aceptados.length > disponibles) {
      rechazos.push(`Solo se admiten ${maxArchivos} archivo${maxArchivos === 1 ? '' : 's'}.`);
    }
    setAviso(rechazos.join(' '));
    if (aceptados.length && disponibles) onChange([...files, ...aceptados.slice(0, disponibles)]);
  }

  function quitar(file) {
    setAviso('');
    onChange(files.filter((f) => claveDe(f) !== claveDe(file)));
  }

  function abrir() {
    if (!bloqueado) inputRef.current?.click();
  }

  return (
    <div>
      <div
        role="button"
        tabIndex={bloqueado ? -1 : 0}
        aria-disabled={bloqueado}
        aria-describedby={`${inputId}-hint`}
        onClick={abrir}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            abrir();
          }
        }}
        onDragEnter={(e) => {
          e.preventDefault();
          if (bloqueado) return;
          profundidad.current += 1;
          setActivo(true);
        }}
        onDragOver={(e) => {
          e.preventDefault();
          e.dataTransfer.dropEffect = bloqueado ? 'none' : 'copy';
        }}
        onDragLeave={(e) => {
          e.preventDefault();
          profundidad.current = Math.max(0, profundidad.current - 1);
          if (profundidad.current === 0) setActivo(false);
        }}
        onDrop={(e) => {
          e.preventDefault();
          profundidad.current = 0;
          setActivo(false);
          if (!bloqueado) agregar(e.dataTransfer.files);
        }}
        className={cn(
          'flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed px-4 py-6 text-center outline-none transition-colors',
          'focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/30',
          bloqueado
            ? 'cursor-not-allowed border-line bg-slate-50 opacity-60'
            : activo
              ? 'cursor-copy border-primary bg-primary/5'
              : 'cursor-pointer border-line bg-white hover:border-primary/60 hover:bg-slate-50'
        )}
      >
        <UploadCloud className={cn('h-7 w-7', activo ? 'text-primary' : 'text-slate-400')} aria-hidden />
        <p className="text-sm font-medium text-navy">
          {lleno ? 'Límite de archivos alcanzado' : activo ? 'Suelte para adjuntar' : label}
        </p>
        {!lleno && (
          <p className="text-xs text-muted">
            o <span className="font-medium text-primary underline underline-offset-2">seleccione desde su equipo</span>
          </p>
        )}
        <p id={`${inputId}-hint`} className="text-[11px] text-slate-500">
          {hint}
        </p>
        <input
          ref={inputRef}
          id={inputId}
          type="file"
          className="hidden"
          multiple={maxArchivos > 1}
          accept={accept}
          onChange={(e) => {
            agregar(e.target.files);
            e.target.value = '';
          }}
        />
      </div>

      {aviso && <p className="mt-2 text-xs text-danger" role="alert">{aviso}</p>}

      {files.length > 0 && (
        <ul className="mt-2 space-y-1.5">
          {files.map((file) => {
            const Icono = extensionDe(file.name) === 'pdf' ? FileText : Image;
            return (
              <li key={claveDe(file)} className="flex items-center gap-2 rounded-md border border-line bg-white px-3 py-2 text-sm">
                <Icono className="h-4 w-4 shrink-0 text-slate-500" aria-hidden />
                <span className="min-w-0 flex-1 truncate text-slate-700" title={file.name}>{file.name}</span>
                <span className="shrink-0 text-xs text-muted">{peso(file.size)}</span>
                <button
                  type="button"
                  onClick={() => quitar(file)}
                  disabled={disabled}
                  className="shrink-0 cursor-pointer rounded p-1 text-slate-400 hover:bg-danger-soft hover:text-danger disabled:cursor-not-allowed"
                  aria-label={`Quitar ${file.name}`}
                >
                  <X className="h-4 w-4" aria-hidden />
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
