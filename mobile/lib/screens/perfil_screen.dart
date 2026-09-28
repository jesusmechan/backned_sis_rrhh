import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/api_config.dart';
import '../models/solicitud.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';

class PerfilScreen extends StatelessWidget {
  const PerfilScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final u = context.watch<AuthProvider>().usuario!;
    return Scaffold(
      appBar: AppBar(title: const Text('Mi perfil')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  CircleAvatar(
                    radius: 36,
                    backgroundColor: AppColors.navy,
                    foregroundColor: Colors.white,
                    child: Text(
                      u.initials,
                      style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    u.displayName,
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
                  ),
                  Text('@${u.nombreUsuario}',
                      style: const TextStyle(color: AppColors.muted)),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.badge_outlined, color: AppColors.info),
                  title: const Text('Rol',
                      style: TextStyle(fontSize: 12, color: AppColors.muted)),
                  subtitle: Text(u.rol,
                      style: const TextStyle(fontWeight: FontWeight.w600)),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.mail_outline, color: AppColors.info),
                  title: const Text('Correo',
                      style: TextStyle(fontSize: 12, color: AppColors.muted)),
                  subtitle: Text(u.correo,
                      style: const TextStyle(fontWeight: FontWeight.w600)),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.fingerprint, color: AppColors.info),
                  title: const Text('ID empleado',
                      style: TextStyle(fontSize: 12, color: AppColors.muted)),
                  subtitle: Text(
                    u.idEmpleado?.toString() ?? 'No asociado',
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                ),
              ],
            ),
          ),
          if (u.idEmpleado != null) ...[
            const SizedBox(height: 12),
            const _SaldoVacacionesCard(),
          ],
          const SizedBox(height: 20),
          OutlinedButton.icon(
            onPressed: () async {
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
            },
            icon: const Icon(Icons.logout, color: AppColors.danger),
            label: const Text('Cerrar sesión',
                style: TextStyle(color: AppColors.danger)),
          ),
          if (kDebugMode) ...[
            const SizedBox(height: 16),
            Text(
              'API: ${ApiConfig.baseUrl}',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 11, color: AppColors.muted),
            ),
          ],
        ],
      ),
    );
  }
}

class _SaldoVacacionesCard extends StatefulWidget {
  const _SaldoVacacionesCard();

  @override
  State<_SaldoVacacionesCard> createState() => _SaldoVacacionesCardState();
}

class _SaldoVacacionesCardState extends State<_SaldoVacacionesCard> {
  late Future<SaldoVacaciones> _future;

  @override
  void initState() {
    super.initState();
    _future = context.read<AuthProvider>().saldoVacaciones();
  }

  String _d(double v) => v.toStringAsFixed(v % 1 == 0 ? 0 : 1);

  @override
  Widget build(BuildContext context) {
    return Card(
      child: FutureBuilder<SaldoVacaciones>(
        future: _future,
        builder: (context, snap) {
          final s = snap.data;
          return ListTile(
            leading: const Icon(Icons.beach_access_outlined, color: AppColors.info),
            title: const Text('Vacaciones disponibles',
                style: TextStyle(fontSize: 12, color: AppColors.muted)),
            subtitle: Text(
              snap.hasError
                  ? 'No disponible'
                  : s == null
                      ? 'Cargando…'
                      : '${_d(s.diasDisponibles)} días  ·  ganados ${_d(s.diasGanados)}, usados ${_d(s.diasUsados)}',
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          );
        },
      ),
    );
  }
}
