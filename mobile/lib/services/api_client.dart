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
  ApiClient({String? baseUrl}) : baseUrl = baseUrl ?? ApiConfig.baseUrl;

  final String baseUrl;

  static const _accessKey = 'andina.access';
  static const _refreshKey = 'andina.refresh';
  static const _userKey = 'andina.user';

  static final _random = Random.secure();

  Future<String?>? _refreshing;

  /// Se invoca cuando el refresh token ya no sirve y la sesión se cerró.
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

  /// Clave para que el API ignore un reintento de la misma operación.
  static String newIdempotencyKey() =>
      List.generate(16, (_) => _random.nextInt(256).toRadixString(16).padLeft(2, '0')).join();

  Uri _uri(String path, [Map<String, String>? query]) {
    final normalized = path.startsWith('/') ? path : '/$path';
    return Uri.parse('$baseUrl$normalized').replace(queryParameters: query);
  }

  Future<dynamic> get(String path, {Map<String, String>? query}) =>
      _send('GET', path, query: query);

  Future<dynamic> post(String path, {Object? body, String? idempotencyKey}) =>
      _send('POST', path, body: body, idempotencyKey: idempotencyKey);

  Future<dynamic> put(String path, {Object? body}) =>
      _send('PUT', path, body: body);

  Future<dynamic> patch(String path) => _send('PATCH', path);

  Future<dynamic> delete(String path) => _send('DELETE', path);

  /// Varias peticiones pueden recibir 401 a la vez; todas esperan el mismo refresh
  /// porque el API revoca el refresh token al usarlo.
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
            _uri('/api/auth/refresh'),
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
    return data['accessToken'] as String;
  }

  ApiException _sinConexion() => ApiException(
        'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo.',
      );

  Future<dynamic> _send(
    String method,
    String path, {
    Object? body,
    Map<String, String>? query,
    String? idempotencyKey,
    bool retried = false,
  }) async {
    final headers = <String, String>{'Accept': 'application/json'};
    if (body != null) headers['Content-Type'] = 'application/json';
    if (idempotencyKey != null) headers['Idempotency-Key'] = idempotencyKey;
    final token = await getAccessToken();
    if (token != null) headers['Authorization'] = 'Bearer $token';

    final uri = _uri(path, query);
    final encoded = body == null ? null : jsonEncode(body);

    final http.Response response;
    try {
      final Future<http.Response> request = switch (method) {
        'GET' => http.get(uri, headers: headers),
        'POST' => http.post(uri, headers: headers, body: encoded),
        'PUT' => http.put(uri, headers: headers, body: encoded),
        'PATCH' => http.patch(uri, headers: headers),
        'DELETE' => http.delete(uri, headers: headers),
        _ => throw ApiException('Método no soportado: $method'),
      };
      response = await request.timeout(ApiConfig.timeout);
    } on TimeoutException {
      throw _sinConexion();
    } on http.ClientException {
      throw _sinConexion();
    }

    if (response.statusCode == 401 && !path.startsWith('/api/auth/') && !retried) {
      final next = await _refreshAccess();
      if (next != null) {
        return _send(method, path,
            body: body, query: query, idempotencyKey: idempotencyKey, retried: true);
      }
      throw ApiException('Su sesión expiró. Vuelva a ingresar.', statusCode: 401);
    }

    if (response.statusCode == 204 || response.bodyBytes.isEmpty) return null;

    final text = utf8.decode(response.bodyBytes);
    if (response.statusCode >= 400) {
      throw ApiException(_parseError(response.statusCode, text), statusCode: response.statusCode);
    }

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
      >= 500 => 'El servidor tuvo un problema. Inténtelo en unos minutos.',
      _ => 'Error $status',
    };
  }
}
