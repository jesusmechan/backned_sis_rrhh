class Notificacion {
  const Notificacion({
    required this.idNotificacion,
    required this.tipo,
    required this.titulo,
    required this.mensaje,
    required this.leida,
    required this.fechaCreacion,
    this.ruta,
    this.tipoSolicitud,
    this.idSolicitud,
    this.idPaso,
  });

  final int idNotificacion;
  final String tipo;
  final String titulo;
  final String mensaje;
  final bool leida;
  final DateTime fechaCreacion;
  final String? ruta;
  final String? tipoSolicitud;
  final int? idSolicitud;
  final int? idPaso;

  Notificacion copyWith({bool? leida}) => Notificacion(
        idNotificacion: idNotificacion,
        tipo: tipo,
        titulo: titulo,
        mensaje: mensaje,
        leida: leida ?? this.leida,
        fechaCreacion: fechaCreacion,
        ruta: ruta,
        tipoSolicitud: tipoSolicitud,
        idSolicitud: idSolicitud,
        idPaso: idPaso,
      );

  factory Notificacion.fromJson(Map<String, dynamic> json) {
    return Notificacion(
      idNotificacion: json['idNotificacion'] as int,
      tipo: json['tipo'] as String? ?? '',
      titulo: json['titulo'] as String? ?? '',
      mensaje: json['mensaje'] as String? ?? '',
      leida: json['leida'] as bool? ?? false,
      fechaCreacion: DateTime.parse(json['fechaCreacion'] as String),
      ruta: json['ruta'] as String?,
      tipoSolicitud: json['tipoSolicitud'] as String?,
      idSolicitud: json['idSolicitud'] as int?,
      idPaso: json['idPaso'] as int?,
    );
  }
}
