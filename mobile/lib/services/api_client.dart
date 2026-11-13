import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import '../config/api_config.dart';
import '../models/usuario.dart';

class ApiException implements Exception {
  ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  bool get sinConexion => statusCode == null;

  @override
  String toString() => message;
}

class ApiClient {
  ApiClient({String? baseUrl}) : _baseUrlOverride = baseUrl;

  final String? _baseUrlOverride;
  String? _baseUrl;

  Future<String> get baseUrl async {
    if (_baseUrlOverride != null) return _baseUrlOverride;
    _baseUrl ??= await ApiConfig.resolveBaseUrl();
    return _baseUrl!;
  }

  String get baseUrlLabel => _baseUrl ?? _baseUrlOverride ?? ApiConfig.baseUrl;

  Future<void> prepareConnection() async {
    resetBaseUrl();
    _baseUrl = await ApiConfig.redetectBaseUrl();
  }

  Future<void> ensureReady() async {
    await baseUrl;
  }

  void resetBaseUrl() {
    _baseUrl = null;
    ApiConfig.resetResolved();
  }

  static const _accessKey = 'andina.access';
  static const _refreshKey = 'andina.refresh';
  static const _userKey = 'andina.user';

  static final _random = Random.secure();

  Future<String?>? _refreshing;

  void Function()? onSessionExpired;

