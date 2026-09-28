import 'package:flutter/foundation.dart';

import '../models/marcacion.dart';
import '../models/notificacion.dart';
import '../models/permiso.dart';
import '../models/solicitud.dart';
import '../models/usuario.dart';
import '../services/api_client.dart';
import '../utils/lima_time.dart';

class AuthProvider extends ChangeNotifier {
  AuthProvider(this._api) {
    _api.onSessionExpired = () {
      _usuario = null;
      _error = 'Su sesión expiró. Vuelva a ingresar.';
      notifyListeners();
    };
  }

  final ApiClient _api;

  Usuario? _usuario;
  bool _booting = true;
  bool _busy = false;
  String? _error;

  Usuario? get usuario => _usuario;
  bool get isAuth => _usuario != null;
  bool get booting => _booting;
  bool get busy => _busy;
  String? get error => _error;
  ApiClient get api => _api;

  Future<void> bootstrap() async {
    _booting = true;
    notifyListeners();
    try {
      _usuario = await _api.getStoredUser();
      if (_usuario != null) {
        try {
          final raw = await _api.get('/api/sesion') as Map<String, dynamic>;
          _usuario = Usuario.fromJson(raw);
          await _api.saveUser(_usuario!);
        } on ApiException catch (e) {
          if (e.statusCode == 401) _usuario = null;
        } catch (_) {}
      }
    } finally {
      _booting = false;
      notifyListeners();
    }
  }

