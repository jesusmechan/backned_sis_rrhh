import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/permiso.dart';
import '../models/solicitud.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/common.dart';

/// Aprobar o rechazar un paso de la bandeja, viendo la solicitud y su circuito.
class DecisionScreen extends StatefulWidget {
  const DecisionScreen({super.key, required this.idPaso});

  final int idPaso;

  @override
  State<DecisionScreen> createState() => _DecisionScreenState();
}

class _DecisionScreenState extends State<DecisionScreen> {
  final _comentarioCtrl = TextEditingController();
  BandejaItem? _item;
  Permiso? _permiso;
  HoraExtra? _horaExtra;
  bool _loading = true;
  bool _enviando = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _comentarioCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final auth = context.read<AuthProvider>();
    try {
      final item = await auth.paso(widget.idPaso);
      _item = item;
      if (item.esPermiso) {
        _permiso = await auth.obtenerPermiso(item.idSolicitud);
      } else {
        _horaExtra = await auth.obtenerHoraExtra(item.idSolicitud);
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _decidir(bool aprobar) async {
    var comentario = _comentarioCtrl.text.trim();
    if (!aprobar && comentario.length < 3) {
      showMensaje(context, 'Indique el motivo del rechazo (mínimo 3 caracteres).', error: true);
      return;
    }
    if (aprobar && comentario.isEmpty) comentario = 'Conforme';
    final ok = await confirmar(
      context,
      titulo: aprobar ? 'Aprobar solicitud' : 'Rechazar solicitud',
      mensaje: aprobar
          ? 'Se aprobará este paso y la solicitud seguirá su circuito.'
          : 'La solicitud quedará rechazada y se notificará al solicitante.',
      aceptar: aprobar ? 'Aprobar' : 'Rechazar',
      peligro: !aprobar,
    );
    if (!ok || !mounted) return;
    setState(() => _enviando = true);
    try {
      await context
          .read<AuthProvider>()
          .decidir(widget.idPaso, aprobar: aprobar, comentario: comentario);
      if (!mounted) return;
      showMensaje(context, aprobar ? 'Paso aprobado.' : 'Solicitud rechazada.');
      Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) showMensaje(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  List<Widget> _datos() {
    final i = _item!;
    final p = _permiso;
    final h = _horaExtra;
    return [
      InfoRow('Solicitante', i.solicitante),
      InfoRow('Trámite', i.tipoTramite),
      if (p != null) ...[
        InfoRow('Fechas', p.fechaInicio == p.fechaFin
            ? fechaCorta(p.fechaInicio)
            : '${fechaCorta(p.fechaInicio)} al ${fechaCorta(p.fechaFin)}'),
        if (p.horario != null) InfoRow('Horario', p.horario!),
      ],
      if (h != null) ...[
        InfoRow('Fecha', fechaCorta(h.fecha)),
        InfoRow('Horario', '${h.horaInicio} – ${h.horaFin}'),
        InfoRow('Horas', h.cantidadHoras.toStringAsFixed(h.cantidadHoras % 1 == 0 ? 0 : 1)),
      ],
      InfoRow('Motivo', i.motivo),
      InfoRow('Paso actual', '${i.numeroPaso}. ${i.nombrePaso}'),
    ];
  }

  @override
  Widget build(BuildContext context) {
    final item = _item;
    return Scaffold(
      appBar: AppBar(title: const Text('Revisar solicitud')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null || item == null
              ? ListView(
                  padding: const EdgeInsets.all(16),
                  children: [ErrorBanner(_error ?? 'No se encontró el paso.', onRetry: _load)],
                )
              : ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    SectionCard(
                      title: item.esPermiso ? 'Permiso' : 'Horas extras',
                      trailing: StatusChip.fromEstado(item.estadoSolicitud),
                      child: Column(children: _datos()),
                    ),
                    const SizedBox(height: 12),
                    SectionCard(
                      title: 'Circuito de aprobación',
                      child: CircuitoAprobacion(_permiso?.pasos ?? _horaExtra?.pasos ?? const []),
                    ),
                    const SizedBox(height: 12),
                    if (item.puedeDecidir) ...[
                      TextField(
                        controller: _comentarioCtrl,
                        maxLines: 3,
                        maxLength: 400,
                        decoration: const InputDecoration(
                          labelText: 'Comentario',
                          hintText: 'Opcional al aprobar; obligatorio al rechazar',
                          alignLabelWithHint: true,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Expanded(
                            child: OutlinedButton.icon(
                              style: OutlinedButton.styleFrom(
                                foregroundColor: AppColors.danger,
                                side: const BorderSide(color: AppColors.danger),
                                minimumSize: const Size.fromHeight(48),
                              ),
                              onPressed: _enviando ? null : () => _decidir(false),
                              icon: const Icon(Icons.close),
                              label: const Text('Rechazar'),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: FilledButton.icon(
                              style: FilledButton.styleFrom(
                                backgroundColor: AppColors.ok,
                                minimumSize: const Size.fromHeight(48),
                              ),
                              onPressed: _enviando ? null : () => _decidir(true),
                              icon: const Icon(Icons.check),
                              label: Text(_enviando ? 'Enviando…' : 'Aprobar'),
                            ),
                          ),
                        ],
                      ),
                    ] else
                      const Card(
                        child: Padding(
                          padding: EdgeInsets.all(16),
                          child: Text(
                            'Este paso ya fue atendido o no le corresponde decidirlo.',
                            style: TextStyle(color: AppColors.muted),
                          ),
                        ),
                      ),
                  ],
                ),
    );
  }
}