  Future<String?> getAccessToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_accessKey);
  }

  Future<String?> getRefreshToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_refreshKey);
  }

  Future<Usuario?> getStoredUser() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_userKey);
    if (raw == null) return null;
    return Usuario.fromJson(jsonDecode(raw) as Map<String, dynamic>);
  }

  Future<void> saveSession({
    required String accessToken,
    required String refreshToken,
    required Usuario usuario,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_accessKey, accessToken);
    await prefs.setString(_refreshKey, refreshToken);
    await prefs.setString(_userKey, jsonEncode(usuario.toJson()));
  }

  Future<void> saveUser(Usuario usuario) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_userKey, jsonEncode(usuario.toJson()));
  }

  Future<void> clearSession() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_accessKey);
    await prefs.remove(_refreshKey);
    await prefs.remove(_userKey);
  }

  static String newIdempotencyKey() =>
      List.generate(16, (_) => _random.nextInt(256).toRadixString(16).padLeft(2, '0')).join();

  Future<Uri> _uri(String path, [Map<String, String>? query]) async {
    final normalized = path.startsWith('/') ? path : '/$path';
    final root = await baseUrl;
    return Uri.parse('$root$normalized').replace(queryParameters: query);
  }

  Future<dynamic> get(String path, {Map<String, String>? query}) =>
      _send('GET', path, query: query);

  Future<dynamic> post(String path, {Object? body, String? idempotencyKey}) =>
      _send('POST', path, body: body, idempotencyKey: idempotencyKey);

  Future<void> ping() async {
    final uri = await _uri('/api/configuracion/publica');
    final response = await http
        .get(uri, headers: {'Accept': 'application/json'})
        .timeout(ApiConfig.timeout);
    if (response.statusCode >= 500) {
      throw ApiException(
        'El servidor respondió con error ${response.statusCode}.',
        statusCode: response.statusCode,
      );
    }
    await ApiConfig.markReachable(await baseUrl);
  }

  Future<String?> _refreshAccess() {
    return _refreshing ??= _doRefresh().whenComplete(() => _refreshing = null);
  }

  Future<String?> _doRefresh() async {
    final refresh = await getRefreshToken();
    if (refresh == null) return null;
    final http.Response response;
    try {
      response = await http
          .post(
            await _uri('/api/auth/refresh'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'refreshToken': refresh}),
          )
          .timeout(ApiConfig.timeout);
    } on TimeoutException {
      throw _sinConexion();
    } on http.ClientException {
      throw _sinConexion();
    }
    if (response.statusCode >= 400) {
      await clearSession();
      onSessionExpired?.call();
      return null;
    }
    final data = jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
    final usuario = Usuario.fromJson(data['usuario'] as Map<String, dynamic>);
    await saveSession(
      accessToken: data['accessToken'] as String,
      refreshToken: data['refreshToken'] as String,
      usuario: usuario,
    );
    await ApiConfig.markReachable(await baseUrl);
    return data['accessToken'] as String;
  }

  ApiException _sinConexion() => ApiException(
        'No hay conexión con el servidor ($baseUrlLabel). '
        'Comprueba que el backend esté activo (:8080) y configura la IP de tu PC en '
        'Perfil → Servidor API o en login (Detectar auto / Guardar URL).',
      );

  Future<bool> _redetectAndRetry() async {
    if (_baseUrlOverride != null) return false;
    resetBaseUrl();
    _baseUrl = await ApiConfig.redetectBaseUrl();
    return await ApiConfig.isReachable(_baseUrl!);
  }

  Future<dynamic> _send(
    String method,
    String path, {
    Object? body,
    Map<String, String>? query,
    String? idempotencyKey,
    bool retriedAuth = false,
    bool retriedNetwork = false,
  }) async {
    final headers = <String, String>{'Accept': 'application/json'};
    if (body != null) headers['Content-Type'] = 'application/json';
    if (idempotencyKey != null) headers['Idempotency-Key'] = idempotencyKey;
    final token = await getAccessToken();
    if (token != null) headers['Authorization'] = 'Bearer $token';

    final uri = await _uri(path, query);
    final encoded = body == null ? null : jsonEncode(body);

    http.Response response;
    try {
      final Future<http.Response> request = switch (method) {
        'GET' => http.get(uri, headers: headers),
        'POST' => http.post(uri, headers: headers, body: encoded),
        _ => throw ApiException('Método no soportado: $method'),
      };
      response = await request.timeout(ApiConfig.timeout);
    } on TimeoutException {
      if (!retriedNetwork && await _redetectAndRetry()) {
        return _send(method, path,
            body: body,
            query: query,
            idempotencyKey: idempotencyKey,
            retriedAuth: retriedAuth,
            retriedNetwork: true);
      }
      throw _sinConexion();
    } on http.ClientException {
      if (!retriedNetwork && await _redetectAndRetry()) {
        return _send(method, path,
            body: body,
            query: query,
            idempotencyKey: idempotencyKey,
            retriedAuth: retriedAuth,
            retriedNetwork: true);
      }
      throw _sinConexion();
    }

    if (response.statusCode == 401 && !path.startsWith('/api/auth/') && !retriedAuth) {
      final next = await _refreshAccess();
      if (next != null) {
        return _send(method, path,
            body: body,
            query: query,
            idempotencyKey: idempotencyKey,
            retriedAuth: true,
            retriedNetwork: retriedNetwork);
      }
      throw ApiException('Su sesión expiró. Vuelva a ingresar.', statusCode: 401);
    }

    if (response.statusCode == 204 || response.bodyBytes.isEmpty) {
      if (response.statusCode >= 200 && response.statusCode < 300) {
        await ApiConfig.markReachable(await baseUrl);
      }
      return null;
    }

    final text = utf8.decode(response.bodyBytes);
    if (response.statusCode >= 400) {
      throw ApiException(_parseError(response.statusCode, text), statusCode: response.statusCode);
    }

    await ApiConfig.markReachable(await baseUrl);
    final contentType = response.headers['content-type'] ?? '';
    if (contentType.contains('application/json')) return jsonDecode(text);
    return text;
  }

  String _parseError(int status, String text) {
    try {
      final body = jsonDecode(text);
      if (body is Map && body['message'] != null && body['message'].toString().isNotEmpty) {
        return body['message'].toString();
      }
    } catch (_) {}
    return switch (status) {
      403 => 'No tiene permiso para esta acción.',
      404 => 'No se encontró el registro.',
      429 => 'Demasiados intentos. Espere un minuto e inténtelo de nuevo.',
      >= 500 => 'El servidor tuvo un problema. Comprueba que el backend siga en ejecución.',
      _ => 'Error $status',
    };
  }
}
