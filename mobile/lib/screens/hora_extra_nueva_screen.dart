import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/common.dart';

class HoraExtraNuevaScreen extends StatefulWidget {
  const HoraExtraNuevaScreen({super.key});

  @override
  State<HoraExtraNuevaScreen> createState() => _HoraExtraNuevaScreenState();
}

class _HoraExtraNuevaScreenState extends State<HoraExtraNuevaScreen> {
  final _formKey = GlobalKey<FormState>();
  final _motivoCtrl = TextEditingController();
  DateTime _fecha = DateTime.now();
  TimeOfDay? _inicio;
  TimeOfDay? _fin;
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    _motivoCtrl.dispose();
    super.dispose();
  }

  /// Horas entre inicio y fin, redondeadas a la media hora (lo que acepta el API).
  double? get _horas {
    if (_inicio == null || _fin == null) return null;
    final min = (_fin!.hour * 60 + _fin!.minute) - (_inicio!.hour * 60 + _inicio!.minute);
    if (min <= 0) return null;
    return (min / 30).round() / 2;
  }

  String _hhmm(TimeOfDay t) =>
      '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';

  String _iso(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Future<void> _pickFecha() async {
    final now = DateTime.now();
    final d = await showDatePicker(
      context: context,
      initialDate: _fecha,
      firstDate: now.subtract(const Duration(days: 60)),
      lastDate: now.add(const Duration(days: 60)),
      locale: const Locale('es'),
    );
    if (d != null) setState(() => _fecha = d);
  }

  Future<void> _pickHora({required bool inicio}) async {
    final t = await showTimePicker(
      context: context,
      initialTime: (inicio ? _inicio : _fin) ??
          (inicio ? const TimeOfDay(hour: 18, minute: 0) : const TimeOfDay(hour: 20, minute: 0)),
    );
    if (t != null) setState(() => inicio ? _inicio = t : _fin = t);
  }

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;
    final horas = _horas;
    if (_inicio == null || _fin == null) {
      setState(() => _error = 'Indique la hora de inicio y de fin.');
      return;
    }
    if (horas == null) {
      setState(() => _error = 'La hora de fin debe ser posterior a la de inicio.');
      return;
    }
    if (horas < 0.5 || horas > 8) {
      setState(() => _error = 'Se pueden solicitar entre 0.5 y 8 horas extras por día.');
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await context.read<AuthProvider>().crearHoraExtra(
            fecha: _iso(_fecha),
            horaInicio: _hhmm(_inicio!),
            horaFin: _hhmm(_fin!),
            cantidadHoras: horas,
            motivo: _motivoCtrl.text,
          );
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final horas = _horas;
    return Scaffold(
      appBar: AppBar(title: const Text('Registrar horas extras')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            ErrorBanner(_error),
            OutlinedButton.icon(
              onPressed: _pickFecha,
              icon: const Icon(Icons.calendar_today_outlined, size: 18),
              label: Text('Fecha: ${fechaCorta(_iso(_fecha))}'),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _pickHora(inicio: true),
                    icon: const Icon(Icons.login, size: 18),
                    label: Text('Desde: ${_inicio == null ? '--:--' : _hhmm(_inicio!)}'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _pickHora(inicio: false),
                    icon: const Icon(Icons.logout, size: 18),
                    label: Text('Hasta: ${_fin == null ? '--:--' : _hhmm(_fin!)}'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.infoSoft,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  const Icon(Icons.timer_outlined, color: AppColors.info),
                  const SizedBox(width: 8),
                  Text(
                    horas == null
                        ? 'Elija el horario para calcular las horas.'
                        : 'Total a solicitar: ${horas.toStringAsFixed(horas % 1 == 0 ? 0 : 1)} h',
                    style: const TextStyle(color: AppColors.info, fontWeight: FontWeight.w600),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _motivoCtrl,
              maxLines: 4,
              maxLength: 400,
              decoration: const InputDecoration(
                labelText: 'Motivo',
                hintText: 'Ej.: Cierre mensual del cliente XYZ',
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