  Future<void> login(String nombreUsuario, String password) async {
    _busy = true;
    _error = null;
    notifyListeners();
    try {
      final data = await _api.post('/api/auth/login', body: {
        'nombreUsuario': nombreUsuario.trim().toLowerCase(),
        'password': password,
      }) as Map<String, dynamic>;
      final user = Usuario.fromJson(data['usuario'] as Map<String, dynamic>);
      await _api.saveSession(
        accessToken: data['accessToken'] as String,
        refreshToken: data['refreshToken'] as String,
        usuario: user,
      );
      _usuario = user;
    } on ApiException catch (e) {
      _error = e.sinConexion
          ? 'No se pudo conectar con el servidor (${_api.baseUrl}).'
          : e.message;
      rethrow;
    } finally {
      _busy = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    try {
      final refresh = await _api.getRefreshToken();
      if (refresh != null) {
        await _api.post('/api/auth/logout', body: {'refreshToken': refresh});
      }
    } catch (_) {}
    await _api.clearSession();
    _usuario = null;
    _error = null;
    notifyListeners();
  }

  int _requireEmpleado() {
    final id = _usuario?.idEmpleado;
    if (id == null) throw ApiException('Su usuario no está asociado a un trabajador.');
    return id;
  }

  List<Map<String, dynamic>> _content(Object? page) =>
      ((page as Map<String, dynamic>)['content'] as List<dynamic>? ?? [])
          .cast<Map<String, dynamic>>();

  // ---------------------------------------------------------------- Asistencia

  Future<List<Marcacion>> marcacionesHoy() async {
    final hoy = LimaTime.todayIso();
    return marcacionesRango(desde: hoy, hasta: hoy);
  }

  Future<List<Marcacion>> marcacionesRango({
    required String desde,
    required String hasta,
    int size = 50,
  }) async {
    final id = _usuario?.idEmpleado;
    if (id == null) return [];
    final data = await _api.get('/api/asistencias', query: {
      'desde': desde,
      'hasta': hasta,
      'page': '1',
      'size': '$size',
      'idEmpleado': '$id',
    });
    return _content(data).map(Marcacion.fromJson).toList();
  }

  /// La hora la pone el servidor: el reloj del celular puede estar adelantado o manipulado.
  Future<Marcacion> marcar(String tipo) async {
    final data = await _api.post(
      '/api/asistencias/marcar',
      body: {'idEmpleado': _requireEmpleado(), 'tipo': tipo, 'origen': 'MOVIL'},
      idempotencyKey: ApiClient.newIdempotencyKey(),
    ) as Map<String, dynamic>;
    return Marcacion.fromJson(data);
  }

  // ------------------------------------------------------------------ Permisos

  Future<List<Permiso>> listarPermisos({String? estado}) async {
    final query = <String, String>{'page': '1', 'size': '100'};
    if (estado != null && estado.isNotEmpty) query['estado'] = estado;
    final propios = _usuario?.idEmpleado;
    return _content(await _api.get('/api/permisos', query: query))
        .map(Permiso.fromJson)
        .where((p) => p.idEmpleado == propios)
        .toList()
      ..sort((a, b) => b.idSolicitudPermiso.compareTo(a.idSolicitudPermiso));
  }

  Future<Permiso> obtenerPermiso(int id) async =>
      Permiso.fromJson(await _api.get('/api/permisos/$id') as Map<String, dynamic>);

  Future<List<HistorialSolicitud>> historialPermiso(int id) async =>
      (await _api.get('/api/permisos/$id/historial') as List<dynamic>)
          .map((e) => HistorialSolicitud.fromJson(e as Map<String, dynamic>))
          .toList();

  Future<void> cancelarPermiso(int id) =>
      _api.post('/api/permisos/$id/cancelar');

  Future<List<TipoPermiso>> tiposPermiso() async {
    final data = await _api.get('/api/catalogos/tipos-permiso') as List<dynamic>;
    return data.map((e) => TipoPermiso.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<SaldoVacaciones> saldoVacaciones() async => SaldoVacaciones.fromJson(
      await _api.get('/api/contratos/saldo-vacaciones') as Map<String, dynamic>);

  Future<Permiso> crearPermiso({
    required int idTipoPermiso,
    required String fechaInicio,
    required String fechaFin,
    required String motivo,
    String? horaInicio,
    String? horaFin,
  }) async {
    final body = <String, dynamic>{
      'idEmpleado': _requireEmpleado(),
      'idTipoPermiso': idTipoPermiso,
      'fechaInicio': fechaInicio,
      'fechaFin': fechaFin,
      'motivo': motivo.trim(),
    };
    if (horaInicio != null && horaInicio.isNotEmpty) body['horaInicio'] = _conSegundos(horaInicio);
    if (horaFin != null && horaFin.isNotEmpty) body['horaFin'] = _conSegundos(horaFin);
    final data = await _api.post('/api/permisos', body: body) as Map<String, dynamic>;
    return Permiso.fromJson(data);
  }

  String _conSegundos(String hora) => hora.length == 5 ? '$hora:00' : hora;

  // -------------------------------------------------------------- Horas extras

  Future<List<HoraExtra>> listarHorasExtras() async {
    final propios = _usuario?.idEmpleado;
    final rows = _content(await _api.get('/api/horas-extras', query: {'page': '1', 'size': '100'}));
    return rows
        .where((r) => r['idEmpleado'] == propios)
        .map(HoraExtra.fromJson)
        .toList()
      ..sort((a, b) => b.idSolicitudHoraExtra.compareTo(a.idSolicitudHoraExtra));
  }

  Future<HoraExtra> obtenerHoraExtra(int id) async =>
      HoraExtra.fromJson(await _api.get('/api/horas-extras/$id') as Map<String, dynamic>);

  Future<List<HistorialSolicitud>> historialHoraExtra(int id) async =>
      (await _api.get('/api/horas-extras/$id/historial') as List<dynamic>)
          .map((e) => HistorialSolicitud.fromJson(e as Map<String, dynamic>))
          .toList();

  Future<void> cancelarHoraExtra(int id) =>
      _api.post('/api/horas-extras/$id/cancelar');

  Future<HoraExtra> crearHoraExtra({
    required String fecha,
    required String horaInicio,
    required String horaFin,
    required double cantidadHoras,
    required String motivo,
  }) async {
    final data = await _api.post(
      '/api/horas-extras',
      body: {
        'idEmpleado': _requireEmpleado(),
        'fecha': fecha,
        'horaInicio': _conSegundos(horaInicio),
        'horaFin': _conSegundos(horaFin),
        'cantidadHoras': cantidadHoras,
        'motivo': motivo.trim(),
      },
    ) as Map<String, dynamic>;
    return HoraExtra.fromJson(data);
  }

  // ------------------------------------------------------------------- Bandeja

  Future<List<BandejaItem>> bandeja({bool seguimiento = false}) async {
    final query = {'page': '1', 'size': '100', 'orden': 'ASC'};
    if (seguimiento) query['vista'] = 'SEGUIMIENTO';
    return _content(await _api.get('/api/bandeja', query: query))
        .map(BandejaItem.fromJson)
        .toList();
  }

  Future<int> contarPorAtender() async {
    final data = await _api.get('/api/bandeja', query: {'page': '1', 'size': '1'})
        as Map<String, dynamic>;
    return (data['totalElements'] as num?)?.toInt() ?? 0;
  }

  Future<BandejaItem> paso(int idPaso) async =>
      BandejaItem.fromJson(await _api.get('/api/pasos/$idPaso') as Map<String, dynamic>);

  Future<void> decidir(int idPaso, {required bool aprobar, required String comentario}) =>
      _api.post(
        '/api/pasos/$idPaso/${aprobar ? 'aprobar' : 'rechazar'}',
        body: {'comentario': comentario},
        idempotencyKey: ApiClient.newIdempotencyKey(),
      );

  // ------------------------------------------------------------ Notificaciones

  Future<List<Notificacion>> listarNotificaciones() async {
    final data = await _api.get('/api/notificaciones') as List<dynamic>;
    return data.map((e) => Notificacion.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<int> contarNoLeidas() async {
    final data = await _api.get('/api/notificaciones/no-leidas') as Map<String, dynamic>;
    return (data['noLeidas'] as num?)?.toInt() ?? 0;
  }

  Future<void> marcarNotificacionLeida(int id) async {
    await _api.post('/api/notificaciones/$id/leer');
  }

  Future<void> marcarTodasLeidas() async {
    await _api.post('/api/notificaciones/leer-todas');
  }
}
