import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../providers/theme_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/server_config.dart';
import '../widgets/theme_switch.dart';

/// Pantalla aparte: tema + servidor API.
class ConfiguracionScreen extends StatelessWidget {
  const ConfiguracionScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    final isDark = context.select<ThemeProvider, bool>((t) => t.isDark);
    final apiLabel = context.watch<AuthProvider>().api.baseUrlLabel;

    return Scaffold(
      appBar: AppBar(title: const Text('Configuración')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 12, 14),
              child: Row(
                children: [
                  Icon(
                    isDark ? Icons.dark_mode_outlined : Icons.light_mode_outlined,
                    color: tokens.iconTint,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Tema de la app',
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            color: tokens.ink,
                          ),
                        ),
                        Text(
                          isDark ? 'Modo oscuro' : 'Modo claro',
                          style: TextStyle(fontSize: 12, color: tokens.muted),
                        ),
                      ],
                    ),
                  ),
                  const ThemeSunMoonSwitch(),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: ListTile(
              leading: Icon(Icons.dns_outlined, color: tokens.iconTint),
              title: Text(
                'Servidor API',
                style: TextStyle(
                  fontWeight: FontWeight.w700,
                  color: tokens.ink,
                ),
              ),
              subtitle: Text(
                apiLabel,
                style: TextStyle(fontSize: 12, color: tokens.muted),
              ),
              trailing: Icon(Icons.chevron_right, color: tokens.muted),
              onTap: () async {
                final ok = await showServerConfigDialog(context);
                if (ok && context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(
                        'Servidor: ${context.read<AuthProvider>().api.baseUrlLabel}',
                      ),
                    ),
                  );
                }
              },
            ),
          ),
        ],
      ),
    );
  }
}
