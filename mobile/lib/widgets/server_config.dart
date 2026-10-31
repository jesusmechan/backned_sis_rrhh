import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/api_config.dart';
import '../providers/auth_provider.dart';
import '../services/api_client.dart';
import '../theme/app_theme.dart';

enum _ServerDialogAction { cancel, autoDetect, save }

/// Diálogo para configurar la URL del backend (cualquier IP/host en la LAN).
Future<bool> showServerConfigDialog(BuildContext context) async {
  final api = context.read<AuthProvider>().api;
  await api.ensureReady();
  if (!context.mounted) return false;
  final manual = await ApiConfig.getManualBaseUrl();
  final ctrl = TextEditingController(text: manual ?? api.baseUrlLabel);

  final action = await showDialog<_ServerDialogAction>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: const Text('Servidor API'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Dirección del backend (puerto 8080). Usa la IP de tu PC en la red actual '
              '(ej. http://192.168.0.15:8080) o http://127.0.0.1:8080 con USB + adb reverse.',
              style: TextStyle(fontSize: 13, color: AppColors.muted),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: ctrl,
              autocorrect: false,
              keyboardType: TextInputType.url,
              decoration: const InputDecoration(
                labelText: 'URL del servidor',
                hintText: 'http://192.168.0.25:8080',
                helperText: 'También: 192.168.0.25 o 192.168.0.25:8080',
              ),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(ctx, _ServerDialogAction.cancel),
          child: const Text('Cancelar'),
        ),
        TextButton(
          onPressed: () => Navigator.pop(ctx, _ServerDialogAction.autoDetect),
          child: const Text('Detectar auto'),
        ),
        FilledButton(
          onPressed: () {
            try {
              ApiConfig.parseUserUrl(ctrl.text);
            } catch (e) {
              ScaffoldMessenger.of(ctx).showSnackBar(
                SnackBar(content: Text(e.toString().replaceFirst('FormatException: ', ''))),
              );
              return;
            }
            Navigator.pop(ctx, _ServerDialogAction.save);
          },
          child: const Text('Guardar'),
        ),
      ],
    ),
  );

  final text = ctrl.text;
  ctrl.dispose();

  if (action == null || action == _ServerDialogAction.cancel || !context.mounted) {
    return false;
  }

  if (action == _ServerDialogAction.autoDetect) {
    await ApiConfig.clearManualBaseUrl();
  } else {
    await ApiConfig.saveCustomBaseUrl(text);
  }

  api.resetBaseUrl();
  await api.ensureReady();
  return true;
}

Future<String> probeServerMessage(ApiClient api) async {
  try {
    await api.ping();
    return 'Conexión correcta (${api.baseUrlLabel}).';
  } on ApiException catch (e) {
    return e.message;
  } catch (e) {
    return e.toString();
  }
}
