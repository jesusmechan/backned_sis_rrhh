import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/solicitud.dart';
import '../theme/app_theme.dart';

/// `EN_CURSO` → `En curso`.
String estadoLabel(String estado) {
  final s = estado.replaceAll('_', ' ').trim().toLowerCase();
  if (s.isEmpty) return s;
  return s[0].toUpperCase() + s.substring(1);
}

String fechaCorta(String iso) {
  final d = DateTime.tryParse(iso);
  return d == null ? iso : DateFormat('dd/MM/yyyy').format(d);
}

String fechaHora(DateTime? d) =>
    d == null ? '—' : DateFormat('dd/MM/yyyy HH:mm').format(d.toLocal());

void showMensaje(BuildContext context, String texto, {bool error = false}) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(
      content: Text(texto),
      backgroundColor: error ? AppColors.danger : AppColors.navy,
      behavior: SnackBarBehavior.floating,
    ));
}

Future<bool> confirmar(
  BuildContext context, {
  required String titulo,
  required String mensaje,
  String aceptar = 'Confirmar',
  bool peligro = false,
}) async {
  final ok = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(titulo),
      content: Text(mensaje),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Volver')),
        FilledButton(
          style: peligro ? FilledButton.styleFrom(backgroundColor: AppColors.danger) : null,
          onPressed: () => Navigator.pop(ctx, true),
          child: Text(aceptar),
        ),
      ],
    ),
  );
  return ok ?? false;
}

class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.label, this.tone = ChipTone.neutral});

  final String label;
  final ChipTone tone;

  factory StatusChip.fromEstado(String estado) {
    final e = estado.toUpperCase();
    final label = estadoLabel(estado);
    if (e.contains('APROB') || e == 'ACTIVO') {
      return StatusChip(label: label, tone: ChipTone.ok);
    }
    if (e.contains('RECHAZ') || e.contains('CANCEL')) {
      return StatusChip(label: label, tone: ChipTone.danger);
    }
    if (e.contains('PEND') || e.contains('CURSO')) {
      return StatusChip(label: label, tone: ChipTone.warn);
    }
    return StatusChip(label: label);
  }

  @override
  Widget build(BuildContext context) {
    final (bg, fg) = switch (tone) {
      ChipTone.ok => (AppColors.okSoft, AppColors.ok),
      ChipTone.warn => (AppColors.warnSoft, AppColors.warn),
      ChipTone.danger => (AppColors.dangerSoft, AppColors.danger),
      ChipTone.info => (AppColors.infoSoft, AppColors.info),
      ChipTone.neutral => (AppColors.surface, AppColors.muted),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(color: fg, fontSize: 12, fontWeight: FontWeight.w700),
      ),
    );
  }
}

enum ChipTone { ok, warn, danger, info, neutral }

class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
  });

  final IconData icon;
  final String title;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 42, color: AppColors.muted),
            const SizedBox(height: 12),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
            ),
            if (subtitle != null) ...[
              const SizedBox(height: 6),
              Text(
                subtitle!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppColors.muted),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class ErrorBanner extends StatelessWidget {
  const ErrorBanner(this.message, {super.key, this.onRetry});

  final String? message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    if (message == null || message!.isEmpty) return const SizedBox.shrink();
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.dangerSoft,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFFECACA)),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: AppColors.danger, size: 20),
          const SizedBox(width: 8),
          Expanded(child: Text(message!, style: const TextStyle(color: AppColors.danger))),
          if (onRetry != null)
            TextButton(onPressed: onRetry, child: const Text('Reintentar')),
        ],
      ),
    );
  }
}

class SectionCard extends StatelessWidget {
  const SectionCard({super.key, required this.title, required this.child, this.trailing});

  final String title;
  final Widget child;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(title,
                      style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15)),
                ),
                if (trailing != null) trailing!,
              ],
            ),
            const SizedBox(height: 12),
            child,
          ],
        ),
      ),
    );
  }
}

class InfoRow extends StatelessWidget {
  const InfoRow(this.label, this.value, {super.key});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(label, style: const TextStyle(color: AppColors.muted)),
          ),
          Expanded(
            child: Text(value, style: const TextStyle(fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }
}

/// Pasos del circuito de aprobación en forma de línea de tiempo.
class CircuitoAprobacion extends StatelessWidget {
  const CircuitoAprobacion(this.pasos, {super.key});

  final List<PasoAprobacion> pasos;

  @override
  Widget build(BuildContext context) {
    if (pasos.isEmpty) {
      return const Text('Sin pasos de aprobación registrados.',
          style: TextStyle(color: AppColors.muted));
    }
    return Column(
      children: [
        for (var i = 0; i < pasos.length; i++)
          _PasoTile(paso: pasos[i], ultimo: i == pasos.length - 1),
      ],
    );
  }
}

class _PasoTile extends StatelessWidget {
  const _PasoTile({required this.paso, required this.ultimo});

  final PasoAprobacion paso;
  final bool ultimo;

  @override
  Widget build(BuildContext context) {
    final e = paso.estado.toUpperCase();
    final (color, icon) = e.contains('APROB')
        ? (AppColors.ok, Icons.check_circle)
        : e.contains('RECHAZ')
            ? (AppColors.danger, Icons.cancel)
            : e.contains('PEND')
                ? (AppColors.warn, Icons.schedule)
                : (AppColors.muted, Icons.radio_button_unchecked);
    final responsable = paso.usuarioDecision ?? paso.usuarioAsignado ?? paso.rol;
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Column(
            children: [
              Icon(icon, color: color, size: 22),
              if (!ultimo)
                Expanded(child: Container(width: 2, color: AppColors.line)),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Padding(
              padding: EdgeInsets.only(bottom: ultimo ? 0 : 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text('${paso.numeroPaso}. ${paso.nombrePaso}',
                            style: const TextStyle(fontWeight: FontWeight.w700)),
                      ),
                      StatusChip.fromEstado(paso.estado),
                    ],
                  ),
                  if (responsable != null && responsable.isNotEmpty)
                    Text(responsable, style: const TextStyle(color: AppColors.muted)),
                  if (paso.fechaDecision != null)
                    Text(fechaHora(paso.fechaDecision),
                        style: const TextStyle(color: AppColors.muted, fontSize: 12)),
                  if (paso.comentario != null && paso.comentario!.isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Text('“${paso.comentario}”',
                          style: const TextStyle(fontStyle: FontStyle.italic)),
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

class HistorialLista extends StatelessWidget {
  const HistorialLista(this.items, {super.key});

  final List<HistorialSolicitud> items;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const Text('Sin movimientos.', style: TextStyle(color: AppColors.muted));
    }
    return Column(
      children: [
        for (final h in items)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(
                  padding: EdgeInsets.only(top: 6),
                  child: Icon(Icons.circle, size: 8, color: AppColors.navy),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(estadoLabel(h.accion),
                          style: const TextStyle(fontWeight: FontWeight.w700)),
                      Text(
                        [fechaHora(h.fechaHora), if (h.usuario != null) h.usuario!].join(' · '),
                        style: const TextStyle(color: AppColors.muted, fontSize: 12),
                      ),
                      if (h.comentario != null && h.comentario!.isNotEmpty)
                        Text(h.comentario!),
                    ],
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
