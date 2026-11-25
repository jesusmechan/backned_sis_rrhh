import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/api_config.dart';
import '../providers/auth_provider.dart';
import '../services/api_client.dart';
import '../theme/app_theme.dart';

enum _ServerDialogAction { cancel, autoDetect, save }

/// Diálogo para configurar la URL del backend (nube HTTPS o IP local).
Future<bool> showServerConfigDialog(BuildContext context) async {
  final api = context.read<AuthProvider>().api;
  await api.ensureReady();
  if (!context.mounted) return false;
  final manual = await ApiConfig.getManualBaseUrl();
  if (!context.mounted) return false;

  final action = await showDialog<_ServerDialogAction>(
    context: context,
    builder: (ctx) => _ServerConfigDialog(
      initialUrl: manual ?? api.baseUrlLabel,
    ),
  );

  if (action == null || action == _ServerDialogAction.cancel || !context.mounted) {
    return false;
  }

  if (action == _ServerDialogAction.autoDetect) {
    await ApiConfig.clearManualBaseUrl();
    api.resetBaseUrl();
    await api.ensureReady();
  } else {
    // La URL ya se guardó en el diálogo: aplicarla sin que el sondeo la pise.
    await api.applyConfiguredUrl();
  }
  return true;
}

class _ServerConfigDialog extends StatefulWidget {
  const _ServerConfigDialog({required this.initialUrl});

  final String initialUrl;

  @override
  State<_ServerConfigDialog> createState() => _ServerConfigDialogState();
}

class _ServerConfigDialogState extends State<_ServerConfigDialog> {
  late final TextEditingController _ctrl;

  @override
  void initState() {
    super.initState();
    _ctrl = TextEditingController(text: widget.initialUrl);
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    try {
      ApiConfig.parseUserUrl(_ctrl.text);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('FormatException: ', ''))),
      );
      return;
    }
    await ApiConfig.saveCustomBaseUrl(_ctrl.text);
    if (!mounted) return;
    Navigator.pop(context, _ServerDialogAction.save);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Servidor API'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'URL del backend (sin / final). Nube: '
              'https://backned-sis-rrhh-api.onrender.com '
              '(Render Free puede tardar ~1 min en despertar). '
              'Local: IP de tu PC o http://127.0.0.1:8080 con USB + adb reverse.',
              style: TextStyle(fontSize: 13, color: AppColors.mutedOf(context)),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _ctrl,
              autocorrect: false,
              keyboardType: TextInputType.url,
              decoration: const InputDecoration(
                labelText: 'URL del servidor',
                hintText: 'https://backned-sis-rrhh-api.onrender.com',
                helperText: 'Nube: …onrender.com — Local: 192.168.0.25:8080',
              ),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, _ServerDialogAction.cancel),
          child: const Text('Cancelar'),
        ),
        TextButton(
          onPressed: () => Navigator.pop(context, _ServerDialogAction.autoDetect),
          child: const Text('Detectar auto'),
        ),
        FilledButton(
          onPressed: _save,
          child: const Text('Guardar'),
        ),
      ],
    );
  }
}

Future<String> probeServerMessage(ApiClient api) async {
  try {
    await api.verifyConfiguredConnection();
    return 'Conexión correcta (${api.baseUrlLabel}).';
  } on ApiException catch (e) {
    return e.message;
  } on TimeoutException {
    return 'El servidor no respondió a tiempo (${api.baseUrlLabel}). '
        'En Render Free espera ~1 min y vuelve a probar.';
  } catch (e) {
    return e.toString();
  }
}
