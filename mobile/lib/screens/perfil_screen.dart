import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../models/solicitud.dart';
import '../models/usuario.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'configuracion_screen.dart';
import 'seguridad_notificaciones_screen.dart';

class PerfilScreen extends StatelessWidget {
  const PerfilScreen({super.key});

  bool _esAdmin(Usuario u) {
    final r = '${u.rol} ${u.perfil}'.toUpperCase();
    return r.contains('ADMIN') || r.contains('RRHH') || r.contains('GEREN');
  }

  String _rolCorto(Usuario u) {
    final r = u.rol.trim();
    if (r.isEmpty) return u.perfil;
    return r.length > 18 ? r.substring(0, 18) : r;
  }

  String _fmtId(int? id) {
    if (id == null) return '—';
    return '#${id.toString().padLeft(4, '0')}';
  }

  Future<void> _copiarCorreo(BuildContext context, String correo) async {
    await Clipboard.setData(ClipboardData(text: correo));
    if (!context.mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        const SnackBar(content: Text('Correo copiado al portapapeles')),
      );
  }

  Future<void> _cerrarSesion(BuildContext context) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cerrar sesión'),
        content: const Text('¿Desea salir de la aplicación?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Salir'),
          ),
        ],
      ),
    );
    if (ok == true && context.mounted) {
      await context.read<AuthProvider>().logout();
    }
  }

  @override
  Widget build(BuildContext context) {
    final u = context.watch<AuthProvider>().usuario!;
    final tokens = AppColors.tokens(context);
    final admin = _esAdmin(u);
    final year = DateTime.now().year;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(title: const Text('Mi perfil')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 28),
        children: [
          _HeaderCard(usuario: u, admin: admin),
          const SizedBox(height: 22),
          _SectionLabel('Información del colaborador'),
          const SizedBox(height: 10),
          _InfoCard(
            children: [
              _InfoRow(
                icon: Icons.badge_outlined,
                iconBg: const Color(0xFFF3E8FF),
                iconFg: const Color(0xFF7C3AED),
                label: 'Rol asignado',
                value: _rolCorto(u).toUpperCase(),
                trailing: admin
                    ? _Pill(
                        label: 'Nivel máximo',
                        bg: tokens.softNeutral,
                        fg: tokens.muted,
                      )
                    : null,
              ),
              Divider(height: 1, color: tokens.line),
              _InfoRow(
                icon: Icons.mail_outline_rounded,
                iconBg: const Color(0xFFE8F1FF),
                iconFg: const Color(0xFF2563EB),
                label: 'Correo corporativo',
                value: u.correo,
                trailing: IconButton(
                  tooltip: 'Copiar',
                  onPressed: () => _copiarCorreo(context, u.correo),
                  icon: Icon(Icons.copy_rounded, size: 18, color: tokens.muted),
                  visualDensity: VisualDensity.compact,
                ),
              ),
              Divider(height: 1, color: tokens.line),
              _InfoRow(
                icon: Icons.fingerprint_rounded,
                iconBg: const Color(0xFFDCFCE7),
                iconFg: const Color(0xFF16A34A),
                label: 'ID de empleado',
                value: _fmtId(u.idEmpleado),
                trailing: u.idEmpleado != null
                    ? _Pill(
                        label: 'Biometría activa',
                        bg: tokens.softOk,
                        fg: tokens.okFg,
                        icon: Icons.check_circle,
                      )
                    : _Pill(
                        label: 'Sin asociar',
                        bg: tokens.softNeutral,
                        fg: tokens.muted,
                      ),
              ),
            ],
          ),
          const SizedBox(height: 22),
          _SectionLabel('Gestión y balance'),
          const SizedBox(height: 10),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(child: _VacacionesBalanceCard(year: year)),
              const SizedBox(width: 12),
              const Expanded(child: _HorarioCard()),
            ],
          ),
          const SizedBox(height: 22),
          _SectionLabel('Ajustes & sistema'),
          const SizedBox(height: 10),
          _InfoCard(
            children: [
              _NavRow(
                icon: Icons.settings_rounded,
                iconBg: AppColors.navy,
                iconFg: Colors.white,
                title: 'Configuración',
                subtitle: 'Tema y servidor API',
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => const ConfiguracionScreen(),
                    ),
                  );
                },
              ),
              Divider(height: 1, indent: 68, color: tokens.line),
              _NavRow(
                icon: Icons.shield_outlined,
                iconBg: const Color(0xFFE8F1FF),
                iconFg: const Color(0xFF2563EB),
                title: 'Seguridad & notificaciones',
                subtitle: 'Recordatorios de marcación',
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => const SeguridadNotificacionesScreen(),
                    ),
                  );
                },
              ),
            ],
          ),
          const SizedBox(height: 22),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.danger,
                backgroundColor: AppColors.dangerSoft.withValues(alpha: 0.55),
                side: const BorderSide(color: Color(0xFFFECACA)),
                minimumSize: const Size.fromHeight(52),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              onPressed: () => _cerrarSesion(context),
              icon: const Icon(Icons.logout_rounded),
              label: const Text(
                'Cerrar sesión',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            'Andina RR. HH. · Versión 1.0.0 (Build 1)',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12, color: tokens.muted),
          ),
        ],
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text.toUpperCase(),
      style: TextStyle(
        fontSize: 11,
        fontWeight: FontWeight.w800,
        letterSpacing: 1.1,
        color: AppColors.mutedOf(context),
      ),
    );
  }
}

