class Usuario {
  const Usuario({
    required this.idUsuario,
    required this.nombreUsuario,
    required this.correo,
    required this.rol,
    required this.perfil,
    this.idRol,
    this.idEmpleado,
    this.nombreCompleto,
    this.permisos = const [],
  });

  final int idUsuario;
  final String nombreUsuario;
  final String correo;
  final String rol;
  final String perfil;
  final int? idRol;
  final int? idEmpleado;
  final String? nombreCompleto;
  final List<String> permisos;

  String get displayName =>
      (nombreCompleto != null && nombreCompleto!.trim().isNotEmpty)
          ? nombreCompleto!
          : nombreUsuario;

  String get initials {
    final parts = displayName.trim().split(RegExp(r'\s+'));
    if (parts.length >= 2) {
      return '${parts.first[0]}${parts[1][0]}'.toUpperCase();
    }
    return displayName.substring(0, displayName.length.clamp(0, 2)).toUpperCase();
  }

  factory Usuario.fromJson(Map<String, dynamic> json) {
    return Usuario(
      idUsuario: json['idUsuario'] as int,
      nombreUsuario: json['nombreUsuario'] as String? ?? '',
      correo: json['correo'] as String? ?? '',
      rol: json['rol'] as String? ?? '',
      perfil: json['perfil'] as String? ?? json['rol'] as String? ?? '',
      idRol: json['idRol'] as int?,
      idEmpleado: json['idEmpleado'] as int?,
      nombreCompleto: json['nombreCompleto'] as String?,
      permisos: (json['permisos'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
    );
  }

  Map<String, dynamic> toJson() => {
        'idUsuario': idUsuario,
        'nombreUsuario': nombreUsuario,
        'correo': correo,
        'rol': rol,
        'perfil': perfil,
        'idRol': idRol,
        'idEmpleado': idEmpleado,
        'nombreCompleto': nombreCompleto,
        'permisos': permisos,
      };
}
