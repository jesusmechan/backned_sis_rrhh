import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/permiso.dart';
import '../models/solicitud.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/common.dart';
import 'hora_extra_nueva_screen.dart';
import 'permiso_nuevo_screen.dart';
import 'solicitud_detalle_screen.dart';

/// Mis permisos y mis horas extras, con acceso al detalle y a nuevas solicitudes.
class SolicitudesScreen extends StatefulWidget {
  const SolicitudesScreen({super.key, this.active = true});

  final bool active;

  @override
  State<SolicitudesScreen> createState() => _SolicitudesScreenState();
}

class _SolicitudesScreenState extends State<SolicitudesScreen> {
  bool _verPermisos = true;
  List<Permiso> _permisos = [];
  List<HoraExtra> _horas = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void didUpdateWidget(covariant SolicitudesScreen old) {
    super.didUpdateWidget(old);
    if (widget.active && !old.active) _load(silent: true);
  }

  Future<void> _load({bool silent = false}) async {
    setState(() {
      if (!silent) _loading = true;
      _error = null;
    });
    final auth = context.read<AuthProvider>();
    try {
      final r = await Future.wait([auth.listarPermisos(), auth.listarHorasExtras()]);
      if (!mounted) return;
      setState(() {
        _permisos = r[0] as List<Permiso>;
        _horas = r[1] as List<HoraExtra>;
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _nuevo() async {
    final created = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) =>
            _verPermisos ? const PermisoNuevoScreen() : const HoraExtraNuevaScreen(),
      ),
    );
    if (created == true) {
      if (mounted) showMensaje(context, 'Solicitud registrada. Se envió a aprobación.');
      _load();
    }
  }

  Future<void> _abrir({required bool esPermiso, required int id}) async {
    final cambio = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => SolicitudDetalleScreen(esPermiso: esPermiso, id: id)),
    );
    if (cambio == true) _load();
  }

  @override
  Widget build(BuildContext context) {
    final vacia = _verPermisos ? _permisos.isEmpty : _horas.isEmpty;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mis solicitudes'),
        actions: [
          IconButton(
            tooltip: 'Actualizar',
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _nuevo,
        backgroundColor: AppColors.navy,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add),
        label: Text(_verPermisos ? 'Pedir permiso' : 'Registrar horas'),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: SizedBox(
              width: double.infinity,
              child: SegmentedButton<bool>(
                segments: [
                  ButtonSegment(
                    value: true,
                    icon: const Icon(Icons.event_note_outlined),
                    label: Text('Permisos (${_permisos.length})'),
                  ),
                  ButtonSegment(
                    value: false,
                    icon: const Icon(Icons.more_time),
                    label: Text('Horas extras (${_horas.length})'),
                  ),
                ],
                selected: {_verPermisos},
                showSelectedIcon: false,
                onSelectionChanged: (s) => setState(() => _verPermisos = s.first),
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
                        : vacia
                            ? ListView(
                                children: [
                                  const SizedBox(height: 60),
                                  EmptyState(
                                    icon: _verPermisos
                                        ? Icons.event_busy_outlined
                                        : Icons.more_time,
                                    title: 'Sin solicitudes',
                                    subtitle: _verPermisos
                                        ? 'Pulsa «Pedir permiso» para registrar uno.'
                                        : 'Pulsa «Registrar horas» para solicitar horas extras.',
                                  ),
                                ],
                              )
                            : _verPermisos
                                ? _listaPermisos()
                                : _listaHoras(),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _listaPermisos() => ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 88),
        itemCount: _permisos.length,
        separatorBuilder: (_, _) => const SizedBox(height: 10),
        itemBuilder: (context, i) {
          final p = _permisos[i];
          return _SolicitudCard(
            titulo: p.tipoPermiso,
            linea: [
              p.fechaInicio == p.fechaFin
                  ? fechaCorta(p.fechaInicio)
                  : '${fechaCorta(p.fechaInicio)} al ${fechaCorta(p.fechaFin)}',
              if (p.horario != null) p.horario!,
            ].join(' · '),
            motivo: p.motivo,
            estado: p.estado,
            onTap: () => _abrir(esPermiso: true, id: p.idSolicitudPermiso),
          );
        },
      );

  Widget _listaHoras() => ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 88),
        itemCount: _horas.length,
        separatorBuilder: (_, _) => const SizedBox(height: 10),
        itemBuilder: (context, i) {
          final h = _horas[i];
          final horas = h.cantidadHoras.toStringAsFixed(h.cantidadHoras % 1 == 0 ? 0 : 1);
          return _SolicitudCard(
            titulo: '$horas h extra',
            linea: '${fechaCorta(h.fecha)} · ${h.horaInicio} – ${h.horaFin}',
            motivo: h.motivo,
            estado: h.estado,
            onTap: () => _abrir(esPermiso: false, id: h.idSolicitudHoraExtra),
          );
        },
      );
}

class _SolicitudCard extends StatelessWidget {
  const _SolicitudCard({
    required this.titulo,
    required this.linea,
    required this.motivo,
    required this.estado,
    required this.onTap,
  });

  final String titulo;
  final String linea;
  final String motivo;
  final String estado;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(titulo,
                        style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15)),
                  ),
                  StatusChip.fromEstado(estado),
                ],
              ),
              const SizedBox(height: 6),
              Text(linea, style: const TextStyle(color: AppColors.muted)),
              const SizedBox(height: 4),
              Text(motivo, maxLines: 2, overflow: TextOverflow.ellipsis),
            ],
          ),
        ),
      ),
    );
  }
}
