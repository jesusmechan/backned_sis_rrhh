/// Zona horaria de negocio: America/Lima (UTC-5, sin horario de verano).
class LimaTime {
  LimaTime._();

  static const Duration offset = Duration(hours: -5);

  /// Ahora en hora de Lima (reloj de pared).
  static DateTime now() => DateTime.now().toUtc().add(offset);

  /// Convierte cualquier instante a reloj de pared Lima.
  static DateTime of(DateTime value) => value.toUtc().add(offset);

  /// Fecha ISO `yyyy-MM-dd` en Lima.
  static String todayIso() {
    final n = now();
    final y = n.year.toString().padLeft(4, '0');
    final m = n.month.toString().padLeft(2, '0');
    final d = n.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }
}
