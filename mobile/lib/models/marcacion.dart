class Marcacion {
  const Marcacion({
    required this.idMarcacion,
    required this.idEmpleado,
    required this.empleado,
    required this.tipo,
    required this.fechaHora,
    this.fecha,
    this.origen,
    this.observacion,
  });

  final int idMarcacion;
  final int idEmpleado;
  final String empleado;
  final String tipo;
  final DateTime fechaHora;
  final String? fecha;
  final String? origen;
  final String? observacion;

  bool get esIngreso => tipo.toUpperCase() == 'INGRESO';
  bool get esSalida => tipo.toUpperCase() == 'SALIDA';

  factory Marcacion.fromJson(Map<String, dynamic> json) {
    return Marcacion(
      idMarcacion: json['idMarcacion'] as int,
      idEmpleado: json['idEmpleado'] as int,
      empleado: json['empleado'] as String? ?? '',
      tipo: json['tipo'] as String? ?? '',
      fechaHora: DateTime.parse(json['fechaHora'] as String),
      fecha: json['fecha']?.toString(),
      origen: json['origen'] as String?,
      observacion: json['observacion'] as String?,
    );
  }
}
