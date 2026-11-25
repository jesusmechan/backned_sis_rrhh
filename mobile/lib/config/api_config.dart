import 'dart:io' show Platform;

import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

/// URL base del API Spring Boot (puerto 8080).
///
/// Prioridad: URL manual (Cambiar/Guardar) → `--dart-define=API_URL` →
/// última URL que funcionó → sondeo (127.0.0.1 USB, 10.0.2.2 emulador, etc.).
class ApiConfig {
  ApiConfig._();

  static const String _override = String.fromEnvironment('API_URL');
  static const String _prefsManualKey = 'andina.api.baseUrl';
  static const String _prefsLastOkKey = 'andina.api.lastWorking';

  /// Timeout general (login, APIs). HTTPS en Render Free puede despertar lento.
  static const Duration timeout = Duration(seconds: 45);
  /// Sondeo local (LAN / emulador).
  static const Duration probeTimeout = Duration(seconds: 4);
  /// Primer contacto HTTPS: Render Free suele tardar 30–60 s en despertar.
  static const Duration cloudProbeTimeout = Duration(seconds: 75);

  static const String defaultHost = 'http://127.0.0.1:8080';

  static Duration waitFor(String url) =>
      url.startsWith('https://') ? cloudProbeTimeout : probeTimeout;

  static String? _resolved;

  static void resetResolved() => _resolved = null;

  static void setResolved(String url) => _resolved = _normalize(url);

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

  /// URL que el usuario/config eligió, sin sondear otros hosts.
  /// Sirve para «Probar conexión» y para respetar Cambiar/Guardar.
  static Future<String> preferredBaseUrl() async {
    final prefs = await SharedPreferences.getInstance();
    final manual = prefs.getString(_prefsManualKey);
    if (manual != null && manual.trim().isNotEmpty) {
      try {
        return manual.startsWith('http') ? _normalize(manual) : parseUserUrl(manual);
      } catch (_) {}
    }
    if (_override.isNotEmpty) {
      try {
        return _override.startsWith('http')
            ? _normalize(_override)
            : parseUserUrl(_override);
      } catch (_) {}
    }
    final lastOk = prefs.getString(_prefsLastOkKey);
    if (lastOk != null && lastOk.trim().isNotEmpty) {
      return _normalize(lastOk);
    }
    if (_resolved != null) return _resolved!;
    return defaultHost;
  }

  /// Busca un servidor accesible. Si hay URL manual, siempre se respeta
  /// (aunque no responda ahora): el usuario la eligió a propósito.
  static Future<String> redetectBaseUrl() async {
    final prefs = await SharedPreferences.getInstance();
    final manual = prefs.getString(_prefsManualKey);

    if (manual != null && manual.trim().isNotEmpty) {
      try {
        final n = manual.startsWith('http') ? _normalize(manual) : parseUserUrl(manual);
        _resolved = n;
        return n;
      } catch (_) {}
    }

    final lastOk = prefs.getString(_prefsLastOkKey);

    final candidates = <String>[];
    void add(String? url) {
      if (url == null || url.trim().isEmpty) return;
      try {
        final n = url.startsWith('http') ? _normalize(url) : parseUserUrl(url);
        if (!candidates.contains(n)) candidates.add(n);
      } catch (_) {}
    }

    if (_override.isNotEmpty) add(_override);
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

    _resolved = candidates.isNotEmpty ? candidates.first : defaultHost;
    return _resolved!;
  }

  static Future<void> saveCustomBaseUrl(String input) async {
    final normalized = parseUserUrl(input);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefsManualKey, normalized);
    // Siempre aplicar la URL elegida; la comprobación de vida es aparte.
    _resolved = normalized;
    if (await isReachable(normalized)) {
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

  /// Si la URL ya no responde, quita el «último OK» para no fingir conexión.
  static Future<void> clearLastWorkingIf(String url) async {
    final normalized = _normalize(url);
    final prefs = await SharedPreferences.getInstance();
    if (prefs.getString(_prefsLastOkKey) == normalized) {
      await prefs.remove(_prefsLastOkKey);
    }
  }

  static Future<String?> getManualBaseUrl() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_prefsManualKey);
  }

  static Future<bool> isReachable(String url) async {
    final normalized = _normalize(url);
    final wait = waitFor(normalized);
    final cloud = normalized.startsWith('https://');
    // En nube: 2 intentos (el 1º suele despertar el dyno de Render Free).
    final attempts = cloud ? 2 : 1;
    for (var i = 0; i < attempts; i++) {
      try {
        if (cloud && i == 0) {
          // Despierta el servicio con health (más liviano) antes del smoke de negocio.
          try {
            await http
                .get(
                  Uri.parse('$normalized/actuator/health'),
                  headers: {'Accept': 'application/json'},
                )
                .timeout(wait);
          } catch (_) {}
        }
        final uri = Uri.parse('$normalized/api/configuracion/publica');
        final r =
            await http.get(uri, headers: {'Accept': 'application/json'}).timeout(wait);
        // Solo 2xx cuenta: un 404/502 significa que esa API ya no está (o no es la correcta).
        if (r.statusCode >= 200 && r.statusCode < 300) return true;
      } catch (_) {
        if (i == attempts - 1) return false;
      }
    }
    return false;
  }

  static String _normalize(String url) {
    var u = url.trim();
    while (u.endsWith('/')) {
      u = u.substring(0, u.length - 1);
    }
    return u;
  }
}
