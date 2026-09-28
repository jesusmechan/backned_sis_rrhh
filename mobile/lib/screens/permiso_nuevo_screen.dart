import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/permiso.dart';
import '../models/solicitud.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/common.dart';

class PermisoNuevoScreen extends StatefulWidget {
  const PermisoNuevoScreen({super.key});

  @override
  State<PermisoNuevoScreen> createState() => _PermisoNuevoScreenState();
}

class _PermisoNuevoScreenState extends State<PermisoNuevoScreen> {
  final _formKey = GlobalKey<FormState>();
  final _motivoCtrl = TextEditingController();
  List<TipoPermiso> _tipos = [];
  TipoPermiso? _tipo;
  DateTime? _inicio;
  DateTime? _fin;
  bool _porHoras = false;
  TimeOfDay? _horaInicio;
  TimeOfDay? _horaFin;
  SaldoVacaciones? _saldo;
  String? _saldoError;
  bool _loadingTipos = true;
  bool _saving = false;
  String? _error;

  bool get _esVacaciones => _tipo?.codigo.toUpperCase() == 'VACACIONES';

  /// Días calendario, igual que el cálculo del saldo en el servidor.
  int? get _diasPedidos =>
      _inicio == null || _fin == null ? null : _fin!.difference(_inicio!).inDays + 1;

  @override
  void initState() {
    super.initState();
    _cargarTipos();
  }

  @override
  void dispose() {
    _motivoCtrl.dispose();
    super.dispose();
  }

