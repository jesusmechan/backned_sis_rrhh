import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../models/marcacion.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../utils/lima_time.dart';
import '../widgets/common.dart';

class _DiaMarcacion {
  _DiaMarcacion({required this.fecha});

  final String fecha;
  Marcacion? entrada;
  Marcacion? salida;

  String get signature =>
      '${entrada?.idMarcacion ?? 0}-${salida?.idMarcacion ?? 0}-'
      '${entrada?.fechaHora.toIso8601String() ?? ''}-'
      '${salida?.fechaHora.toIso8601String() ?? ''}';
}

class MarcarScreen extends StatefulWidget {
  const MarcarScreen({super.key, this.active = true, this.onMarcado});

  /// Reloj y sondeo solo corren mientras la pestaña está visible.
  final bool active;
  final VoidCallback? onMarcado;

  @override
  State<MarcarScreen> createState() => _MarcarScreenState();
}

class _MarcarScreenState extends State<MarcarScreen>
    with WidgetsBindingObserver {
  static const _diasHistorial = 14;
  static const _syncEvery = Duration(seconds: 15);

  List<_DiaMarcacion> _dias = [];
  bool _loading = true;
  bool _saving = false;
  String? _error;
  String? _ok;
  Timer? _clock;
  Timer? _syncTimer;
  DateTime _now = LimaTime.now();
  late final PageController _pageController;
  int _pageIndex = 0;
  String _signature = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _pageController = PageController();
    _dias = _calendarioVacio();
    _load(initial: true);
    if (widget.active) _startTimers();
  }

  @override
  void didUpdateWidget(covariant MarcarScreen old) {
    super.didUpdateWidget(old);
    if (widget.active == old.active) return;
    if (widget.active) {
      _load(silent: true);
      _startTimers();
    } else {
      _stopTimers();
    }
  }

  void _startTimers() {
    _stopTimers();
    _now = LimaTime.now();
    _clock = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) setState(() => _now = LimaTime.now());
    });
    _syncTimer = Timer.periodic(_syncEvery, (_) => _load(silent: true));
  }

  void _stopTimers() {
    _clock?.cancel();
    _syncTimer?.cancel();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _stopTimers();
    _pageController.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!widget.active) return;
    if (state == AppLifecycleState.resumed) {
      _load(silent: true);
      _startTimers();
    } else if (state == AppLifecycleState.paused) {
      _stopTimers();
    }
  }

  String _isoOf(DateTime d) {
    final y = d.year.toString().padLeft(4, '0');
    final m = d.month.toString().padLeft(2, '0');
    final day = d.day.toString().padLeft(2, '0');
    return '$y-$m-$day';
  }

  List<_DiaMarcacion> _calendarioVacio() {
    final base = LimaTime.now();
    return List.generate(_diasHistorial, (i) {
      final d = base.subtract(Duration(days: i));
      return _DiaMarcacion(fecha: _isoOf(d));
    });
  }

  List<_DiaMarcacion> _construirDias(List<Marcacion> rows) {
    final map = <String, _DiaMarcacion>{};
    for (final r in rows) {
      final fecha = r.fecha ?? _isoOf(LimaTime.of(r.fechaHora));
      final day = map.putIfAbsent(fecha, () => _DiaMarcacion(fecha: fecha));
      if (r.esIngreso) day.entrada = r;
      if (r.esSalida) day.salida = r;
    }
    final base = LimaTime.now();
    return List.generate(_diasHistorial, (i) {
      final iso = _isoOf(base.subtract(Duration(days: i)));
      final found = map[iso];
      return found ?? _DiaMarcacion(fecha: iso);
    });
  }

  Future<void> _load({bool initial = false, bool silent = false}) async {
    if (!silent && mounted) {
      setState(() {
        if (initial) _loading = true;
        _error = null;
      });
    }
    try {
      final hoy = LimaTime.todayIso();
      final desde = _isoOf(LimaTime.now().subtract(const Duration(days: 13)));
      final rows = await context.read<AuthProvider>().marcacionesRango(
            desde: desde,
            hasta: hoy,
          );
      if (!mounted) return;
      final dias = _construirDias(rows);
      final sig = dias.map((d) => d.signature).join('|');
      if (silent && sig == _signature) return;
      setState(() {
        _dias = dias;
        _signature = sig;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      if (!silent) {
        setState(() {
          _error = e.toString();
          _loading = false;
        });
      }
    }
  }

  _DiaMarcacion get _diaActual =>
      _dias.isEmpty ? _DiaMarcacion(fecha: LimaTime.todayIso()) : _dias[_pageIndex];

  bool get _viendoHoy => _pageIndex == 0;

  bool get _finDeSemanaHoy =>
      _now.weekday == DateTime.saturday || _now.weekday == DateTime.sunday;

  String? get _siguiente {
    if (!_viendoHoy || _finDeSemanaHoy) return null;
    if (_diaActual.entrada == null) return 'INGRESO';
    if (_diaActual.salida == null) return 'SALIDA';
    return null;
  }

  Future<void> _marcar() async {
    final tipo = _siguiente;
    if (tipo == null) return;
    setState(() {
      _saving = true;
      _error = null;
      _ok = null;
    });
    try {
      final m = await context.read<AuthProvider>().marcar(tipo);
      HapticFeedback.mediumImpact();
      await _load();
      widget.onMarcado?.call();
      if (!mounted) return;
      final hora = _fmtTime(m.fechaHora);
      setState(() => _ok = tipo == 'INGRESO'
          ? 'Entrada registrada a las $hora.'
          : 'Salida registrada a las $hora.');
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  String _fmtTime(DateTime? dt, {bool withSeconds = false}) {
    if (dt == null) return '—';
    return DateFormat(withSeconds ? 'HH:mm:ss' : 'HH:mm').format(LimaTime.of(dt));
  }

  String _fmtDayTitle(String iso) {
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

  String _fmtDayShort(String iso) {
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

  String _duracion(_DiaMarcacion day) {
    if (day.entrada == null) return '—';
    final start = day.entrada!.fechaHora;
    final end = day.salida?.fechaHora ??
        (_viendoHoy ? DateTime.now().toUtc() : start);
    final ms = end.difference(start).inMilliseconds;
    if (ms < 0) return '—';
    final totalMin = ms ~/ 60000;
    final h = totalMin ~/ 60;
    final m = totalMin % 60;
    if (h == 0) return '$m min';
    return '$h h ${m.toString().padLeft(2, '0')} min';
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().usuario;
    final next = _siguiente;
    final day = _diaActual;
    final jornadaCerrada = day.entrada != null && day.salida != null;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Marcar asistencia'),
        actions: [
          IconButton(
            onPressed: _loading ? null : () => _load(),
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar',
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                Expanded(
                  child: RefreshIndicator(
                    onRefresh: () => _load(),
                    child: ListView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                      children: [
                        ErrorBanner(_error),
                        if (_ok != null)
                          Container(
                            margin: const EdgeInsets.only(bottom: 12),
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: AppColors.okSoft,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text(_ok!,
                                style: const TextStyle(color: AppColors.ok)),
                          ),
                        // Reloj solo relevante “hoy”
                        if (_viendoHoy) ...[
                          Card(
                            color: AppColors.navy,
                            child: Padding(
                              padding: const EdgeInsets.all(20),
                              child: Column(
                                children: [
                                  const Text(
                                    'HORA LIMA',
                                    style: TextStyle(
                                      color: Colors.white70,
                                      fontSize: 11,
                                      fontWeight: FontWeight.w800,
                                      letterSpacing: 1.4,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    DateFormat('HH:mm:ss').format(_now),
                                    style: const TextStyle(
                                      fontSize: 42,
                                      fontWeight: FontWeight.w800,
                                      color: Colors.white,
                                      fontFeatures: [FontFeature.tabularFigures()],
                                    ),
                                  ),
                                  Text(
                                    DateFormat("EEEE d 'de' MMMM", 'es').format(_now),
                                    style: const TextStyle(color: Colors.white70),
                                  ),
                                  const SizedBox(height: 10),
                                  StatusChip(
                                    label: _finDeSemanaHoy
                                        ? 'Fin de semana'
                                        : jornadaCerrada
                                            ? 'Jornada completa'
                                            : day.entrada != null
                                                ? 'En jornada'
                                                : 'Sin marcar',
                                    tone: jornadaCerrada
                                        ? ChipTone.ok
                                        : day.entrada != null
                                            ? ChipTone.info
                                            : ChipTone.warn,
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 12),
                        ] else ...[
                          Card(
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Row(
                                children: [
                                  const Icon(Icons.history, color: AppColors.info),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          _fmtDayTitle(day.fecha),
                                          style: const TextStyle(
                                            fontWeight: FontWeight.w800,
                                            fontSize: 15,
                                          ),
                                        ),
                                        const Text(
                                          'Marcación de un día anterior',
                                          style: TextStyle(
                                            fontSize: 12,
                                            color: AppColors.muted,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  TextButton(
                                    onPressed: () => _pageController.animateToPage(
                                      0,
                                      duration: const Duration(milliseconds: 280),
                                      curve: Curves.easeOut,
                                    ),
                                    child: const Text('Hoy'),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 12),
                        ],
                        // Indicador de swipe
                        Row(
                          children: [
                            const Icon(Icons.swipe, size: 16, color: AppColors.muted),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                _viendoHoy
                                    ? 'Desliza a la izquierda para ver días anteriores'
                                    : 'Desliza para cambiar de día · ${_pageIndex + 1}/$_diasHistorial',
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: AppColors.muted,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        SizedBox(
                          height: 210,
                          child: PageView.builder(
                            controller: _pageController,
                            itemCount: _dias.length,
                            onPageChanged: (i) => setState(() => _pageIndex = i),
                            itemBuilder: (context, index) {
                              final d = _dias[index];
                              final esHoy = index == 0;
                              return Padding(
                                padding: const EdgeInsets.symmetric(horizontal: 2),
                                child: _DiaPage(
                                  titulo: esHoy ? 'Hoy · ${_fmtDayShort(d.fecha)}' : _fmtDayShort(d.fecha),
                                  entrada: _fmtTime(d.entrada?.fechaHora, withSeconds: true),
                                  salida: _fmtTime(d.salida?.fechaHora, withSeconds: true),
                                  duracion: _duracion(d),
                                  tieneEntrada: d.entrada != null,
                                  tieneSalida: d.salida != null,
                                ),
                              );
                            },
                          ),
                        ),
                        const SizedBox(height: 8),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: List.generate(
                            _dias.length.clamp(0, 7),
                            (i) {
                              // Show first 7 dots representing proximity; highlight current if within
                              final active = _pageIndex == i;
                              return AnimatedContainer(
                                duration: const Duration(milliseconds: 200),
                                margin: const EdgeInsets.symmetric(horizontal: 3),
                                width: active ? 16 : 6,
                                height: 6,
                                decoration: BoxDecoration(
                                  color: active ? AppColors.navy : AppColors.line,
                                  borderRadius: BorderRadius.circular(99),
                                ),
                              );
                            },
                          ),
                        ),
                        if (_pageIndex >= 7)
                          Padding(
                            padding: const EdgeInsets.only(top: 6),
                            child: Text(
                              'Día ${_pageIndex + 1} de $_diasHistorial',
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                fontSize: 11,
                                color: AppColors.muted,
                              ),
                            ),
                          ),
                        const SizedBox(height: 16),
                        if (_viendoHoy) ...[
                          FilledButton.icon(
                            onPressed: (next == null ||
                                    _saving ||
                                    user?.idEmpleado == null)
                                ? null
                                : _marcar,
                            icon: Icon(next == 'SALIDA' ? Icons.logout : Icons.login),
                            label: Text(
                              _saving
                                  ? 'Registrando…'
                                  : next == null
                                      ? (_finDeSemanaHoy
                                          ? 'No disponible (finde)'
                                          : 'Jornada ya registrada')
                                      : (next == 'INGRESO'
                                          ? 'Registrar entrada'
                                          : 'Registrar salida'),
                            ),
                          ),
                          const SizedBox(height: 10),
                          Text(
                            'La hora la registra el servidor (hora de Lima), no el celular.',
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              fontSize: 11,
                              color: AppColors.muted,
                            ),
                          ),
                        ] else
                          const Text(
                            'Solo puedes marcar el día de hoy. Desliza hasta “Hoy” para registrar.',
                            textAlign: TextAlign.center,
                            style: TextStyle(fontSize: 12, color: AppColors.muted),
                          ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}

class _DiaPage extends StatelessWidget {
  const _DiaPage({
    required this.titulo,
    required this.entrada,
    required this.salida,
    required this.duracion,
    required this.tieneEntrada,
    required this.tieneSalida,
  });

  final String titulo;
  final String entrada;
  final String salida;
  final String duracion;
  final bool tieneEntrada;
  final bool tieneSalida;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              titulo,
              style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15),
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                Expanded(
                  child: _HoraCard(
                    icon: Icons.login,
                    label: 'Entrada',
                    time: entrada,
                    done: tieneEntrada,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _HoraCard(
                    icon: Icons.logout,
                    label: 'Salida',
                    time: salida,
                    done: tieneSalida,
                  ),
                ),
              ],
            ),
            const Spacer(),
            Row(
              children: [
                const Icon(Icons.timer_outlined, size: 16, color: AppColors.muted),
                const SizedBox(width: 6),
                Text(
                  'Tiempo: $duracion',
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: AppColors.muted,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _HoraCard extends StatelessWidget {
  const _HoraCard({
    required this.icon,
    required this.label,
    required this.time,
    required this.done,
  });

  final IconData icon;
  final String label;
  final String time;
  final bool done;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: done ? AppColors.okSoft : AppColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.line),
      ),
      child: Column(
        children: [
          Icon(icon, size: 18, color: done ? AppColors.ok : AppColors.info),
          const SizedBox(height: 6),
          Text(label, style: const TextStyle(color: AppColors.muted, fontSize: 12)),
          const SizedBox(height: 4),
          Text(
            time,
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w800,
              fontFamily: 'monospace',
              color: done ? AppColors.ok : AppColors.navy,
            ),
          ),
        ],
      ),
    );
  }
}
