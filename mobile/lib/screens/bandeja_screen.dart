import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/solicitud.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/common.dart';
import 'decision_screen.dart';
import 'solicitud_detalle_screen.dart';

/// Pasos que el usuario debe aprobar y los que ya atendió.
class BandejaScreen extends StatefulWidget {
  const BandejaScreen({super.key});

  @override
  State<BandejaScreen> createState() => _BandejaScreenState();
}

class _BandejaScreenState extends State<BandejaScreen> {
  bool _seguimiento = false;
  List<BandejaItem> _items = [];
  bool _loading = true;
  bool _cambio = false;
  String? _error;

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
    try {
      final list = await context.read<AuthProvider>().bandeja(seguimiento: _seguimiento);
      if (mounted) setState(() => _items = list);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _abrir(BandejaItem i) async {
    final nav = Navigator.of(context);
    if (_seguimiento) {
      await nav.push(MaterialPageRoute(
        builder: (_) => SolicitudDetalleScreen(esPermiso: i.esPermiso, id: i.idSolicitud),
      ));
      return;
    }
    final decidido = await nav.push<bool>(
      MaterialPageRoute(builder: (_) => DecisionScreen(idPaso: i.idPasoSolicitud)),
    );
    if (decidido == true) {
      _cambio = true;
      _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) Navigator.of(context).pop(_cambio);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Bandeja de aprobación'),
          actions: [
            IconButton(
              tooltip: 'Actualizar',
              onPressed: _loading ? null : _load,
              icon: const Icon(Icons.refresh),
            ),
          ],
        ),
        body: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
              child: SizedBox(
                width: double.infinity,
                child: SegmentedButton<bool>(
                  segments: const [
                    ButtonSegment(
                      value: false,
                      icon: Icon(Icons.inbox_outlined),
                      label: Text('Por atender'),
                    ),
                    ButtonSegment(
                      value: true,
                      icon: Icon(Icons.history),
                      label: Text('Seguimiento'),
                    ),
                  ],
                  selected: {_seguimiento},
                  showSelectedIcon: false,
                  onSelectionChanged: (s) {
                    setState(() => _seguimiento = s.first);
                    _load();
                  },
                ),
              ),
            ),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator())
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: _error != null
                          ? ListView(
                              padding: const EdgeInsets.all(16),
                              children: [ErrorBanner(_error, onRetry: _load)],
                            )
                          : _items.isEmpty
                              ? ListView(
                                  children: [
                                    const SizedBox(height: 60),
                                    EmptyState(
                                      icon: Icons.task_alt,
                                      title: _seguimiento
                                          ? 'Sin solicitudes atendidas'
                                          : 'Nada por aprobar',
                                      subtitle: _seguimiento
                                          ? 'Aquí verás las solicitudes en las que participaste.'
                                          : 'Cuando alguien te envíe una solicitud aparecerá aquí.',
                                    ),
                                  ],
                                )
                              : ListView.separated(
                                  padding: const EdgeInsets.all(16),
                                  itemCount: _items.length,
                                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                                  itemBuilder: (context, idx) => _ItemCard(
                                    item: _items[idx],
                                    onTap: () => _abrir(_items[idx]),
                                  ),
                                ),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ItemCard extends StatelessWidget {
  const _ItemCard({required this.item, required this.onTap});

  final BandejaItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: item.esPermiso ? AppColors.infoSoft : AppColors.warnSoft,
                foregroundColor: item.esPermiso ? AppColors.info : AppColors.warn,
                child: Icon(item.esPermiso ? Icons.event_note_outlined : Icons.more_time),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.solicitante,
                        style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15)),
                    Text(item.tipoTramite, style: const TextStyle(color: AppColors.muted)),
                    const SizedBox(height: 4),
                    Text(item.motivo, maxLines: 2, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 6),
                    Text('Paso ${item.numeroPaso}: ${item.nombrePaso}',
                        style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  StatusChip.fromEstado(item.estadoSolicitud),
                  if (item.puedeDecidir) ...[
                    const SizedBox(height: 8),
                    const Icon(Icons.chevron_right, color: AppColors.muted),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
