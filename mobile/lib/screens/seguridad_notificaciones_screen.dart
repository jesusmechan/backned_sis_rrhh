import 'package:flutter/material.dart';

import '../services/marcacion_reminder_service.dart';
import '../theme/app_theme.dart';

/// Ajustes de recordatorios locales (solo notificaciones push de la app).
class SeguridadNotificacionesScreen extends StatefulWidget {
  const SeguridadNotificacionesScreen({super.key});

  @override
  State<SeguridadNotificacionesScreen> createState() =>
      _SeguridadNotificacionesScreenState();
}

class _SeguridadNotificacionesScreenState
    extends State<SeguridadNotificacionesScreen> {
  ReminderSettings? _settings;
  bool _loading = true;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      await MarcacionReminderService.instance.init();
      final s = await MarcacionReminderService.instance.loadSettings();
      if (!mounted) return;
      setState(() {
        _settings = s;
        _loading = false;
        _error = null;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.toString();
      });
    }
  }

  Future<void> _apply(ReminderSettings next) async {
    setState(() {
      _settings = next;
      _saving = true;
      _error = null;
    });
    try {
      final ok = await MarcacionReminderService.instance.requestPermissions();
      if (!ok && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Active el permiso de notificaciones para recibir los recordatorios.',
            ),
          ),
        );
      }
      await MarcacionReminderService.instance.saveSettings(next);
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          const SnackBar(
            content: Text('Recordatorios actualizados'),
          ),
        );
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _pickTime({required bool entrada}) async {
    final s = _settings;
    if (s == null) return;
    final initial = entrada ? s.entradaTime : s.salidaTime;
    final picked = await showTimePicker(
      context: context,
      initialTime: initial,
      helpText: entrada
          ? 'Hora del aviso de entrada'
          : 'Hora del aviso de salida',
      builder: (context, child) {
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
          child: child!,
        );
      },
    );
    if (picked == null) return;
    final next = entrada
        ? s.copyWith(
            entradaEnabled: true,
            entradaHour: picked.hour,
            entradaMinute: picked.minute,
          )
        : s.copyWith(
            salidaEnabled: true,
            salidaHour: picked.hour,
            salidaMinute: picked.minute,
          );
    await _apply(next);
  }

  String _hhmm(TimeOfDay t) =>
      '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    final s = _settings;

    return Scaffold(
      appBar: AppBar(title: const Text('Seguridad & notificaciones')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Text(
                  'RECORDATORIOS DE MARCACIÓN',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 1.1,
                    color: tokens.muted,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Solo notificaciones locales de la app. No realizan la marcación automática.',
                  style: TextStyle(fontSize: 13, color: tokens.muted, height: 1.35),
                ),
                const SizedBox(height: 14),
                if (_error != null) ...[
                  Text(_error!, style: const TextStyle(color: AppColors.danger)),
                  const SizedBox(height: 10),
                ],
                Card(
                  child: Column(
                    children: [
                      SwitchListTile(
                        value: s?.entradaEnabled ?? false,
                        onChanged: s == null || _saving
                            ? null
                            : (v) => _apply(s.copyWith(entradaEnabled: v)),
                        secondary: Container(
                          width: 42,
                          height: 42,
                          decoration: BoxDecoration(
                            color: tokens.softInfo,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Icon(
                            Icons.login_rounded,
                            color: tokens.infoFg,
                          ),
                        ),
                        title: Text(
                          'Aviso de entrada',
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            color: tokens.ink,
                          ),
                        ),
                        subtitle: Text(
                          'Recuerde marcar su ingreso',
                          style: TextStyle(color: tokens.muted, fontSize: 12),
                        ),
                      ),
                      if (s?.entradaEnabled == true) ...[
                        const Divider(height: 1),
                        ListTile(
                          leading: Icon(Icons.alarm_rounded, color: tokens.iconTint),
                          title: Text(
                            'Hora del aviso',
                            style: TextStyle(color: tokens.ink),
                          ),
                          trailing: Text(
                            _hhmm(s!.entradaTime),
                            style: TextStyle(
                              fontWeight: FontWeight.w800,
                              fontSize: 16,
                              color: tokens.brand,
                            ),
                          ),
                          onTap: _saving ? null : () => _pickTime(entrada: true),
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                Card(
                  child: Column(
                    children: [
                      SwitchListTile(
                        value: s?.salidaEnabled ?? false,
                        onChanged: s == null || _saving
                            ? null
                            : (v) => _apply(s.copyWith(salidaEnabled: v)),
                        secondary: Container(
                          width: 42,
                          height: 42,
                          decoration: BoxDecoration(
                            color: const Color(0xFFFFF1E6),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(
                            Icons.logout_rounded,
                            color: Color(0xFFEA580C),
                          ),
                        ),
                        title: Text(
                          'Aviso de salida',
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            color: tokens.ink,
                          ),
                        ),
                        subtitle: Text(
                          'Recuerde marcar su salida',
                          style: TextStyle(color: tokens.muted, fontSize: 12),
                        ),
                      ),
                      if (s?.salidaEnabled == true) ...[
                        const Divider(height: 1),
                        ListTile(
                          leading: Icon(Icons.alarm_rounded, color: tokens.iconTint),
                          title: Text(
                            'Hora del aviso',
                            style: TextStyle(color: tokens.ink),
                          ),
                          trailing: Text(
                            _hhmm(s!.salidaTime),
                            style: TextStyle(
                              fontWeight: FontWeight.w800,
                              fontSize: 16,
                              color: tokens.brand,
                            ),
                          ),
                          onTap: _saving ? null : () => _pickTime(entrada: false),
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: tokens.softInfo,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: tokens.line),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(Icons.info_outline_rounded, color: tokens.infoFg),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          'El aviso se envía todos los días a la hora configurada '
                          '(hora del dispositivo). Sugerencia: 08:15 para entrada '
                          'y 17:45 para salida, unos minutos antes del turno.',
                          style: TextStyle(
                            fontSize: 13,
                            height: 1.4,
                            color: tokens.ink,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                if (_saving) ...[
                  const SizedBox(height: 16),
                  const Center(
                    child: SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(strokeWidth: 2.4),
                    ),
                  ),
                ],
              ],
            ),
    );
  }
}
