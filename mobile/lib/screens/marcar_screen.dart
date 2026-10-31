import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../utils/asistencia_helpers.dart';
import '../utils/lima_time.dart';
import '../widgets/common.dart';
import '../widgets/dia_asistencia_card.dart';
import 'historial_asistencia_screen.dart';

class MarcarScreen extends StatefulWidget {
  const MarcarScreen({super.key, this.active = true, this.onMarcado});

  /// Reloj y sondeo solo corren mientras la pestaña está visible.
  final bool active;
  final VoidCallback? onMarcado;

  @override
  State<MarcarScreen> createState() => _MarcarScreenState();
}

class _MarcarScreenState extends State<MarcarScreen> with WidgetsBindingObserver {
  static const _diasHistorial = 14;
  static const _syncEvery = Duration(seconds: 15);

  List<DiaAsistencia> _dias = [];
  bool _loading = true;
  bool _saving = false;
  String? _error;
  String? _ok;
  Timer? _clock;
  Timer? _syncTimer;
  DateTime _now = LimaTime.now();
  String _signature = '';
  String? _expandedFecha;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _dias = ultimosDiasCalendario(_diasHistorial, const []);
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
      setState(() => _expandedFecha = null);
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

  Future<void> _load({bool initial = false, bool silent = false}) async {
    if (!silent && mounted) {
      setState(() {
        if (initial) _loading = true;
        _error = null;
      });
    }
    try {
      final hoy = LimaTime.todayIso();
      final desde = isoOf(LimaTime.now().subtract(const Duration(days: 13)));
      final rows = await context.read<AuthProvider>().marcacionesRango(
            desde: desde,
            hasta: hoy,
          );
      if (!mounted) return;
      final dias = ultimosDiasCalendario(_diasHistorial, rows);
      final sig = dias.map((d) => d.signature).join('|');
      if (silent && sig == _signature) return;
      setState(() {
        _dias = dias;
        _signature = sig;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        if (!silent) _error = e.toString();
        _loading = false;
      });
    }
  }

  DiaAsistencia get _hoy =>
      _dias.isEmpty ? DiaAsistencia(fecha: LimaTime.todayIso()) : _dias.first;

  List<DiaAsistencia> get _diasAnteriores =>
      _dias.length <= 1 ? const [] : _dias.sublist(1);

  bool get _finDeSemanaHoy =>
      _now.weekday == DateTime.saturday || _now.weekday == DateTime.sunday;

  String? get _siguiente {
    if (_finDeSemanaHoy) return null;
    final day = _hoy;
    if (day.entrada == null) return 'INGRESO';
    if (day.salida == null) return 'SALIDA';
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
      final hora = fmtTimeMarcacion(m.fechaHora);
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

  void _toggleDiaAnterior(String fecha) {
    setState(() {
      _expandedFecha = _expandedFecha == fecha ? null : fecha;
    });
  }

  void _abrirHistorialCompleto() {
    setState(() => _expandedFecha = null);
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => const HistorialAsistenciaScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().usuario;
    final next = _siguiente;
    final day = _hoy;
    final jornadaCerrada = day.jornadaCompleta;
    final anteriores = _diasAnteriores;

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
                        DiaAsistenciaCard(
                          day: day,
                          titulo: 'Hoy · ${fmtDayShort(day.fecha)}',
                          enCursoHoy: true,
                        ),
                        const SizedBox(height: 16),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton.icon(
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
                        ),
                        const SizedBox(height: 10),
                        const Text(
                          'La hora la registra el servidor (hora de Lima), no el celular.',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 11,
                            color: AppColors.muted,
                          ),
                        ),
                        if (anteriores.isNotEmpty) ...[
                          const SizedBox(height: 28),
                          Row(
                            children: [
                              const Icon(Icons.history, size: 18, color: AppColors.navy),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Text(
                                      'Días anteriores',
                                      style: TextStyle(
                                        fontWeight: FontWeight.w800,
                                        fontSize: 15,
                                        color: AppColors.navy,
                                      ),
                                    ),
                                    Text(
                                      'Últimos ${anteriores.length} días · toca un día para ver detalle',
                                      style: const TextStyle(
                                        fontSize: 12,
                                        color: AppColors.muted,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          ...anteriores.map(
                            (d) => Padding(
                              padding: const EdgeInsets.only(bottom: 10),
                              child: DiaAsistenciaCard(
                                day: d,
                                titulo: fmtDayShort(d.fecha),
                                compact: true,
                                expandible: true,
                                expanded: _expandedFecha == d.fecha,
                                onTap: () => _toggleDiaAnterior(d.fecha),
                              ),
                            ),
                          ),
                          const SizedBox(height: 8),
                          OutlinedButton.icon(
                            onPressed: _abrirHistorialCompleto,
                            icon: const Icon(Icons.calendar_month_outlined),
                            label: const Text('Consultar historial completo'),
                          ),
                          const SizedBox(height: 8),
                        ] else ...[
                          const SizedBox(height: 20),
                          OutlinedButton.icon(
                            onPressed: _abrirHistorialCompleto,
                            icon: const Icon(Icons.calendar_month_outlined),
                            label: const Text('Consultar historial completo'),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}
