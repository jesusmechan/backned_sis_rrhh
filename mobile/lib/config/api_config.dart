import 'dart:io' show Platform;

import 'package:flutter/foundation.dart' show kIsWeb;

/// URL base del API Spring Boot (:8080).
///
/// En un celular físico se indica la IP de la PC en la red local:
/// `flutter run --dart-define=API_URL=http://192.168.1.10:8080`
class ApiConfig {
  ApiConfig._();

  static const String _override = String.fromEnvironment('API_URL');

  static const Duration timeout = Duration(seconds: 15);

  static String get baseUrl {
    if (_override.isNotEmpty) return _override;
    if (kIsWeb) return 'http://localhost:8080';
    try {
      if (Platform.isAndroid) return 'http://10.0.2.2:8080';
    } catch (_) {}
    return 'http://localhost:8080';
  }
}
