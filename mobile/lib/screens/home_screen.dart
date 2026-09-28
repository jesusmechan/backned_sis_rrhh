import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../models/marcacion.dart';
import '../models/solicitud.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../utils/lima_time.dart';
import '../widgets/common.dart';
import 'bandeja_screen.dart';
import 'hora_extra_nueva_screen.dart';
import 'permiso_nuevo_screen.dart';

/// Resumen del día: marcación, trámites propios pendientes y pasos por aprobar.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, this.onOpenTab, this.active = true, this.onChanged});

  final void Function(int index)? onOpenTab;
  final bool active;
  final VoidCallback? onChanged;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  Marcacion? _entrada;
  Marcacion? _salida;
  int _misPendientes = 0;
  int _porAprobar = 0;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void didUpdateWidget(covariant HomeScreen old) {
    super.didUpdateWidget(old);
    if (widget.active && !old.active) _load();
  }

  Future<void> _load() async {
    final auth = context.read<AuthProvider>();
    setState(() => _error = null);
    try {
      final r = await Future.wait([
        auth.marcacionesHoy(),
        auth.listarPermisos(estado: 'PENDIENTE'),
        auth.listarHorasExtras(),
        auth.contarPorAtender(),
      ]);
      if (!mounted) return;
      final marcas = r[0] as List<Marcacion>;
      final horas = r[2] as List<HoraExtra>;
      setState(() {
        _entrada = marcas.where((m) => m.esIngreso).firstOrNull;
        _salida = marcas.where((m) => m.esSalida).firstOrNull;
        _misPendientes = (r[1] as List).length +
            horas.where((h) => h.estado.toUpperCase() == 'PENDIENTE').length;
        _porAprobar = r[3] as int;
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _push(Widget screen) async {
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => screen),
    );
    if (changed == true) {
      widget.onChanged?.call();
      _load();
    }
  }

  String _hora(Marcacion? m) =>
      m == null ? '--:--' : DateFormat('HH:mm').format(LimaTime.of(m.fechaHora));

  @override
  Widget build(BuildContext context) {
    final u = context.watch<AuthProvider>().usuario!;
    final ahora = LimaTime.now();
    final finde = ahora.weekday == DateTime.saturday || ahora.weekday == DateTime.sunday;
    final (estadoDia, tono) = finde
        ? ('Fin de semana', ChipTone.neutral)
        : _salida != null
            ? ('Jornada completa', ChipTone.ok)
            : _entrada != null
                ? ('En jornada', ChipTone.info)
                : ('Sin marcar', ChipTone.warn);

    return Scaffold(
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _load,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 24),
            children: [
              const Text(
                'ANDINA RR. HH.',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 1.6,
                  color: AppColors.muted,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                'Hola, ${u.displayName.split(' ').first}',
                style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w800),
              ),
              Text(
                DateFormat("EEEE d 'de' MMMM", 'es').format(ahora),
                style: const TextStyle(color: AppColors.muted),
              ),
              const SizedBox(height: 16),
              ErrorBanner(_error, onRetry: _load),
              _HoyCard(
                entrada: _hora(_entrada),
                salida: _hora(_salida),
                estado: estadoDia,
                tono: tono,
                loading: _loading,
                puedeMarcar: !finde && _salida == null,
                onMarcar: () => widget.onOpenTab?.call(1),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: _Contador(
                      icon: Icons.inbox_outlined,
                      valor: _porAprobar,
                      titulo: 'Por aprobar',
                      destacado: _porAprobar > 0,
                      onTap: () => _push(const BandejaScreen()),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _Contador(
                      icon: Icons.hourglass_top,
                      valor: _misPendientes,
                      titulo: 'Mis pendientes',
                      onTap: () => widget.onOpenTab?.call(2),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              const Text('Accesos rápidos',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              Card(
                child: Column(
                  children: [
                    _Acceso(
                      icon: Icons.event_note_outlined,
                      titulo: 'Pedir permiso',
                      subtitulo: 'Por días, por horas o vacaciones',
                      onTap: () => _push(const PermisoNuevoScreen()),
                    ),
                    const Divider(height: 1),
                    _Acceso(
                      icon: Icons.more_time,
                      titulo: 'Registrar horas extras',
                      subtitulo: 'Enviar a aprobación',
                      onTap: () => _push(const HoraExtraNuevaScreen()),
                    ),
                    const Divider(height: 1),
                    _Acceso(
                      icon: Icons.fact_check_outlined,
                      titulo: 'Bandeja de aprobación',
                      subtitulo: _porAprobar > 0
                          ? '$_porAprobar solicitud(es) esperan su decisión'
                          : 'Pendientes y seguimiento',
                      onTap: () => _push(const BandejaScreen()),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HoyCard extends StatelessWidget {
  const _HoyCard({
    required this.entrada,
    required this.salida,
    required this.estado,
    required this.tono,
    required this.loading,
    required this.puedeMarcar,
    required this.onMarcar,
  });

  final String entrada;
  final String salida;
  final String estado;
  final ChipTone tono;
  final bool loading;
  final bool puedeMarcar;
  final VoidCallback onMarcar;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(colors: [AppColors.navy, AppColors.navyHover]),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text('Mi jornada de hoy',
                    style: TextStyle(color: Colors.white70, fontWeight: FontWeight.w600)),
              ),
              if (!loading) StatusChip(label: estado, tone: tono),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              _HoraBloque(label: 'Entrada', hora: entrada),
              const SizedBox(width: 24),
              _HoraBloque(label: 'Salida', hora: salida),
            ],
          ),
          if (puedeMarcar) ...[
            const SizedBox(height: 14),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                style: FilledButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: AppColors.navy,
                ),
                onPressed: onMarcar,
                icon: const Icon(Icons.fingerprint),
                label: Text(entrada == '--:--' ? 'Marcar entrada' : 'Marcar salida'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _HoraBloque extends StatelessWidget {
  const _HoraBloque({required this.label, required this.hora});

  final String label;
  final String hora;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(color: Colors.white70, fontSize: 12)),
        Text(
          hora,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 26,
            fontWeight: FontWeight.w800,
            fontFeatures: [FontFeature.tabularFigures()],
          ),
        ),
      ],
    );
  }
}

class _Contador extends StatelessWidget {
  const _Contador({
    required this.icon,
    required this.valor,
    required this.titulo,
    required this.onTap,
    this.destacado = false,
  });

  final IconData icon;
  final int valor;
  final String titulo;
  final VoidCallback onTap;
  final bool destacado;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: destacado ? AppColors.warnSoft : null,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Icon(icon, color: destacado ? AppColors.warn : AppColors.info),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('$valor',
                        style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
                    Text(titulo,
                        style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Acceso extends StatelessWidget {
  const _Acceso({
    required this.icon,
    required this.titulo,
    required this.subtitulo,
    required this.onTap,
  });

  final IconData icon;
  final String titulo;
  final String subtitulo;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: onTap,
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: AppColors.infoSoft,
          borderRadius: BorderRadius.circular(10),
        ),
        child: Icon(icon, color: AppColors.info),
      ),
      title: Text(titulo, style: const TextStyle(fontWeight: FontWeight.w700)),
      subtitle: Text(subtitulo),
      trailing: const Icon(Icons.chevron_right, color: AppColors.muted),
    );
  }
}
