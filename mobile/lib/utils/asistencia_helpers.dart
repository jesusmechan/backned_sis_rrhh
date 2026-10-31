import 'package:intl/intl.dart';

import '../models/marcacion.dart';
import 'lima_time.dart';

class DiaAsistencia {
  DiaAsistencia({required this.fecha});

  final String fecha;
  Marcacion? entrada;
  Marcacion? salida;

  bool get tieneAlguna => entrada != null || salida != null;
  bool get jornadaCompleta => entrada != null && salida != null;

  String get signature =>
      '${entrada?.idMarcacion ?? 0}-${salida?.idMarcacion ?? 0}-'
      '${entrada?.fechaHora.toIso8601String() ?? ''}-'
      '${salida?.fechaHora.toIso8601String() ?? ''}';
}

String isoOf(DateTime d) {
  final y = d.year.toString().padLeft(4, '0');
  final m = d.month.toString().padLeft(2, '0');
  final day = d.day.toString().padLeft(2, '0');
  return '$y-$m-$day';
}

String fmtTimeMarcacion(DateTime? dt, {bool withSeconds = false}) {
  if (dt == null) return '—';
  return DateFormat(withSeconds ? 'HH:mm:ss' : 'HH:mm').format(LimaTime.of(dt));
}

String fmtDayShort(String iso) {
  final parts = iso.split('-');
  if (parts.length != 3) return iso;
  final dt = DateTime.utc(
    int.parse(parts[0]),
    int.parse(parts[1]),
    int.parse(parts[2]),
    12,
  );
  return DateFormat('EEE d MMM', 'es').format(dt);
}

String fmtDayLong(String iso) {
  final parts = iso.split('-');
  if (parts.length != 3) return iso;
  final dt = DateTime.utc(
    int.parse(parts[0]),
    int.parse(parts[1]),
    int.parse(parts[2]),
    12,
  );
  return DateFormat("EEEE d 'de' MMMM", 'es').format(dt);
}

String duracionDia(DiaAsistencia day, {bool enCursoHoy = false}) {
  if (day.entrada == null) return '—';
  final start = day.entrada!.fechaHora;
  final end = day.salida?.fechaHora ??
      (enCursoHoy ? DateTime.now().toUtc() : start);
  final ms = end.difference(start).inMilliseconds;
  if (ms < 0) return '—';
  final totalMin = ms ~/ 60000;
  final h = totalMin ~/ 60;
  final m = totalMin % 60;
  if (h == 0) return '$m min';
  return '$h h ${m.toString().padLeft(2, '0')} min';
}

String resumenEstadoDia(DiaAsistencia day) {
  if (day.entrada == null && day.salida == null) return 'Sin marcación';
  if (day.jornadaCompleta) return 'Jornada completa';
  if (day.entrada != null) return 'Falta registrar salida';
  return 'Solo salida registrada';
}

void _asignarMarcacionAlDia(DiaAsistencia day, Marcacion r) {
  // Conserva la primera entrada y la última salida del día.
  if (r.esIngreso) {
    if (day.entrada == null || r.fechaHora.isBefore(day.entrada!.fechaHora)) {
      day.entrada = r;
    }
  }
  if (r.esSalida) {
    if (day.salida == null || r.fechaHora.isAfter(day.salida!.fechaHora)) {
      day.salida = r;
    }
  }
}

Map<String, DiaAsistencia> _mapaPorFecha(List<Marcacion> rows) {
  final map = <String, DiaAsistencia>{};
  final ordenadas = [...rows]..sort((a, b) => a.fechaHora.compareTo(b.fechaHora));
  for (final r in ordenadas) {
    final fecha = r.fecha ?? isoOf(LimaTime.of(r.fechaHora));
    final day = map.putIfAbsent(fecha, () => DiaAsistencia(fecha: fecha));
    _asignarMarcacionAlDia(day, r);
  }
  return map;
}

List<DiaAsistencia> agruparMarcaciones(List<Marcacion> rows) {
  return _mapaPorFecha(rows).values.toList()
    ..sort((a, b) => b.fecha.compareTo(a.fecha));
}

/// Últimos [cantidadDias] calendario (hoy incluido), rellenando días vacíos.
List<DiaAsistencia> ultimosDiasCalendario(int cantidadDias, List<Marcacion> rows) {
  final map = _mapaPorFecha(rows);
  final base = LimaTime.now();
  return List.generate(cantidadDias, (i) {
    final iso = isoOf(base.subtract(Duration(days: i)));
    return map[iso] ?? DiaAsistencia(fecha: iso);
  });
}

bool diaCoincideBusqueda(DiaAsistencia day, String query) {
  final q = query.trim().toLowerCase();
  if (q.isEmpty) return true;
  final blob = [
    day.fecha,
    fmtDayShort(day.fecha),
    fmtDayLong(day.fecha),
    resumenEstadoDia(day),
    fmtTimeMarcacion(day.entrada?.fechaHora, withSeconds: true),
    fmtTimeMarcacion(day.salida?.fechaHora, withSeconds: true),
    duracionDia(day),
  ].join(' ').toLowerCase();
  return blob.contains(q);
}

DateTime parseIsoDate(String iso) {
  final p = iso.split('-');
  return DateTime(int.parse(p[0]), int.parse(p[1]), int.parse(p[2]));
}
