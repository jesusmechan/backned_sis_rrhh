import 'dart:async';

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../models/notificacion.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/common.dart';
import 'decision_screen.dart';
import 'solicitud_detalle_screen.dart';

class NotificacionesScreen extends StatefulWidget {
  const NotificacionesScreen({super.key, this.active = true, this.onChanged});

  /// Solo se sondea el servidor mientras la pestaña está visible.
  final bool active;
  final VoidCallback? onChanged;

  @override
  State<NotificacionesScreen> createState() => _NotificacionesScreenState();
}

class _NotificacionesScreenState extends State<NotificacionesScreen>
    with WidgetsBindingObserver {
  List<Notificacion> _items = [];
  bool _loading = true;
  String? _error;
  Timer? _syncTimer;
  String _signature = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load(initial: true);
    if (widget.active) _startSync();
  }

  @override
  void didUpdateWidget(covariant NotificacionesScreen old) {
    super.didUpdateWidget(old);
    if (widget.active == old.active) return;
    if (widget.active) {
      _load(silent: true);
      _startSync();
    } else {
      _syncTimer?.cancel();
    }
  }

  void _startSync() {
    _syncTimer?.cancel();
    _syncTimer = Timer.periodic(const Duration(seconds: 10), (_) => _load(silent: true));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _syncTimer?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!widget.active) return;
    if (state == AppLifecycleState.resumed) {
      _load(silent: true);
      _startSync();
    } else if (state == AppLifecycleState.paused) {
      _syncTimer?.cancel();
    }
  }

  Future<void> _load({bool initial = false, bool silent = false}) async {
    if (!silent && mounted) {
      setState(() {
        if (initial) _loading = true;
        _error = null;
      });
    }
    try {
      final list = await context.read<AuthProvider>().listarNotificaciones();
      if (!mounted) return;
      final sig = list.map((n) => '${n.idNotificacion}:${n.leida}').join('|');
      if (silent && sig == _signature) return;
      setState(() {
        _items = list;
        _signature = sig;
        _loading = false;
        _error = null;
      });
    } catch (e) {
      if (!mounted) return;
      if (!silent) {
        setState(() {
          _error = e.toString();
          _loading = false;
        });
      }
    }
  }

  Future<void> _abrir(Notificacion n) async {
    final auth = context.read<AuthProvider>();
    final nav = Navigator.of(context);
    if (!n.leida) {
      setState(() => _items = [
            for (final x in _items)
              x.idNotificacion == n.idNotificacion ? x.copyWith(leida: true) : x,
          ]);
      auth.marcarNotificacionLeida(n.idNotificacion).then((_) => widget.onChanged?.call(),
          onError: (_) {});
    }
    final tipo = n.tipo.toUpperCase();
    if (tipo == 'BANDEJA' && n.idPaso != null) {
      await nav.push(MaterialPageRoute(builder: (_) => DecisionScreen(idPaso: n.idPaso!)));
    } else if (n.idSolicitud != null && n.tipoSolicitud != null) {
      await nav.push(MaterialPageRoute(
        builder: (_) => SolicitudDetalleScreen(
          esPermiso: n.tipoSolicitud!.toUpperCase() != 'HORA_EXTRA',
          id: n.idSolicitud!,
        ),
      ));
    }
  }

  Future<void> _leerTodas() async {
    try {
      await context.read<AuthProvider>().marcarTodasLeidas();
      widget.onChanged?.call();
      await _load();
    } catch (e) {
      if (mounted) showMensaje(context, e.toString(), error: true);
    }
  }

  (IconData, Color, Color) _estilo(Notificacion n) => switch (n.tipo.toUpperCase()) {
        'APROBADA' => (Icons.check_circle_outline, AppColors.ok, AppColors.okSoft),
        'RECHAZADA' => (Icons.cancel_outlined, AppColors.danger, AppColors.dangerSoft),
        'BANDEJA' => (Icons.inbox_outlined, AppColors.warn, AppColors.warnSoft),
        _ => (Icons.notifications_none, AppColors.info, AppColors.infoSoft),
      };

  @override
  Widget build(BuildContext context) {
    final unread = _items.where((n) => !n.leida).length;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Avisos'),
        actions: [
          if (unread > 0) TextButton(onPressed: _leerTodas, child: const Text('Leer todas')),
          IconButton(
            tooltip: 'Actualizar',
            onPressed: _loading ? null : () => _load(),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () => _load(),
              child: _items.isEmpty
                  ? ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        ErrorBanner(_error, onRetry: _load),
                        const SizedBox(height: 60),
                        const EmptyState(
                          icon: Icons.notifications_none,
                          title: 'Sin avisos',
                          subtitle: 'Aquí verás las aprobaciones, rechazos y pasos por atender.',
                        ),
                      ],
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: _items.length + (_error != null ? 1 : 0),
                      separatorBuilder: (_, _) => const SizedBox(height: 8),
                      itemBuilder: (context, i) {
                        if (_error != null && i == 0) {
                          return ErrorBanner(_error, onRetry: _load);
                        }
                        final n = _items[_error != null ? i - 1 : i];
                        final (icon, fg, bg) = _estilo(n);
                        final navegable = (n.tipo.toUpperCase() == 'BANDEJA' && n.idPaso != null) ||
                            n.idSolicitud != null;
                        return Card(
                          color: n.leida ? Colors.white : AppColors.infoSoft,
                          child: ListTile(
                            onTap: () => _abrir(n),
                            leading: CircleAvatar(
                              backgroundColor: bg,
                              child: Icon(icon, color: fg, size: 20),
                            ),
                            title: Text(
                              n.titulo,
                              style: TextStyle(
                                fontWeight: n.leida ? FontWeight.w600 : FontWeight.w800,
                              ),
                            ),
                            subtitle: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                if (n.mensaje.isNotEmpty) Text(n.mensaje),
                                const SizedBox(height: 4),
                                Text(
                                  DateFormat('dd MMM · HH:mm', 'es')
                                      .format(n.fechaCreacion.toLocal()),
                                  style: const TextStyle(fontSize: 11, color: AppColors.muted),
                                ),
                              ],
                            ),
                            trailing: navegable
                                ? const Icon(Icons.chevron_right, color: AppColors.muted)
                                : null,
                            isThreeLine: true,
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}
