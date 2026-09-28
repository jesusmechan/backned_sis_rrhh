import 'solicitud.dart';

class Permiso {
  const Permiso({
    required this.idSolicitudPermiso,
    required this.idEmpleado,
    required this.empleado,
    required this.idTipoPermiso,
    required this.tipoPermiso,
    required this.fechaInicio,
    required this.fechaFin,
    required this.motivo,
    required this.estado,
    this.flujo,
    this.horaInicio,
    this.horaFin,
    this.fechaCreacion,
    this.pasos = const [],
  });

  final int idSolicitudPermiso;
  final int idEmpleado;
  final String empleado;
  final int idTipoPermiso;
  final String tipoPermiso;
  final String fechaInicio;
  final String fechaFin;
  final String motivo;
  final String estado;
  final String? flujo;
  final String? horaInicio;
  final String? horaFin;
  final DateTime? fechaCreacion;
  final List<PasoAprobacion> pasos;

  String get rango => fechaInicio == fechaFin ? fechaInicio : '$fechaInicio → $fechaFin';

  String? get horario => horaInicio != null && horaFin != null
      ? '${horaInicio!.substring(0, 5)} – ${horaFin!.substring(0, 5)}'
      : null;

  factory Permiso.fromJson(Map<String, dynamic> json) {
    return Permiso(
      idSolicitudPermiso: json['idSolicitudPermiso'] as int,
      idEmpleado: json['idEmpleado'] as int,
      empleado: json['empleado'] as String? ?? '',
      idTipoPermiso: json['idTipoPermiso'] as int,
      tipoPermiso: json['tipoPermiso'] as String? ?? '',
      fechaInicio: json['fechaInicio']?.toString() ?? '',
      fechaFin: json['fechaFin']?.toString() ?? '',
      motivo: json['motivo'] as String? ?? '',
      estado: json['estado'] as String? ?? '',
      flujo: json['flujo'] as String?,
      horaInicio: json['horaInicio']?.toString(),
      horaFin: json['horaFin']?.toString(),
      fechaCreacion: json['fechaCreacion'] != null
          ? DateTime.tryParse(json['fechaCreacion'].toString())
          : null,
      pasos: PasoAprobacion.listFrom(json['pasos']),
    );
  }
}

class TipoPermiso {
  const TipoPermiso({
    required this.id,
    required this.codigo,
    required this.nombre,
  });

  final int id;
  final String codigo;
  final String nombre;

  factory TipoPermiso.fromJson(Map<String, dynamic> json) {
    return TipoPermiso(
      id: (json['id'] ?? json['idTipoPermiso']) as int,
      codigo: json['codigo'] as String? ?? '',
      nombre: json['nombre'] as String? ?? json['etiqueta'] as String? ?? '',
    );
  }
}
