import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart' show TimeOfDay;
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timezone/data/latest_all.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

/// Recordatorios locales (solo notificación) para marcar entrada/salida.
class MarcacionReminderService {
  MarcacionReminderService._();
  static final MarcacionReminderService instance = MarcacionReminderService._();

  static const _channelId = 'marcacion_reminders';
  static const _channelName = 'Recordatorios de marcación';

  static const _kEntradaOn = 'andina.reminder.entrada.on';
  static const _kEntradaH = 'andina.reminder.entrada.h';
  static const _kEntradaM = 'andina.reminder.entrada.m';
  static const _kSalidaOn = 'andina.reminder.salida.on';
  static const _kSalidaH = 'andina.reminder.salida.h';
  static const _kSalidaM = 'andina.reminder.salida.m';

  static const idEntrada = 1101;
  static const idSalida = 1102;

  final FlutterLocalNotificationsPlugin _plugin =
      FlutterLocalNotificationsPlugin();

  bool _ready = false;

  bool get ready => _ready;

  Future<void> init() async {
    if (_ready) return;

    tzdata.initializeTimeZones();
    try {
      final info = await FlutterTimezone.getLocalTimezone();
      tz.setLocalLocation(tz.getLocation(info.identifier));
    } catch (_) {
      // Fallback Lima (negocio Andina).
      try {
        tz.setLocalLocation(tz.getLocation('America/Lima'));
      } catch (_) {}
    }

    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosInit = DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );
    await _plugin.initialize(
      settings: const InitializationSettings(android: androidInit, iOS: iosInit),
    );

    final android = _plugin.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    await android?.createNotificationChannel(
      const AndroidNotificationChannel(
        _channelId,
        _channelName,
        description: 'Avisos locales para recordar marcar asistencia.',
        importance: Importance.high,
      ),
    );

    _ready = true;
    await rescheduleFromPrefs();
  }

  Future<bool> requestPermissions() async {
    if (kIsWeb) return false;
    if (Platform.isAndroid) {
      final android = _plugin.resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin>();
      final notif = await android?.requestNotificationsPermission() ?? true;
      await android?.requestExactAlarmsPermission();
      return notif;
    }
    if (Platform.isIOS) {
      final ios = _plugin.resolvePlatformSpecificImplementation<
          IOSFlutterLocalNotificationsPlugin>();
      return await ios?.requestPermissions(
            alert: true,
            badge: true,
            sound: true,
          ) ??
          false;
    }
    return true;
  }

  Future<ReminderSettings> loadSettings() async {
    final p = await SharedPreferences.getInstance();
    return ReminderSettings(
      entradaEnabled: p.getBool(_kEntradaOn) ?? false,
      entradaHour: p.getInt(_kEntradaH) ?? 8,
      entradaMinute: p.getInt(_kEntradaM) ?? 15,
      salidaEnabled: p.getBool(_kSalidaOn) ?? false,
      salidaHour: p.getInt(_kSalidaH) ?? 17,
      salidaMinute: p.getInt(_kSalidaM) ?? 45,
    );
  }

  Future<void> saveSettings(ReminderSettings s) async {
    final p = await SharedPreferences.getInstance();
    await p.setBool(_kEntradaOn, s.entradaEnabled);
    await p.setInt(_kEntradaH, s.entradaHour);
    await p.setInt(_kEntradaM, s.entradaMinute);
    await p.setBool(_kSalidaOn, s.salidaEnabled);
    await p.setInt(_kSalidaH, s.salidaHour);
    await p.setInt(_kSalidaM, s.salidaMinute);
    await reschedule(s);
  }

  Future<void> rescheduleFromPrefs() async {
    final s = await loadSettings();
    await reschedule(s);
  }

  Future<void> reschedule(ReminderSettings s) async {
    if (!_ready) await init();
    await _plugin.cancel(id: idEntrada);
    await _plugin.cancel(id: idSalida);

    if (s.entradaEnabled) {
      await _scheduleDaily(
        id: idEntrada,
        hour: s.entradaHour,
        minute: s.entradaMinute,
        title: 'Recordatorio de entrada',
        body:
            'Se acerca la hora de marcar su ingreso. Recuerde registrar su asistencia.',
      );
    }
    if (s.salidaEnabled) {
      await _scheduleDaily(
        id: idSalida,
        hour: s.salidaHour,
        minute: s.salidaMinute,
        title: 'Recordatorio de salida',
        body:
            'Se acerca la hora de marcar su salida. No olvide cerrar su jornada.',
      );
    }
  }

  Future<void> _scheduleDaily({
    required int id,
    required int hour,
    required int minute,
    required String title,
    required String body,
  }) async {
    final when = _nextInstance(hour, minute);
    final details = NotificationDetails(
      android: AndroidNotificationDetails(
        _channelId,
        _channelName,
        channelDescription: 'Avisos locales para recordar marcar asistencia.',
        importance: Importance.high,
        priority: Priority.high,
        icon: '@mipmap/ic_launcher',
        category: AndroidNotificationCategory.reminder,
        styleInformation: BigTextStyleInformation(body),
      ),
      iOS: const DarwinNotificationDetails(
        presentAlert: true,
        presentBadge: true,
        presentSound: true,
      ),
    );

    try {
      await _plugin.zonedSchedule(
        id: id,
        title: title,
        body: body,
        scheduledDate: when,
        notificationDetails: details,
        androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
        matchDateTimeComponents: DateTimeComponents.time,
      );
    } catch (_) {
      // Xiaomi / sin permiso de alarma exacta: modo inexacto.
      await _plugin.zonedSchedule(
        id: id,
        title: title,
        body: body,
        scheduledDate: when,
        notificationDetails: details,
        androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
        matchDateTimeComponents: DateTimeComponents.time,
      );
    }
  }

  tz.TZDateTime _nextInstance(int hour, int minute) {
    final now = tz.TZDateTime.now(tz.local);
    var scheduled = tz.TZDateTime(
      tz.local,
      now.year,
      now.month,
      now.day,
      hour,
      minute,
    );
    if (!scheduled.isAfter(now)) {
      scheduled = scheduled.add(const Duration(days: 1));
    }
    return scheduled;
  }
}

class ReminderSettings {
  const ReminderSettings({
    required this.entradaEnabled,
    required this.entradaHour,
    required this.entradaMinute,
    required this.salidaEnabled,
    required this.salidaHour,
    required this.salidaMinute,
  });

  final bool entradaEnabled;
  final int entradaHour;
  final int entradaMinute;
  final bool salidaEnabled;
  final int salidaHour;
  final int salidaMinute;

  TimeOfDay get entradaTime =>
      TimeOfDay(hour: entradaHour, minute: entradaMinute);
  TimeOfDay get salidaTime => TimeOfDay(hour: salidaHour, minute: salidaMinute);

  ReminderSettings copyWith({
    bool? entradaEnabled,
    int? entradaHour,
    int? entradaMinute,
    bool? salidaEnabled,
    int? salidaHour,
    int? salidaMinute,
  }) {
    return ReminderSettings(
      entradaEnabled: entradaEnabled ?? this.entradaEnabled,
      entradaHour: entradaHour ?? this.entradaHour,
      entradaMinute: entradaMinute ?? this.entradaMinute,
      salidaEnabled: salidaEnabled ?? this.salidaEnabled,
      salidaHour: salidaHour ?? this.salidaHour,
      salidaMinute: salidaMinute ?? this.salidaMinute,
    );
  }
}
