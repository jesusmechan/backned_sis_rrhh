DateTime? _fecha(Object? v) => v == null ? null : DateTime.tryParse(v.toString());

String? _hora(Object? v) {
  final s = v?.toString();
  if (s == null || s.isEmpty) return null;
  return s.length >= 5 ? s.substring(0, 5) : s;
}

/// Paso del circuito de aprobación de una solicitud.
class PasoAprobacion {
  const PasoAprobacion({
    required this.idPasoSolicitud,
    required this.numeroPaso,
    required this.nombrePaso,
    required this.estado,
    this.rol,
    this.usuarioAsignado,
    this.usuarioDecision,
    this.fechaDecision,
    this.comentario,
  });

  final int idPasoSolicitud;
  final int numeroPaso;
  final String nombrePaso;
  final String estado;
  final String? rol;
  final String? usuarioAsignado;
  final String? usuarioDecision;
  final DateTime? fechaDecision;
  final String? comentario;

  factory PasoAprobacion.fromJson(Map<String, dynamic> json) => PasoAprobacion(
        idPasoSolicitud: json['idPasoSolicitud'] as int,
        numeroPaso: (json['numeroPaso'] as num?)?.toInt() ?? 0,
        nombrePaso: json['nombrePaso'] as String? ?? '',
        estado: json['estado']?.toString() ?? '',
        rol: json['rol'] as String?,
        usuarioAsignado: json['usuarioAsignado'] as String?,
        usuarioDecision: json['usuarioDecision'] as String?,
        fechaDecision: _fecha(json['fechaDecision']),
        comentario: json['comentario'] as String?,
      );

  static List<PasoAprobacion> listFrom(Object? raw) => (raw as List<dynamic>? ?? [])
      .map((e) => PasoAprobacion.fromJson(e as Map<String, dynamic>))
      .toList()
    ..sort((a, b) => a.numeroPaso.compareTo(b.numeroPaso));
}

class HistorialSolicitud {
  const HistorialSolicitud({
    required this.accion,
    required this.estadoNuevo,
    required this.fechaHora,
    this.usuario,
    this.comentario,
  });

  final String accion;
  final String estadoNuevo;
  final DateTime? fechaHora;
  final String? usuario;
  final String? comentario;

  factory HistorialSolicitud.fromJson(Map<String, dynamic> json) => HistorialSolicitud(
        accion: json['accion']?.toString() ?? '',
        estadoNuevo: json['estadoNuevo']?.toString() ?? '',
        fechaHora: _fecha(json['fechaHora']),
        usuario: json['usuario'] as String?,
        comentario: json['comentario'] as String?,
      );
}

class HoraExtra {
  const HoraExtra({
    required this.idSolicitudHoraExtra,
    required this.fecha,
    required this.horaInicio,
    required this.horaFin,
    required this.cantidadHoras,
    required this.motivo,
    required this.estado,
    this.empleado = '',
    this.flujo,
    this.fechaCreacion,
    this.pasos = const [],
  });

  final int idSolicitudHoraExtra;
  final String fecha;
  final String horaInicio;
  final String horaFin;
  final double cantidadHoras;
  final String motivo;
  final String estado;
  final String empleado;
  final String? flujo;
  final DateTime? fechaCreacion;
  final List<PasoAprobacion> pasos;

  factory HoraExtra.fromJson(Map<String, dynamic> json) => HoraExtra(
        idSolicitudHoraExtra: json['idSolicitudHoraExtra'] as int,
        fecha: json['fecha']?.toString() ?? '',
        horaInicio: _hora(json['horaInicio']) ?? '',
        horaFin: _hora(json['horaFin']) ?? '',
        cantidadHoras: (json['cantidadHoras'] as num?)?.toDouble() ?? 0,
        motivo: json['motivo'] as String? ?? '',
        estado: json['estado']?.toString() ?? '',
        empleado: json['empleado'] as String? ?? '',
        flujo: json['flujo'] as String?,
        fechaCreacion: _fecha(json['fechaCreacion']),
        pasos: PasoAprobacion.listFrom(json['pasos']),
      );
}

/// Paso que el usuario debe atender (o que ya atendió, en la vista de seguimiento).
class BandejaItem {
  const BandejaItem({
    required this.idPasoSolicitud,
    required this.tipoSolicitud,
    required this.idSolicitud,
    required this.solicitante,
    required this.tipoTramite,
    required this.numeroPaso,
    required this.nombrePaso,
    required this.motivo,
    required this.estadoSolicitud,
    required this.puedeDecidir,
    this.fechaInicio,
  });

  final int idPasoSolicitud;
  final String tipoSolicitud;
  final int idSolicitud;
  final String solicitante;
  final String tipoTramite;
  final int numeroPaso;
  final String nombrePaso;
  final String motivo;
  final String estadoSolicitud;
  final bool puedeDecidir;
  final DateTime? fechaInicio;

  bool get esPermiso => tipoSolicitud.toUpperCase() == 'PERMISO';

  factory BandejaItem.fromJson(Map<String, dynamic> json) => BandejaItem(
        idPasoSolicitud: json['idPasoSolicitud'] as int,
        tipoSolicitud: json['tipoSolicitud'] as String? ?? '',
        idSolicitud: json['idSolicitud'] as int,
        solicitante: json['solicitante'] as String? ?? '',
        tipoTramite: json['tipoTramite'] as String? ?? '',
        numeroPaso: (json['numeroPaso'] as num?)?.toInt() ?? 0,
        nombrePaso: json['nombrePaso'] as String? ?? '',
        motivo: json['motivo'] as String? ?? '',
        estadoSolicitud: json['estadoSolicitud']?.toString() ?? '',
        puedeDecidir: json['puedeDecidir'] as bool? ?? false,
        fechaInicio: _fecha(json['fechaInicio']),
      );
}

class SaldoVacaciones {
  const SaldoVacaciones({
    required this.diasGanados,
    required this.diasUsados,
    required this.diasDisponibles,
  });

  final double diasGanados;
  final double diasUsados;
  final double diasDisponibles;

  factory SaldoVacaciones.fromJson(Map<String, dynamic> json) => SaldoVacaciones(
        diasGanados: (json['diasGanados'] as num?)?.toDouble() ?? 0,
        diasUsados: (json['diasUsados'] as num?)?.toDouble() ?? 0,
        diasDisponibles: (json['diasDisponibles'] as num?)?.toDouble() ?? 0,
      );
}
