import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../models/marcacion.dart';
import '../models/solicitud.dart';
import '../models/usuario.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../utils/lima_time.dart';
import '../widgets/common.dart';
import '../widgets/responsive.dart';
import 'bandeja_screen.dart';
import 'hora_extra_nueva_screen.dart';
import 'permiso_nuevo_screen.dart';

/// Resumen del día: marcación, trámites propios pendientes y pasos por aprobar.
class HomeScreen extends StatefulWidget {
  const HomeScreen({
    super.key,
    this.onOpenTab,
    this.active = true,
    this.onChanged,
  });

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
      m == null ? '-- : --' : DateFormat('HH:mm').format(LimaTime.of(m.fechaHora));

  String _fechaBonita(DateTime d) {
    final raw = DateFormat("EEEE d 'de' MMMM", 'es').format(d);
    return raw.split(' ').map((w) {
      if (w.isEmpty || w == 'de') return w;
      return '${w[0].toUpperCase()}${w.substring(1)}';
    }).join(' ');
  }

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
    final firstName = u.displayName.split(' ').first;
    final tokens = AppColors.tokens(context);
    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: SafeArea(
        child: ResponsiveBody(
          child: RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              padding: Breakpoints.pagePadding(context),
              children: [
                _HeaderBar(
                  usuario: u,
                  onProfile: () => widget.onOpenTab?.call(ShellTabs.perfil),
                ),
                const SizedBox(height: 18),
                Text(
                  'Hola, $firstName 👋',
                  style: TextStyle(
                    fontSize: Breakpoints.isPhone(context) ? 28 : 32,
                    fontWeight: FontWeight.w800,
                    height: 1.15,
                    letterSpacing: -0.4,
                    color: AppColors.inkOf(context),
                  ),
                ),
                const SizedBox(height: 6),
                Row(
                  children: [
                    Container(
                      width: 7,
                      height: 7,
                      decoration: const BoxDecoration(
                        color: Color(0xFF3B82F6),
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      _fechaBonita(ahora),
                      style: TextStyle(
                        color: AppColors.mutedOf(context),
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 18),
                ErrorBanner(_error, onRetry: _load),
                _HoyCard(
                  entrada: _hora(_entrada),
                  salida: _hora(_salida),
                  estado: estadoDia,
                  tono: tono,
                  loading: _loading,
                  puedeMarcar: !finde && _salida == null,
                  onMarcar: () => widget.onOpenTab?.call(ShellTabs.marcar),
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    Expanded(
                      child: _Contador(
                        icon: Icons.description_outlined,
                        iconBg: tokens.softInfo,
                        iconFg: tokens.infoFg,
                        valor: _porAprobar,
                        titulo: 'Por aprobar',
                        onTap: () => _push(const BandejaScreen()),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _Contador(
                        icon: Icons.schedule_outlined,
                        iconBg: tokens.softWarn,
                        iconFg: tokens.warnFg,
                        valor: _misPendientes,
                        titulo: 'Mis pendientes',
                        onTap: () => widget.onOpenTab?.call(ShellTabs.solicitudes),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 22),
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        'Accesos rápidos',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w800,
                          color: tokens.ink,
                        ),
                      ),
                    ),
                    TextButton(
                      onPressed: () => widget.onOpenTab?.call(ShellTabs.solicitudes),
                      child: Text(
                        'Ver todo',
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          color: tokens.brand,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Container(
                  decoration: BoxDecoration(
                    color: tokens.card,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: tokens.line),
                    boxShadow: [
                      BoxShadow(
                        color: AppColors.navy.withValues(alpha: 0.08),
                        blurRadius: 16,
                        offset: const Offset(0, 6),
                      ),
                    ],
                  ),
                  child: Column(
                    children: [
                      _Acceso(
                        icon: Icons.calendar_month_outlined,
                        iconBg: tokens.softInfo,
                        iconFg: tokens.infoFg,
                        titulo: 'Pedir permiso',
                        subtitulo: 'Por días, por horas o vacaciones',
                        onTap: () => _push(const PermisoNuevoScreen()),
                      ),
                      Divider(height: 1, indent: 68, color: tokens.line),
                      _Acceso(
                        icon: Icons.more_time_rounded,
                        iconBg: tokens.softInfo,
                        iconFg: tokens.brand,
                        titulo: 'Registrar horas extras',
                        subtitulo: 'Enviar a aprobación',
                        onTap: () => _push(const HoraExtraNuevaScreen()),
                      ),
                      Divider(height: 1, indent: 68, color: tokens.line),
                      _Acceso(
                        icon: Icons.verified_user_outlined,
                        iconBg: tokens.softOk,
                        iconFg: tokens.okFg,
                        titulo: 'Bandeja de aprobación',
                        subtitulo: _porAprobar > 0
                            ? '$_porAprobar solicitud(es) esperan su decisión'
                            : 'Pendientes y seguimiento',
                        onTap: () => _push(const BandejaScreen()),
                      ),
                      Divider(height: 1, indent: 68, color: tokens.line),
                      _Acceso(
                        icon: Icons.receipt_long_outlined,
                        iconBg: tokens.softDanger,
                        iconFg: tokens.dangerFg,
                        titulo: 'Mis boletas de pago',
                        subtitulo: 'Descargar últimos recibos',
                        onTap: () => showMensaje(
                          context,
                          'Las boletas de pago estarán disponibles pronto.',
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Índices de pestañas del shell (orden visual de la barra).
abstract final class ShellTabs {
  static const solicitudes = 0;
  static const marcar = 1;
  static const inicio = 2;
  static const avisos = 3;
  static const perfil = 4;
}

class _HeaderBar extends StatelessWidget {
  const _HeaderBar({required this.usuario, required this.onProfile});

  final Usuario usuario;
  final VoidCallback onProfile;

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    return Row(
      children: [
        Container(
          width: 38,
          height: 38,
          alignment: Alignment.center,
          decoration: const BoxDecoration(
            color: AppColors.navy,
            shape: BoxShape.circle,
          ),
          child: const Text(
            'A',
            style: TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w800,
              fontSize: 17,
            ),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Text.rich(
            TextSpan(
              children: [
                TextSpan(
                  text: 'ANDINA',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                    color: tokens.brand,
                  ),
                ),
                TextSpan(
                  text: ' RR. HH.',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: tokens.muted,
                  ),
                ),
              ],
            ),
          ),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: tokens.softOk,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.circle, size: 7, color: tokens.okFg),
              const SizedBox(width: 6),
              Text(
                'Sede Central',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: tokens.okFg,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 10),
        InkWell(
          onTap: onProfile,
          borderRadius: BorderRadius.circular(22),
          child: Stack(
            clipBehavior: Clip.none,
            children: [
              CircleAvatar(
                radius: 20,
                backgroundColor: tokens.line,
                child: Text(
                  usuario.initials,
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 13,
                    color: tokens.ink,
                  ),
                ),
              ),
              Positioned(
                right: -1,
                bottom: -1,
                child: Container(
                  width: 11,
                  height: 11,
                  decoration: BoxDecoration(
                    color: const Color(0xFF22C55E),
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: Theme.of(context).scaffoldBackgroundColor,
                      width: 2,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
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
      padding: const EdgeInsets.fromLTRB(18, 16, 18, 18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF15233D), Color(0xFF1E3358), Color(0xFF243B63)],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF15233D).withValues(alpha: 0.30),
            blurRadius: 24,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 7,
                height: 7,
                decoration: const BoxDecoration(
                  color: Color(0xFFFBBF24),
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Mi jornada de hoy',
                  style: TextStyle(
                    color: Color(0xFFCBD5E1),
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                  ),
                ),
              ),
              if (!loading) StatusChip(label: estado, tone: tono),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(child: _HoraBloque(label: 'Entrada', hora: entrada)),
              const SizedBox(width: 10),
              Expanded(child: _HoraBloque(label: 'Salida', hora: salida)),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.08),
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Row(
              children: [
                Icon(Icons.location_on, size: 15, color: Color(0xFF4ADE80)),
                SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Ubicación verificada por GPS',
                    style: TextStyle(
                      color: Color(0xFFCBD5E1),
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
                Text(
                  '08:30 – 18:00',
                  style: TextStyle(
                    color: Color(0xFF94A3B8),
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          if (puedeMarcar) ...[
            const SizedBox(height: 14),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                style: FilledButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: const Color(0xFF1D4ED8),
                  minimumSize: const Size.fromHeight(50),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                onPressed: onMarcar,
                icon: const Icon(Icons.fingerprint),
                label: Text(
                  entrada.contains('--') ? 'Marcar entrada' : 'Marcar salida',
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
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
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 12),
          ),
          const SizedBox(height: 4),
          Text(
            hora,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 26,
              fontWeight: FontWeight.w800,
              letterSpacing: 0.5,
              fontFeatures: [FontFeature.tabularFigures()],
            ),
          ),
        ],
      ),
    );
  }
}

class _Contador extends StatelessWidget {
  const _Contador({
    required this.icon,
    required this.iconBg,
    required this.iconFg,
    required this.valor,
    required this.titulo,
    required this.onTap,
  });

  final IconData icon;
  final Color iconBg;
  final Color iconFg;
  final int valor;
  final String titulo;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.cardOf(context),
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.lineOf(context)),
          ),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: iconBg,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, size: 20, color: iconFg),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '$valor',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w800,
                        height: 1.1,
                        color: AppColors.inkOf(context),
                      ),
                    ),
                    Text(
                      titulo,
                      style: TextStyle(
                        fontSize: 12,
                        color: AppColors.mutedOf(context),
                        fontWeight: FontWeight.w500,
                      ),
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

class _Acceso extends StatelessWidget {
  const _Acceso({
    required this.icon,
    required this.iconBg,
    required this.iconFg,
    required this.titulo,
    required this.subtitulo,
    required this.onTap,
  });

  final IconData icon;
  final Color iconBg;
  final Color iconFg;
  final String titulo;
  final String subtitulo;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        child: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: iconBg,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: iconFg, size: 22),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    titulo,
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      fontSize: 15,
                      color: AppColors.inkOf(context),
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitulo,
                    style: TextStyle(
                      fontSize: 12,
                      color: AppColors.mutedOf(context),
                    ),
                  ),
                ],
              ),
            ),
            Icon(Icons.chevron_right, color: AppColors.mutedOf(context)),
          ],
        ),
      ),
    );
  }
}
