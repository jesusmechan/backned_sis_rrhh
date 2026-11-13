import 'dart:io' show Platform;

import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

/// URL base del API Spring Boot (puerto 8080).
///
/// Prioridad al conectar: URL manual guardada → última URL que funcionó →
/// `--dart-define=API_URL` → sondeo (127.0.0.1 USB, 10.0.2.2 emulador, etc.).
class ApiConfig {
  ApiConfig._();

  static const String _override = String.fromEnvironment('API_URL');
  static const String _prefsManualKey = 'andina.api.baseUrl';
  static const String _prefsLastOkKey = 'andina.api.lastWorking';

  static const Duration timeout = Duration(seconds: 20);
  static const Duration probeTimeout = Duration(seconds: 4);

  static const String defaultHost = 'http://127.0.0.1:8080';

  static String? _resolved;

  static void resetResolved() => _resolved = null;

  static String get baseUrl => _resolved ?? defaultHost;

  /// Normaliza entrada del usuario: acepta IP LAN, host o URL completa.
  ///
  /// - `https://host` → sin puerto (443).
  /// - `http://host` sin puerto → `:8080` (dev local).
  /// - Con puerto explícito se respeta.
  static String parseUserUrl(String input) {
    var raw = input.trim();
    if (raw.isEmpty) {
      throw FormatException('Indique la dirección del servidor.');
    }
    if (!raw.startsWith('http://') && !raw.startsWith('https://')) {
      raw = 'http://$raw';
    }
    final uri = Uri.tryParse(raw);
    if (uri == null || uri.host.isEmpty) {
      throw FormatException(
        'URL no válida. Ejemplo: https://tu-api.onrender.com o http://192.168.0.15:8080',
      );
    }
    if (uri.hasPort) {
      return _normalize('${uri.scheme}://${uri.host}:${uri.port}');
    }
    if (uri.scheme == 'https') {
      return _normalize('https://${uri.host}');
    }
    return _normalize('http://${uri.host}:8080');
  }

  static Future<String> resolveBaseUrl() async {
    if (_resolved != null) return _resolved!;
    _resolved = await redetectBaseUrl();
    return _resolved!;
  }

  /// Busca un servidor accesible sin quedarse en una IP fija.
  static Future<String> redetectBaseUrl() async {
    final prefs = await SharedPreferences.getInstance();
    final manual = prefs.getString(_prefsManualKey);
    final lastOk = prefs.getString(_prefsLastOkKey);

    final candidates = <String>[];
    void add(String? url) {
      if (url == null || url.trim().isEmpty) return;
      try {
        final n = url.startsWith('http') ? _normalize(url) : parseUserUrl(url);
        if (!candidates.contains(n)) candidates.add(n);
      } catch (_) {}
    }

    // --dart-define=API_URL tiene prioridad (p. ej. USB con 127.0.0.1).
    if (_override.isNotEmpty) add(_override);
    add(manual);
    add(lastOk);

    if (kIsWeb) {
      add('http://localhost:8080');
    } else {
      try {
        final plugin = DeviceInfoPlugin();
        if (Platform.isAndroid) {
          final info = await plugin.androidInfo;
          if (info.isPhysicalDevice) {
            // USB + adb reverse
            add('http://127.0.0.1:8080');
            // WiFi misma red (IP típica de desarrollo local)
            add('http://192.168.1.211:8080');
          } else {
            add('http://10.0.2.2:8080');
            add('http://127.0.0.1:8080');
          }
        } else if (Platform.isIOS) {
          add('http://127.0.0.1:8080');
          add('http://192.168.1.211:8080');
        }
      } catch (_) {
        add('http://127.0.0.1:8080');
      }
    }

    add('http://192.168.1.211:8080');
    add(defaultHost);

    for (final url in candidates) {
      if (await isReachable(url)) {
        _resolved = url;
        return url;
      }
    }

    _resolved = candidates.first;
    return _resolved!;
  }

  static Future<void> saveCustomBaseUrl(String input) async {
    final normalized = parseUserUrl(input);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefsManualKey, normalized);
    resetResolved();
    if (await isReachable(normalized)) {
      _resolved = normalized;
      await markReachable(normalized);
    }
  }

  static Future<void> clearManualBaseUrl() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_prefsManualKey);
    resetResolved();
    await redetectBaseUrl();
  }

  static Future<void> markReachable(String url) async {
    final normalized = _normalize(url);
    _resolved = normalized;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefsLastOkKey, normalized);
  }

  static Future<String?> getManualBaseUrl() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_prefsManualKey);
  }

  static Future<bool> isReachable(String url) async {
    try {
      final normalized = _normalize(url);
      final uri = Uri.parse('$normalized/api/configuracion/publica');
      // Render Free puede tardar en despertar; HTTPS usa timeout largo.
      final wait = normalized.startsWith('https://') ? timeout : probeTimeout;
      final r = await http.get(uri, headers: {'Accept': 'application/json'}).timeout(wait);
      return r.statusCode >= 200 && r.statusCode < 500;
    } catch (_) {
      return false;
    }
  }

  static String _normalize(String url) {
    var u = url.trim();
    while (u.endsWith('/')) {
      u = u.substring(0, u.length - 1);
    }
    return u;
  }
}