class _HeaderCard extends StatelessWidget {
  const _HeaderCard({required this.usuario, required this.admin});

  final Usuario usuario;
  final bool admin;

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(20, 22, 20, 20),
      decoration: BoxDecoration(
        color: tokens.card,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: tokens.line),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: 0.06),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        children: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              CircleAvatar(
                radius: 42,
                backgroundColor: AppColors.navy,
                foregroundColor: Colors.white,
                child: Text(
                  usuario.initials,
                  style: const TextStyle(
                    fontSize: 26,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              Positioned(
                right: 2,
                bottom: 2,
                child: Container(
                  width: 16,
                  height: 16,
                  decoration: BoxDecoration(
                    color: const Color(0xFF22C55E),
                    shape: BoxShape.circle,
                    border: Border.all(color: tokens.card, width: 2.5),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Flexible(
                child: Text(
                  usuario.displayName,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                    color: tokens.ink,
                  ),
                ),
              ),
              const SizedBox(width: 6),
              const Icon(
                Icons.verified_rounded,
                size: 18,
                color: Color(0xFF2563EB),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            '@${usuario.nombreUsuario}',
            style: TextStyle(color: tokens.muted, fontWeight: FontWeight.w500),
          ),
          const SizedBox(height: 14),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            alignment: WrapAlignment.center,
            children: [
              if (admin)
                const _Pill(
                  label: 'Administrador',
                  bg: AppColors.navy,
                  fg: Colors.white,
                  icon: Icons.star_rounded,
                  iconColor: Color(0xFFFBBF24),
                ),
              _Pill(
                label: usuario.idEmpleado != null
                    ? 'Personal fijo'
                    : 'Usuario sistema',
                bg: tokens.softOk,
                fg: tokens.okFg,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    return Container(
      decoration: BoxDecoration(
        color: tokens.card,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: tokens.line),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(children: children),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.iconBg,
    required this.iconFg,
    required this.label,
    required this.value,
    this.trailing,
  });

  final IconData icon;
  final Color iconBg;
  final Color iconFg;
  final String label;
  final String value;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    return Padding(
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
                  label,
                  style: TextStyle(fontSize: 12, color: tokens.muted),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 14,
                    color: tokens.ink,
                  ),
                ),
              ],
            ),
          ),
          ?trailing,
        ],
      ),
    );
  }
}

class _NavRow extends StatelessWidget {
  const _NavRow({
    required this.icon,
    required this.iconBg,
    required this.iconFg,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final Color iconBg;
  final Color iconFg;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
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
                    title,
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      fontSize: 15,
                      color: tokens.ink,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: TextStyle(fontSize: 12, color: tokens.muted),
                  ),
                ],
              ),
            ),
            Icon(Icons.chevron_right_rounded, color: tokens.muted),
          ],
        ),
      ),
    );
  }
}

class _Pill extends StatelessWidget {
  const _Pill({
    required this.label,
    required this.bg,
    required this.fg,
    this.icon,
    this.iconColor,
  });

  final String label;
  final Color bg;
  final Color fg;
  final IconData? icon;
  final Color? iconColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 13, color: iconColor ?? fg),
            const SizedBox(width: 4),
          ],
          Text(
            label,
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: fg,
            ),
          ),
        ],
      ),
    );
  }
}

