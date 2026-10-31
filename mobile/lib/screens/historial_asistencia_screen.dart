import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../utils/asistencia_helpers.dart';
import '../utils/lima_time.dart';
import '../widgets/common.dart';
import '../widgets/dia_asistencia_card.dart';

class HistorialAsistenciaScreen extends StatefulWidget {
  const HistorialAsistenciaScreen({super.key});

  @override
  State<HistorialAsistenciaScreen> createState() => _HistorialAsistenciaScreenState();
}

class _HistorialAsistenciaScreenState extends State<HistorialAsistenciaScreen> {
  final _busquedaCtrl = TextEditingController();
  String _busqueda = '';

  late String _desde;
  late String _hasta;

  List<DiaAsistencia> _dias = [];
  bool _loading = true;
  String? _error;
  String? _expandedFecha;

  @override
  void initState() {
    super.initState();
    // Rango real de consulta (último año). Hoy se usa como ejemplo en el calendario.
    _hasta = LimaTime.todayIso();
    _desde = isoOf(LimaTime.now().subtract(const Duration(days: 365)));
    _busquedaCtrl.addListener(() {
      setState(() => _busqueda = _busquedaCtrl.text);
    });
    _load();
  }

  @override
  void dispose() {
    _busquedaCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final rows = await context.read<AuthProvider>().marcacionesRangoTodasPaginas(
            desde: _desde,
            hasta: _hasta,
          );
      if (!mounted) return;
      setState(() {
        _dias = agruparMarcaciones(rows);
        _loading = false;
        _expandedFecha = null;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  Future<bool> _elegirFecha({required bool esDesde}) async {
    final firstDate = DateTime(2020);
    final lastDate = DateTime.now().add(const Duration(days: 1));
    // El calendario abre en hoy como ejemplo; el valor guardado se aplica al elegir.
    final ahora = LimaTime.now();
    var initial = DateTime(ahora.year, ahora.month, ahora.day);
    if (initial.isBefore(firstDate)) initial = firstDate;
    if (initial.isAfter(lastDate)) initial = lastDate;
    final picked = await showDialog<DateTime>(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: Text(esDesde ? 'Desde' : 'Hasta'),
          contentPadding: const EdgeInsets.fromLTRB(0, 12, 0, 0),
          content: SizedBox(
            width: double.maxFinite,
            child: CalendarDatePicker(
              initialDate: initial,
              currentDate: initial,
              firstDate: firstDate,
              lastDate: lastDate,
              onDateChanged: (d) => Navigator.pop(ctx, d),
            ),
          ),
        );
      },
    );
    if (picked == null || !mounted) return false;
    setState(() {
      final iso = isoOf(picked);
      if (esDesde) {
        _desde = iso;
        if (_desde.compareTo(_hasta) > 0) _hasta = _desde;
      } else {
        _hasta = iso;
        if (_hasta.compareTo(_desde) < 0) _desde = _hasta;
      }
    });
    return true;
  }

  Future<void> _mostrarFiltroFechas() async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setSheetState) {
            return Padding(
              padding: EdgeInsets.fromLTRB(
                20,
                8,
                20,
                20 + MediaQuery.paddingOf(ctx).bottom,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    'Filtrar por fechas',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Hora de Lima · rango inclusive · ej. ${_fmtFiltro(LimaTime.todayIso())}',
                    style: const TextStyle(fontSize: 12, color: AppColors.muted),
                  ),
                  const SizedBox(height: 16),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.date_range_outlined),
                    title: const Text('Desde'),
                    subtitle: Text(_fmtFiltro(_desde)),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () async {
                      final ok = await _elegirFecha(esDesde: true);
                      if (!ctx.mounted || !ok) return;
                      // Solo refresca el sheet; no lo cierra.
                      setSheetState(() {});
                      if (mounted) setState(() {});
                    },
                  ),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.event_outlined),
                    title: const Text('Hasta'),
                    subtitle: Text(_fmtFiltro(_hasta)),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () async {
                      final ok = await _elegirFecha(esDesde: false);
                      if (!ctx.mounted || !ok) return;
                      setSheetState(() {});
                      if (mounted) setState(() {});
                    },
                  ),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: () {
                      Navigator.pop(ctx);
                      _load();
                    },
                    child: const Text('Aplicar filtro'),
                  ),
                  TextButton(
                    onPressed: () {
                      setState(() {
                        _hasta = LimaTime.todayIso();
                        _desde =
                            isoOf(LimaTime.now().subtract(const Duration(days: 365)));
                      });
                      Navigator.pop(ctx);
                      _load();
                    },
                    child: const Text('Restablecer último año'),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  String _fmtFiltro(String iso) {
    return DateFormat('dd/MM/yyyy').format(parseIsoDate(iso));
  }

  List<DiaAsistencia> get _diasFiltrados =>
      _dias.where((d) => diaCoincideBusqueda(d, _busqueda)).toList();

  @override
  Widget build(BuildContext context) {
    final visibles = _diasFiltrados;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Historial de asistencia'),
        actions: [
          IconButton(
            onPressed: _loading ? null : _mostrarFiltroFechas,
            icon: const Icon(Icons.filter_list),
            tooltip: 'Filtrar fechas',
          ),
          IconButton(
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar',
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextField(
                  controller: _busquedaCtrl,
                  decoration: InputDecoration(
                    hintText: 'Buscar por día, hora o estado…',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: _busqueda.isEmpty
                        ? null
                        : IconButton(
                            onPressed: () {
                              _busquedaCtrl.clear();
                              FocusScope.of(context).unfocus();
                            },
                            icon: const Icon(Icons.close),
                          ),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  '${_fmtFiltro(_desde)} – ${_fmtFiltro(_hasta)}'
                  '${_loading ? '' : ' · ${visibles.length} día${visibles.length == 1 ? '' : 's'}'}',
                  style: const TextStyle(fontSize: 12, color: AppColors.muted),
                ),
              ],
            ),
          ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: _load,
                    child: visibles.isEmpty
                        ? ListView(
                            physics: const AlwaysScrollableScrollPhysics(),
                            padding: const EdgeInsets.all(24),
                            children: [
                              ErrorBanner(_error),
                              const SizedBox(height: 24),
                              Icon(Icons.event_busy_outlined,
                                  size: 48, color: AppColors.muted.withValues(alpha: 0.6)),
                              const SizedBox(height: 12),
                              Text(
                                _error != null
                                    ? 'No se pudo cargar el historial.'
                                    : _busqueda.isNotEmpty
                                        ? 'Sin resultados para «$_busqueda».'
                                        : 'No hay marcaciones en este periodo.',
                                textAlign: TextAlign.center,
                                style: const TextStyle(color: AppColors.muted),
                              ),
                            ],
                          )
                        : ListView.builder(
                            physics: const AlwaysScrollableScrollPhysics(),
                            padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                            itemCount: visibles.length + (_error != null ? 1 : 0),
                            itemBuilder: (context, i) {
                              if (_error != null && i == 0) {
                                return ErrorBanner(_error);
                              }
                              final idx = _error != null ? i - 1 : i;
                              final d = visibles[idx];
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 10),
                                child: DiaAsistenciaCard(
                                  day: d,
                                  titulo: fmtDayShort(d.fecha),
                                  compact: true,
                                  expandible: true,
                                  expanded: _expandedFecha == d.fecha,
                                  onTap: () {
                                    setState(() {
                                      _expandedFecha =
                                          _expandedFecha == d.fecha ? null : d.fecha;
                                    });
                                  },
                                ),
                              );
                            },
                          ),
                  ),
          ),
        ],
      ),
    );
  }
}
