import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { http } from '../api/client';
import { Alert, Avatar, BackLink, Badge, Button, Field, Modal } from '../components/ui';
import { SolicitudHistorial } from '../components/solicitud/SolicitudHistorial';
import { ORIGEN, PasosInstancia } from './flujoShared';
import { TIPO, factsOf, solicitudPath } from './bandejaShared';
import { text } from '../lib/input';

export function BandejaDecision() {
  const { idPaso, tipo, id } = useParams();
  const navigate = useNavigate();
  const deciding = Boolean(idPaso);
  const [item, setItem] = useState(null);
  const [detalle, setDetalle] = useState(null);
  const [historial, setHistorial] = useState([]);
  const [comentario, setComentario] = useState('');
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [confirmReject, setConfirmReject] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setError('');
      setOk('');
      setLoading(true);
      try {
        let tipoSolicitud;
        let idSolicitud;
        let bandejaItem = null;

        if (deciding) {
          bandejaItem = await http.get(`/api/pasos/${idPaso}`);
          tipoSolicitud = bandejaItem.tipoSolicitud;
          idSolicitud = bandejaItem.idSolicitud;
        } else {
          tipoSolicitud = tipo === 'hora-extra' ? 'HORA_EXTRA' : 'PERMISO';
          idSolicitud = Number(id);
        }

        const base = solicitudPath(tipoSolicitud, idSolicitud);
        const [d, h] = await Promise.all([
          http.get(base),
          http.get(`${base}/historial`).catch(() => [])
        ]);
        if (cancelled) return;
        setItem(bandejaItem || {
          tipoSolicitud,
          idSolicitud,
          solicitante: d.empleado,
          tipoTramite: d.tipoPermiso || 'Horas extras',
          motivo: d.motivo
        });
        setDetalle(d);
        setHistorial(h || []);
      } catch (e) {
        if (!cancelled) {
          setError(e.message);
          setItem(null);
          setDetalle(null);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [idPaso, tipo, id, deciding]);

  function pedirRechazo() {
    if (!idPaso) return;
    if (comentario.trim().length < 3) {
      setError('Indique el motivo del rechazo.');
      return;
    }
    setError('');
    setConfirmReject(true);
  }

  async function decidir(accion) {
    if (!idPaso) return;
    const textComentario = comentario.trim();
    if (accion === 'rechazar' && textComentario.length < 3) {
      setError('Indique el motivo del rechazo.');
      setConfirmReject(false);
      return;
    }
    setError('');
    setOk('');
    setSaving(true);
    try {
      await http.post(`/api/pasos/${idPaso}/${accion}`, { comentario: textComentario || 'Conforme' });
      navigate('/bandeja', {
        replace: true,
        state: {
          ok: accion === 'aprobar' ? 'Paso aprobado. La solicitud queda en seguimiento, solo lectura.' : 'Solicitud rechazada. Queda visible en seguimiento.',
          tab: 'seguimiento'
        }
      });
    } catch (e) {
      setError(e.message);
      setConfirmReject(false);
    } finally {
      setSaving(false);
    }
  }

  const facts = factsOf(detalle, item?.tipoSolicitud);
  const pasoActual = (detalle?.pasos || []).find((p) => String(p.idPasoSolicitud) === String(idPaso));

  return (
    <div>
      <BackLink to="/bandeja">Bandeja</BackLink>

      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Aprobaciones</p>
          <h1 className="page-title mt-1">
            {deciding ? 'Revisar solicitud' : 'Solicitud'}
          </h1>
          {item && (
            <p className="mt-2 text-sm text-muted">
              {ORIGEN[item.tipoSolicitud] || TIPO[item.tipoSolicitud]} · #{item.idSolicitud}
              {pasoActual ? ` · Paso ${pasoActual.numeroPaso}: ${pasoActual.nombrePaso}` : ''}
            </p>
          )}
        </div>
        {detalle?.estado && <Badge value={detalle.estado} />}
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      {loading ? (
        <p className="text-sm text-muted">Cargando solicitud…</p>
      ) : !detalle ? null : (
        <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
          <section className="rounded-xl border border-line bg-white p-5">
            <div className="flex items-center gap-3">
              <Avatar name={item?.solicitante} />
              <div>
                <p className="font-semibold text-navy">{item?.solicitante}</p>
                <p className="text-xs text-muted">{item?.tipoTramite}</p>
              </div>
            </div>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              {facts.map(([label, value]) => (
                <div key={label}>
                  <p className="text-xs text-slate-500">{label}</p>
                  <p className="mt-0.5 text-sm font-medium text-navy">{value || '—'}</p>
                </div>
              ))}
            </div>
            {(detalle.motivo || item?.motivo) && (
              <div className="mt-5">
                <p className="text-xs text-slate-500">Motivo</p>
                <p className="mt-1 text-sm text-slate-700">{detalle.motivo || item.motivo}</p>
              </div>
            )}

            {deciding && (
              <div className="mt-6 border-t border-line pt-5">
                <Field label="Comentario">
                  <textarea
                    value={comentario}
                    onChange={(e) => setComentario(text(e.target.value, 400))}
                    placeholder="Obligatorio al rechazar"
                  />
                </Field>
                <div className="mt-4 flex flex-wrap gap-2">
                  <Button disabled={saving} onClick={() => decidir('aprobar')}>
                    {saving ? 'Guardando…' : 'Aprobar paso'}
                  </Button>
                  <Button variant="danger" disabled={saving} onClick={pedirRechazo}>
                    Rechazar solicitud
                  </Button>
                  <Button type="button" variant="secondary" onClick={() => navigate('/bandeja')}>
                    Cancelar
                  </Button>
                </div>
              </div>
            )}
          </section>

          <div className="space-y-4">
            <section className="rounded-xl border border-line bg-white p-5">
              <p className="mb-3 text-sm font-semibold text-navy">Circuito</p>
              <PasosInstancia pasos={detalle.pasos || []} />
            </section>
            <SolicitudHistorial historial={historial} />
          </div>
        </div>
      )}

      {confirmReject && (
        <Modal title="Rechazar solicitud" onClose={() => !saving && setConfirmReject(false)}>
          <p className="text-sm text-slate-600">
            ¿Confirma el rechazo? El flujo se cierra y la solicitud quedará visible en seguimiento.
          </p>
          {comentario.trim() && (
            <p className="mt-3 rounded-lg border border-line bg-surface px-3 py-2 text-sm text-navy">
              <span className="block text-xs font-medium text-muted">Motivo</span>
              {comentario.trim()}
            </p>
          )}
          <div className="mt-5 flex flex-wrap justify-end gap-2">
            <Button type="button" variant="secondary" disabled={saving} onClick={() => setConfirmReject(false)}>
              Volver
            </Button>
            <Button type="button" variant="danger" disabled={saving} onClick={() => decidir('rechazar')}>
              {saving ? 'Procesando…' : 'Confirmar rechazo'}
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}