class _VacacionesBalanceCard extends StatefulWidget {
  const _VacacionesBalanceCard({required this.year});

  final int year;

  @override
  State<_VacacionesBalanceCard> createState() => _VacacionesBalanceCardState();
}

class _VacacionesBalanceCardState extends State<_VacacionesBalanceCard> {
  late Future<SaldoVacaciones> _future;

  @override
  void initState() {
    super.initState();
    _future = context.read<AuthProvider>().saldoVacaciones();
  }

  String _d(double v) => v.toStringAsFixed(v % 1 == 0 ? 0 : 1);

  void _mostrarInfo(BuildContext context, SaldoVacaciones? s) {
    final detalle = s == null
        ? 'El saldo se calcula según los días acumulados por su antigüedad '
            'menos los ya gozados o programados.'
        : 'Dispone de ${_d(s.diasDisponibles)} día(s) por disfrutar. '
            'Acumulados: ${_d(s.diasGanados)} · Utilizados: ${_d(s.diasUsados)}. '
            'Este saldo se actualiza al aprobar o registrar vacaciones.';

    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      backgroundColor: AppColors.cardOf(context),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(18)),
      ),
      builder: (ctx) {
        final t = AppColors.tokens(ctx);
        return Padding(
          padding: const EdgeInsets.fromLTRB(20, 4, 20, 28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.info_outline_rounded, color: t.iconTint),
                  const SizedBox(width: 10),
                  Text(
                    'Saldo de vacaciones',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w800,
                      color: t.ink,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                detalle,
                style: TextStyle(fontSize: 14, height: 1.45, color: t.muted),
              ),
              const SizedBox(height: 8),
              Text(
                'Para solicitar días, use «Pedir permiso» e indique el tipo Vacaciones.',
                style: TextStyle(fontSize: 13, height: 1.4, color: t.ink),
              ),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    return FutureBuilder<SaldoVacaciones>(
      future: _future,
      builder: (context, snap) {
        final s = snap.data;
        final label = snap.hasError
            ? '—'
            : s == null
                ? '…'
                : _d(s.diasDisponibles);
        return Material(
          color: tokens.card,
          borderRadius: BorderRadius.circular(18),
          child: InkWell(
            onTap: () => _mostrarInfo(context, s),
            borderRadius: BorderRadius.circular(18),
            child: Container(
              padding: const EdgeInsets.fromLTRB(14, 14, 14, 16),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: tokens.line),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 34,
                        height: 34,
                        decoration: BoxDecoration(
                          color: tokens.softInfo,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Icon(
                          Icons.cloud_outlined,
                          size: 18,
                          color: tokens.infoFg,
                        ),
                      ),
                      const Spacer(),
                      _Pill(
                        label: 'Al día',
                        bg: const Color(0xFFFFF7ED),
                        fg: const Color(0xFFC2410C),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    label,
                    style: TextStyle(
                      fontSize: 34,
                      fontWeight: FontWeight.w800,
                      height: 1,
                      color: tokens.ink,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Días vacaciones',
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      fontSize: 13,
                      color: tokens.ink,
                    ),
                  ),
                  Text(
                    'Disponibles ${widget.year}',
                    style: TextStyle(fontSize: 12, color: tokens.muted),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _HorarioCard extends StatelessWidget {
  const _HorarioCard();

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 16),
      decoration: BoxDecoration(
        color: tokens.card,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: tokens.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 34,
                height: 34,
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF1E6),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.schedule_rounded,
                  size: 18,
                  color: Color(0xFFEA580C),
                ),
              ),
              const Spacer(),
              _Pill(
                label: 'Turno',
                bg: tokens.softInfo,
                fg: tokens.infoFg,
              ),
            ],
          ),
          const SizedBox(height: 16),
          Text(
            '08:30–18:00',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              height: 1.1,
              color: tokens.ink,
            ),
          ),
          const SizedBox(height: 10),
          Text(
            'Horario regular',
            style: TextStyle(
              fontWeight: FontWeight.w700,
              fontSize: 13,
              color: tokens.ink,
            ),
          ),
          Text(
            'Lunes a viernes',
            style: TextStyle(fontSize: 12, color: tokens.muted),
          ),
        ],
      ),
    );
  }
}
