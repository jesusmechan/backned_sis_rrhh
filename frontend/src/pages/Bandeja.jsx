import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { CalendarDays, Check, Download, Inbox, Timer, X } from 'lucide-react';
import { http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Avatar, Badge, Button, DatePicker, Field, Kpi, KpiRow, Modal, PageHeader, Pager, SearchField, cn, downloadBlob } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { usePagedLoad } from '../lib/usePagedLoad';
import { fmtDateTime, fmtRelative, isoDateEnZona } from '../lib/format';
import { useConfig } from '../auth/ConfigContext';
import { text } from '../lib/input';

function limaToday() {
  return isoDateEnZona();
}

function shiftDays(iso, days) {
  const [y, m, d] = iso.split('-').map(Number);
  const dt = new Date(Date.UTC(y, m - 1, d));
  dt.setUTCDate(dt.getUTCDate() + days);
  return dt.toISOString().slice(0, 10);
}

function startOfMonth(iso) {
  return `${iso.slice(0, 8)}01`;
}

export function Bandeja() {
  const { etiqueta, num, opciones } = useConfig();
  const comentarioMin = num('comentario_rechazo_min_caracteres', 1);
  const ESTADOS_SEGUIMIENTO = [{ value: '', label: 'Todos' }, ...opciones('ESTADO_SOLICITUD').map((o) => ({ value: o.codigo, label: o.nombre }))];
  const navigate = useNavigate();
  const location = useLocation();
  const [tab, setTab] = useState(location.state?.tab === 'seguimiento' ? 'seguimiento' : 'pendientes');
  const [counts, setCounts] = useState({ total: 0, permiso: 0, hora: 0, seguimiento: 0 });
  const [q, setQ, qDebounced] = useQuerySearch();
  const [tipo, setTipo] = useState('');
  const [estadoSeguimiento, setEstadoSeguimiento] = useState('');
  const [tramite, setTramite] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [orden, setOrden] = useState('DESC');
  const [rangoRapido, setRangoRapido] = useState('');
  const [tiposPermiso, setTiposPermiso] = useState([]);
  const [checked, setChecked] = useState({});
  const [ok, setOk] = useState(location.state?.ok || '');
  const [saving, setSaving] = useState(false);
  const [bulk, setBulk] = useState(null);
  const [bulkComment, setBulkComment] = useState('');
  const selectAllRef = useRef(null);

  const filtrosActivos = Boolean(
    qDebounced
    || tipo
    || estadoSeguimiento
    || tramite
    || desde
    || hasta
    || orden !== 'DESC'
  );

  const { page, setPage, rows, meta, error, setError, loading, reload } = usePagedLoad(
    [tab, qDebounced, tipo, estadoSeguimiento, tramite, desde, hasta, orden],
    (pageNum) => {
      const base = { page: pageNum, size: PAGE_SIZE, q: qDebounced, tipo, tramite, desde, hasta, orden };
      if (tab === 'pendientes') {
        return http.page(pagePath('/api/bandeja', base));
      }
      return http.page(pagePath('/api/bandeja', {
        ...base,
        vista: 'SEGUIMIENTO',
        estado: estadoSeguimiento
      }));
    }
  );

  useEffect(() => { setChecked({}); }, [tipo, qDebounced, tab, tramite, desde, hasta, estadoSeguimiento]);

  useEffect(() => {
    if (location.state?.ok) {
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, []);

  useEffect(() => {
    http.get('/api/catalogos/tipos-permiso')
      .then((rowsCat) => setTiposPermiso(Array.isArray(rowsCat) ? rowsCat : []))
      .catch(() => setTiposPermiso([]));
  }, []);

  async function loadCounts() {
    const [perm, hex, seg] = await Promise.all([
      http.page(pagePath('/api/bandeja', { page: 1, size: 1, tipo: 'PERMISO' })),
      http.page(pagePath('/api/bandeja', { page: 1, size: 1, tipo: 'HORA_EXTRA' })),
      http.page(pagePath('/api/bandeja', { page: 1, size: 1, vista: 'SEGUIMIENTO' }))
    ]);
    setCounts({
      permiso: perm.totalElements || 0,
      hora: hex.totalElements || 0,
      total: (perm.totalElements || 0) + (hex.totalElements || 0),
      seguimiento: seg.totalElements || 0
    });
  }

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  const lista = rows;
  const seleccionados = Object.values(checked).filter(Boolean).length;
  const pageIds = tab === 'pendientes' ? rows.map((it) => it.idPasoSolicitud).filter(Boolean) : [];
  const allOnPage = pageIds.length > 0 && pageIds.every((id) => checked[id]);
  const someOnPage = pageIds.some((id) => checked[id]);

  useEffect(() => {
    if (selectAllRef.current) {
      selectAllRef.current.indeterminate = someOnPage && !allOnPage;
    }
  }, [someOnPage, allOnPage]);

  function cambiarVista(next) {
    setTab(next);
    setPage(1);
    setChecked({});
  }

  function cambiarTipo(next) {
    setTipo((prev) => (prev === next ? '' : next));
    if (next === 'HORA_EXTRA') setTramite('');
    setPage(1);
  }

  function aplicarRango(preset) {
    const hoy = limaToday();
    setRangoRapido(preset);
    if (preset === 'hoy') {
      setDesde(hoy);
      setHasta(hoy);
    } else if (preset === '7d') {
      setDesde(shiftDays(hoy, -6));
      setHasta(hoy);
    } else if (preset === 'mes') {
      setDesde(startOfMonth(hoy));
      setHasta(hoy);
    } else {
      setDesde('');
      setHasta('');
    }
    setPage(1);
  }

  function limpiarFiltros() {
    setQ('');
    setTipo('');
    setEstadoSeguimiento('');
    setTramite('');
    setDesde('');
    setHasta('');
    setOrden('DESC');
    setRangoRapido('');
    setPage(1);
  }

  function toggleAll() {
    setChecked((prev) => {
      if (allOnPage) {
        const next = { ...prev };
        pageIds.forEach((id) => { delete next[id]; });
        return next;
      }
      const next = { ...prev };
      pageIds.forEach((id) => { next[id] = true; });
      return next;
    });
  }

  function abrir(it) {
    if (tab === 'pendientes') {
      navigate(`/bandeja/${it.idPasoSolicitud}`);
      return;
    }
    navigate(`/bandeja/ver/${it.tipoSolicitud === 'HORA_EXTRA' ? 'hora-extra' : 'permiso'}/${it.idSolicitud}`);
  }

  async function ejecutarMasiva() {
    const ids = Object.entries(checked).filter(([, v]) => v).map(([id]) => Number(id));
    if (!ids.length || !bulk) return;
    const comentario = bulkComment.trim() || (bulk === 'aprobar' ? 'Aprobación masiva' : '');
    if (bulk === 'rechazar' && comentario.length < comentarioMin) {
      setError(`Indique el motivo del rechazo (mínimo ${comentarioMin} caracteres).`);
      return;
    }
    setError('');
    setOk('');
    setSaving(true);
    let okCount = 0;
    try {
      for (const id of ids) {
        await http.post(`/api/pasos/${id}/${bulk}`, { comentario });
        okCount += 1;
      }
      setOk(bulk === 'aprobar'
        ? `${okCount} paso(s) aprobado(s)`
        : `${okCount} solicitud(es) rechazada(s)`);
      setChecked({});
      setBulk(null);
      setBulkComment('');
      await Promise.all([reload(), loadCounts()]);
    } catch (e) {
      setError(okCount ? `${okCount} procesado(s). Luego falló: ${e.message}` : e.message);
      await Promise.all([reload(), loadCounts()]);
    } finally {
      setSaving(false);
    }
  }

  function exportar() {
    const rowsCsv = tab === 'pendientes'
      ? [['Solicitante', 'Tipo', 'Trámite', 'Paso', 'Motivo', 'Desde']].concat(
        rows.map((it) => [it.solicitante, etiqueta('TIPO_ORIGEN_FLUJO', it.tipoSolicitud), it.tipoTramite, `${it.numeroPaso} ${it.nombrePaso}`, it.motivo, it.fechaInicio || ''])
      )
      : [['Solicitante', 'Tipo', 'Trámite', 'Estado', 'Paso', 'Motivo', 'Fecha']].concat(
        rows.map((it) => [it.solicitante, etiqueta('TIPO_ORIGEN_FLUJO', it.tipoSolicitud), it.tipoTramite, etiqueta('ESTADO_SOLICITUD', it.estadoSolicitud), it.nombrePaso, it.motivo, it.fechaInicio || ''])
      );
    const csv = rowsCsv.map((r) => r.map((c) => `"${String(c ?? '').replaceAll('"', '""')}"`).join(';')).join('\n');
    downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), `bandeja-${tab}.csv`);
  }

  const vacio = !loading && lista.length === 0;
  const vacioTexto = tab === 'pendientes'
    ? (filtrosActivos ? 'Ningún paso coincide con los filtros.' : 'No tiene pasos por atender.')
    : (filtrosActivos ? 'Ninguna solicitud coincide con los filtros.' : 'No hay solicitudes en seguimiento.');

  const opcionesTramite = tipo === 'HORA_EXTRA'
    ? [{ value: 'Horas extras', label: 'Horas extras' }]
    : [
        ...tiposPermiso.map((t) => ({ value: t.nombre, label: t.nombre })),
        ...(tipo === 'PERMISO' ? [] : [{ value: 'Horas extras', label: 'Horas extras' }])
      ];

  return (
    <div>
      <PageHeader
        kicker="Aprobaciones"
        title="Bandeja"
        subtitle="Revise los pasos que le corresponden y siga las solicitudes que ya decidió o que usted creó."
        actions={(
          <Button variant="secondary" onClick={exportar}>
            <Download size={14} /> Exportar
          </Button>
        )}
      />

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        <Kpi
          value={counts.total}
          label="Por atender"
          hint="Pasos que esperan su decisión"
          active={tab === 'pendientes'}
          onClick={() => cambiarVista('pendientes')}
        />
        <Kpi
          value={counts.seguimiento}
          label="En seguimiento"
          hint="Creadas o ya decididas por usted"
          active={tab === 'seguimiento'}
          onClick={() => cambiarVista('seguimiento')}
        />
      </KpiRow>

      {seleccionados > 0 && tab === 'pendientes' && (
        <div className="mb-3 flex flex-col gap-3 rounded-xl border border-navy/15 bg-slate-50 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm font-medium text-navy">
            {seleccionados} solicitud{seleccionados === 1 ? '' : 'es'} seleccionada{seleccionados === 1 ? '' : 's'}
          </p>
          <div className="flex flex-wrap gap-2">
            <Button
              onClick={() => { setError(''); setBulk('aprobar'); setBulkComment('Aprobación masiva'); }}
              disabled={saving}
            >
              <Check size={14} /> Aprobar
            </Button>
            <Button
              variant="danger"
              onClick={() => { setError(''); setBulk('rechazar'); setBulkComment(''); }}
              disabled={saving}
            >
              <X size={14} /> Rechazar
            </Button>
            <Button variant="ghost" onClick={() => setChecked({})} disabled={saving}>
              Quitar selección
            </Button>
          </div>
        </div>
      )}

      <div className="overflow-hidden rounded-xl border border-line bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b border-line px-4 py-3">
          <SearchField
            placeholder="Buscar colaborador, trámite, paso o motivo"
            value={q}
            onChange={(e) => { setQ(e.target.value); setPage(1); }}
          />

          <div className="flex flex-wrap items-center gap-1.5">
            <Chip active={!tipo} onClick={() => { setTipo(''); setPage(1); }}>
              Todas
              {tab === 'pendientes' && <Count>{counts.total}</Count>}
            </Chip>
            <Chip active={tipo === 'PERMISO'} onClick={() => cambiarTipo('PERMISO')}>
              <CalendarDays size={13} /> Permisos
              {tab === 'pendientes' && <Count>{counts.permiso}</Count>}
            </Chip>
            <Chip active={tipo === 'HORA_EXTRA'} onClick={() => cambiarTipo('HORA_EXTRA')}>
              <Timer size={13} /> Horas extras
              {tab === 'pendientes' && <Count>{counts.hora}</Count>}
            </Chip>
            {tab === 'seguimiento' && (
              <>
                <span className="mx-1 hidden h-4 w-px bg-line sm:block" />
                {ESTADOS_SEGUIMIENTO.map((est) => (
                  <Chip
                    key={est.value || 'todos'}
                    active={estadoSeguimiento === est.value}
                    onClick={() => { setEstadoSeguimiento(est.value); setPage(1); }}
                  >
                    {est.label}
                  </Chip>
                ))}
              </>
            )}
            {tab === 'pendientes' && !vacio && !loading && (
              <label className="ml-auto inline-flex cursor-pointer items-center gap-2 text-xs text-slate-500">
                <input
                  ref={selectAllRef}
                  type="checkbox"
                  className="accent-navy"
                  checked={allOnPage}
                  onChange={toggleAll}
                  aria-label="Seleccionar todas las de esta página"
                />
                <span className="hidden sm:inline">Página</span>
              </label>
            )}
          </div>

          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
            <label className="flex flex-col gap-1">
              <span className="text-[11px] font-medium text-slate-500">Trámite</span>
              <select
                className="w-full"
                value={tramite}
                onChange={(e) => { setTramite(e.target.value); setPage(1); }}
              >
                <option value="">Todos los trámites</option>
                {opcionesTramite.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-[11px] font-medium text-slate-500">Desde</span>
              <DatePicker
                value={desde}
                onChange={(v) => {
                  setDesde(v);
                  setRangoRapido('');
                  setPage(1);
                }}
                max={hasta || undefined}
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-[11px] font-medium text-slate-500">Hasta</span>
              <DatePicker
                value={hasta}
                onChange={(v) => {
                  setHasta(v);
                  setRangoRapido('');
                  setPage(1);
                }}
                min={desde || undefined}
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-[11px] font-medium text-slate-500">Orden</span>
              <select
                className="w-full"
                value={orden}
                onChange={(e) => { setOrden(e.target.value); setPage(1); }}
              >
                <option value="DESC">Más recientes primero</option>
                <option value="ASC">Más antiguos primero</option>
              </select>
            </label>
          </div>

          <div className="flex flex-wrap items-center gap-1.5">
            <span className="mr-1 text-[11px] font-medium text-slate-500">Rango:</span>
            <Chip active={rangoRapido === 'hoy'} onClick={() => aplicarRango(rangoRapido === 'hoy' ? '' : 'hoy')}>Hoy</Chip>
            <Chip active={rangoRapido === '7d'} onClick={() => aplicarRango(rangoRapido === '7d' ? '' : '7d')}>Últimos 7 días</Chip>
            <Chip active={rangoRapido === 'mes'} onClick={() => aplicarRango(rangoRapido === 'mes' ? '' : 'mes')}>Este mes</Chip>
            {filtrosActivos && (
              <button
                type="button"
                onClick={limpiarFiltros}
                className="ml-auto text-xs font-medium text-navy hover:underline"
              >
                Limpiar filtros
              </button>
            )}
          </div>
        </div>

        {loading ? (
          <ListaSkeleton />
        ) : vacio ? (
          <div className="px-4 py-14 text-center">
            <div className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-full bg-slate-100 text-slate-400">
              <Inbox size={22} />
            </div>
            <p className="text-sm text-muted">{vacioTexto}</p>
            {filtrosActivos && (
              <button type="button" onClick={limpiarFiltros} className="mt-3 text-sm font-medium text-navy hover:underline">
                Quitar filtros
              </button>
            )}
          </div>
        ) : (
          <div className="divide-y divide-line">
            {lista.map((it) => (
              <FilaBandeja
                key={tab === 'pendientes' ? it.idPasoSolicitud : `${it.tipoSolicitud}-${it.idSolicitud}`}
                item={it}
                tab={tab}
                checked={Boolean(checked[it.idPasoSolicitud])}
                onCheck={(e) => {
                  e.stopPropagation();
                  setChecked((c) => ({ ...c, [it.idPasoSolicitud]: e.target.checked }));
                }}
                onOpen={() => abrir(it)}
              />
            ))}
          </div>
        )}

        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </div>

      {bulk && (
        <Modal title={bulk === 'aprobar' ? 'Aprobar seleccionadas' : 'Rechazar seleccionadas'} onClose={() => !saving && setBulk(null)}>
          <p className="text-sm text-slate-600">
            {bulk === 'aprobar'
              ? `Se aprobará el paso actual de ${seleccionados} solicitud(es). Quedarán en seguimiento.`
              : `Se rechazarán ${seleccionados} solicitud(es) y el flujo se cierra.`}
          </p>
          <div className="mt-4">
            <Field label="Comentario" required={bulk === 'rechazar'}>
              <textarea
                value={bulkComment}
                onChange={(e) => setBulkComment(text(e.target.value, 400))}
                placeholder={bulk === 'rechazar' ? `Obligatorio, mínimo ${comentarioMin} caracteres` : 'Opcional'}
              />
            </Field>
          </div>
          <div className="mt-4 flex flex-wrap justify-end gap-2">
            <Button type="button" variant="secondary" disabled={saving} onClick={() => setBulk(null)}>Cancelar</Button>
            <Button type="button" variant={bulk === 'rechazar' ? 'danger' : 'primary'} disabled={saving} onClick={ejecutarMasiva}>
              {saving ? 'Procesando…' : bulk === 'aprobar' ? 'Aprobar' : 'Rechazar'}
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}

function Chip({ active, onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium transition',
        active ? 'bg-primary text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
      )}
    >
      {children}
    </button>
  );
}

function Count({ children }) {
  return <span className="tabular-nums opacity-80">{children}</span>;
}

function TipoPill({ tipo }) {
  if (tipo === 'HORA_EXTRA') {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-violet-50 px-2 py-0.5 text-[11px] font-medium text-violet-700">
        <Timer size={12} /> Horas extras
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-info-soft px-2 py-0.5 text-[11px] font-medium text-info">
      <CalendarDays size={12} /> Permiso
    </span>
  );
}

function FilaBandeja({ item, tab, checked, onCheck, onOpen }) {
  const hora = item.tipoSolicitud === 'HORA_EXTRA';
  const pendientes = tab === 'pendientes';

  return (
    <article
      className={cn(
        'border-l-[3px] transition',
        hora ? 'border-l-violet-500' : 'border-l-sky-500',
        checked ? 'bg-slate-50' : 'bg-white hover:bg-slate-50'
      )}
    >
      <div className="flex items-start gap-3 px-4 py-3.5">
        {pendientes && (
          <input
            type="checkbox"
            className="mt-3 h-4 w-4 accent-navy"
            checked={checked}
            onChange={onCheck}
            onClick={(e) => e.stopPropagation()}
            aria-label={`Seleccionar solicitud de ${item.solicitante}`}
          />
        )}
        <button type="button" onClick={onOpen} className="flex min-w-0 flex-1 items-start gap-3 text-left">
          <Avatar name={item.solicitante} />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div className="min-w-0">
                <p className="truncate font-semibold text-navy">{item.solicitante}</p>
                <p className="mt-0.5 text-xs text-muted">
                  {item.tipoTramite} · #{item.idSolicitud}
                </p>
              </div>
              <time className="shrink-0 text-[11px] text-muted" dateTime={item.fechaInicio} title={fmtDateTime(item.fechaInicio)}>
                {fmtRelative(item.fechaInicio)}
              </time>
            </div>
            {item.motivo && (
              <p className="mt-1.5 line-clamp-2 text-sm text-slate-600">{item.motivo}</p>
            )}
            <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
              <TipoPill tipo={item.tipoSolicitud} />
              {item.nombrePaso && (
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-navy">
                  {pendientes || item.estadoSolicitud === 'PENDIENTE' ? 'Paso' : 'Último'} {item.numeroPaso}: {item.nombrePaso}
                </span>
              )}
              {!pendientes && <Badge tipo="ESTADO_SOLICITUD" value={item.estadoSolicitud} />}
            </div>
          </div>
        </button>
        <Button variant="secondary" className="mt-1 hidden shrink-0 md:inline-flex" onClick={onOpen}>
          {pendientes ? 'Revisar' : 'Ver'}
        </Button>
      </div>
    </article>
  );
}

function ListaSkeleton() {
  return (
    <div className="divide-y divide-line">
      {[0, 1, 2].map((i) => (
        <div key={i} className="flex items-start gap-3 px-4 py-4">
          <div className="h-10 w-10 animate-pulse rounded-full bg-slate-100" />
          <div className="min-w-0 flex-1 space-y-2">
            <div className="h-4 w-40 animate-pulse rounded bg-slate-100" />
            <div className="h-3 w-56 animate-pulse rounded bg-slate-100" />
            <div className="h-3 w-3/4 animate-pulse rounded bg-slate-50" />
          </div>
        </div>
      ))}
    </div>
  );
}