  Future<void> _cargarTipos() async {
    try {
      final tipos = await context.read<AuthProvider>().tiposPermiso();
      if (!mounted) return;
      setState(() {
        _tipos = tipos;
        _tipo = tipos.isNotEmpty ? tipos.first : null;
      });
      _alCambiarTipo();
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loadingTipos = false);
    }
  }

  Future<void> _alCambiarTipo() async {
    if (!_esVacaciones) return;
    setState(() => _porHoras = false);
    if (_saldo != null) return;
    try {
      final s = await context.read<AuthProvider>().saldoVacaciones();
      if (mounted) setState(() => _saldo = s);
    } catch (e) {
      if (mounted) setState(() => _saldoError = e.toString());
    }
  }

  Future<void> _pickDate({required bool inicio}) async {
    final now = DateTime.now();
    final selected = await showDatePicker(
      context: context,
      initialDate: (inicio ? _inicio : _fin) ?? _inicio ?? now,
      firstDate: DateTime(now.year - 1),
      lastDate: DateTime(now.year + 2),
      locale: const Locale('es'),
    );
    if (selected == null) return;
    setState(() {
      if (inicio) {
        _inicio = selected;
        if (_porHoras || (_fin != null && _fin!.isBefore(selected))) _fin = selected;
      } else {
        _fin = selected;
      }
    });
  }

  Future<void> _pickHora({required bool inicio}) async {
    final t = await showTimePicker(
      context: context,
      initialTime: (inicio ? _horaInicio : _horaFin) ??
          (inicio ? const TimeOfDay(hour: 9, minute: 0) : const TimeOfDay(hour: 11, minute: 0)),
    );
    if (t != null) setState(() => inicio ? _horaInicio = t : _horaFin = t);
  }

  String _iso(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  String _hhmm(TimeOfDay t) =>
      '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';

  String _label(DateTime? d) => d == null ? 'Seleccionar' : fechaCorta(_iso(d));

  String _dias(double v) => v.toStringAsFixed(v % 1 == 0 ? 0 : 1);

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;
    if (_tipo == null || _inicio == null || _fin == null) {
      setState(() => _error = 'Complete el tipo y las fechas.');
      return;
    }
    if (_fin!.isBefore(_inicio!)) {
      setState(() => _error = 'La fecha de fin no puede ser anterior al inicio.');
      return;
    }
    if (_porHoras) {
      if (_horaInicio == null || _horaFin == null) {
        setState(() => _error = 'Indique la hora de inicio y de fin del permiso.');
        return;
      }
      if (_horaFin!.hour * 60 + _horaFin!.minute <= _horaInicio!.hour * 60 + _horaInicio!.minute) {
        setState(() => _error = 'La hora de fin debe ser posterior a la de inicio.');
        return;
      }
    }
    if (_esVacaciones && _saldo != null && _diasPedidos! > _saldo!.diasDisponibles) {
      setState(() => _error =
          'Solo tiene ${_dias(_saldo!.diasDisponibles)} días de vacaciones disponibles.');
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await context.read<AuthProvider>().crearPermiso(
            idTipoPermiso: _tipo!.id,
            fechaInicio: _iso(_inicio!),
            fechaFin: _iso(_fin!),
            motivo: _motivoCtrl.text,
            horaInicio: _porHoras ? _hhmm(_horaInicio!) : null,
            horaFin: _porHoras ? _hhmm(_horaFin!) : null,
          );
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Widget _saldoCard() {
    if (_saldoError != null) {
      return Text(_saldoError!, style: const TextStyle(color: AppColors.danger));
    }
    final s = _saldo;
    if (s == null) return const LinearProgressIndicator();
    final pedidos = _diasPedidos;
    final excede = pedidos != null && pedidos > s.diasDisponibles;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: excede ? AppColors.dangerSoft : AppColors.okSoft,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Saldo de vacaciones: ${_dias(s.diasDisponibles)} días disponibles',
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: excede ? AppColors.danger : AppColors.ok,
            ),
          ),
          const SizedBox(height: 4),
          Text('Ganados ${_dias(s.diasGanados)} · Usados ${_dias(s.diasUsados)}'
              '${pedidos != null ? ' · Solicitas $pedidos' : ''}'),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Nuevo permiso')),
      body: _loadingTipos
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  ErrorBanner(_error),
                  DropdownButtonFormField<TipoPermiso>(
                    // ignore: deprecated_member_use
                    value: _tipo,
                    decoration: const InputDecoration(labelText: 'Tipo de permiso'),
                    items: _tipos
                        .map(
                          (t) => DropdownMenuItem(
                            value: t,
                            child: Text(t.nombre.isNotEmpty ? t.nombre : t.codigo),
                          ),
                        )
                        .toList(),
                    onChanged: (v) {
                      setState(() => _tipo = v);
                      _alCambiarTipo();
                    },
                  ),
                  if (_esVacaciones) ...[
                    const SizedBox(height: 12),
                    _saldoCard(),
                  ],
                  if (!_esVacaciones)
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('Permiso por horas'),
                      subtitle: const Text('Solo una parte de la jornada de un día'),
                      value: _porHoras,
                      onChanged: (v) => setState(() {
                        _porHoras = v;
                        if (v && _inicio != null) _fin = _inicio;
                      }),
                    )
                  else
                    const SizedBox(height: 12),
                  if (_porHoras) ...[
                    OutlinedButton.icon(
                      onPressed: () => _pickDate(inicio: true),
                      icon: const Icon(Icons.calendar_today_outlined, size: 18),
                      label: Text('Día: ${_label(_inicio)}'),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: () => _pickHora(inicio: true),
                            icon: const Icon(Icons.schedule, size: 18),
                            label: Text(
                                'Desde: ${_horaInicio == null ? '--:--' : _hhmm(_horaInicio!)}'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: () => _pickHora(inicio: false),
                            icon: const Icon(Icons.schedule, size: 18),
                            label:
                                Text('Hasta: ${_horaFin == null ? '--:--' : _hhmm(_horaFin!)}'),
                          ),
                        ),
                      ],
                    ),
                  ] else
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: () => _pickDate(inicio: true),
                            icon: const Icon(Icons.calendar_today_outlined, size: 18),
                            label: Text('Inicio: ${_label(_inicio)}'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: () => _pickDate(inicio: false),
                            icon: const Icon(Icons.event_outlined, size: 18),
                            label: Text('Fin: ${_label(_fin)}'),
                          ),
                        ),
                      ],
                    ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _motivoCtrl,
                    maxLines: 4,
                    maxLength: 400,
                    decoration: const InputDecoration(
                      labelText: 'Motivo',
                      alignLabelWithHint: true,
                    ),
                    validator: (v) => (v == null || v.trim().length < 5)
                        ? 'El motivo debe tener al menos 5 caracteres'
                        : null,
                  ),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: _saving ? null : _guardar,
                    child: Text(_saving ? 'Enviando…' : 'Enviar a aprobación'),
                  ),
                ],
              ),
            ),
    );
  }
}
