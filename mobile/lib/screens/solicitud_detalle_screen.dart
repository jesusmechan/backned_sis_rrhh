import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/permiso.dart';
import '../models/solicitud.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/common.dart';

/// Detalle de un permiso o de una hora extra: datos, circuito, historial y cancelación.
class SolicitudDetalleScreen extends StatefulWidget {
  const SolicitudDetalleScreen({super.key, required this.esPermiso, required this.id});

  final bool esPermiso;
  final int id;

  @override
  State<SolicitudDetalleScreen> createState() => _SolicitudDetalleScreenState();
}

class _SolicitudDetalleScreenState extends State<SolicitudDetalleScreen> {
  Permiso? _permiso;
  HoraExtra? _horaExtra;
  List<HistorialSolicitud> _historial = [];
  bool _loading = true;
  bool _cancelando = false;
  bool _cambio = false;
  String? _error;

  String get _estado => _permiso?.estado ?? _horaExtra?.estado ?? '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final auth = context.read<AuthProvider>();
    try {
      if (widget.esPermiso) {
        final r = await Future.wait([
          auth.obtenerPermiso(widget.id),
          auth.historialPermiso(widget.id),
        ]);
        _permiso = r[0] as Permiso;
        _historial = r[1] as List<HistorialSolicitud>;
      } else {
        final r = await Future.wait([
          auth.obtenerHoraExtra(widget.id),
          auth.historialHoraExtra(widget.id),
        ]);
        _horaExtra = r[0] as HoraExtra;
        _historial = r[1] as List<HistorialSolicitud>;
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _cancelar() async {
    final ok = await confirmar(
      context,
      titulo: 'Cancelar solicitud',
      mensaje: 'La solicitud dejará de estar en el circuito de aprobación. ¿Desea continuar?',
      aceptar: 'Cancelar solicitud',
      peligro: true,
    );
    if (!ok || !mounted) return;
    setState(() => _cancelando = true);
    final auth = context.read<AuthProvider>();
    try {
      if (widget.esPermiso) {
        await auth.cancelarPermiso(widget.id);
      } else {
        await auth.cancelarHoraExtra(widget.id);
      }
      _cambio = true;
      if (!mounted) return;
      showMensaje(context, 'Solicitud cancelada.');
      await _load();
    } catch (e) {
      if (mounted) showMensaje(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _cancelando = false);
    }
  }

  List<Widget> _datos() {
    final p = _permiso;
    if (p != null) {
      return [
        InfoRow('Tipo', p.tipoPermiso),
        InfoRow('Fechas', p.fechaInicio == p.fechaFin
            ? fechaCorta(p.fechaInicio)
            : '${fechaCorta(p.fechaInicio)} al ${fechaCorta(p.fechaFin)}'),
        if (p.horario != null) InfoRow('Horario', p.horario!),
        InfoRow('Motivo', p.motivo),
        if (p.flujo != null) InfoRow('Flujo', p.flujo!),
        InfoRow('Registrado', fechaHora(p.fechaCreacion)),
      ];
    }
    final h = _horaExtra!;
    return [
      InfoRow('Fecha', fechaCorta(h.fecha)),
      InfoRow('Horario', '${h.horaInicio} – ${h.horaFin}'),
      InfoRow('Horas', h.cantidadHoras.toStringAsFixed(h.cantidadHoras % 1 == 0 ? 0 : 1)),
      InfoRow('Motivo', h.motivo),
      if (h.flujo != null) InfoRow('Flujo', h.flujo!),
      InfoRow('Registrado', fechaHora(h.fechaCreacion)),
    ];
  }

  @override
  Widget build(BuildContext context) {
    final titulo = widget.esPermiso ? 'Permiso #${widget.id}' : 'Horas extras #${widget.id}';
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) Navigator.of(context).pop(_cambio);
      },
      child: Scaffold(
        appBar: AppBar(title: Text(titulo)),
        body: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? ListView(
                    padding: const EdgeInsets.all(16),
                    children: [ErrorBanner(_error, onRetry: _load)],
                  )
                : RefreshIndicator(
                    onRefresh: _load,
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        SectionCard(
                          title: 'Datos de la solicitud',
                          trailing: StatusChip.fromEstado(_estado),
                          child: Column(children: _datos()),
                        ),
                        const SizedBox(height: 12),
                        SectionCard(
                          title: 'Circuito de aprobación',
                          child: CircuitoAprobacion(_permiso?.pasos ?? _horaExtra!.pasos),
                        ),
                        const SizedBox(height: 12),
                        SectionCard(title: 'Historial', child: HistorialLista(_historial)),
                        if (_estado.toUpperCase() == 'PENDIENTE') ...[
                          const SizedBox(height: 16),
                          OutlinedButton.icon(
                            style: OutlinedButton.styleFrom(
                              foregroundColor: AppColors.danger,
                              side: const BorderSide(color: AppColors.danger),
                              minimumSize: const Size.fromHeight(48),
                            ),
                            onPressed: _cancelando ? null : _cancelar,
                            icon: const Icon(Icons.close),
                            label: Text(_cancelando ? 'Cancelando…' : 'Cancelar solicitud'),
                          ),
                        ],
                      ],
                    ),
                  ),
      ),
    );
  }
}
