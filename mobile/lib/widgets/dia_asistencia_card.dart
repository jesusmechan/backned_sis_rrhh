import 'package:flutter/material.dart';

import '../theme/app_theme.dart';
import '../utils/asistencia_helpers.dart';

class DiaAsistenciaCard extends StatelessWidget {
  const DiaAsistenciaCard({
    super.key,
    required this.day,
    this.titulo,
    this.enCursoHoy = false,
    this.expandible = false,
    this.expanded = false,
    this.onTap,
    this.compact = false,
  });

  final DiaAsistencia day;
  final String? titulo;
  final bool enCursoHoy;
  final bool expandible;
  final bool expanded;
  final VoidCallback? onTap;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final tituloText = titulo ?? fmtDayShort(day.fecha);
    final entrada = fmtTimeMarcacion(day.entrada?.fechaHora, withSeconds: true);
    final salida = fmtTimeMarcacion(day.salida?.fechaHora, withSeconds: true);
    final duracion = duracionDia(day, enCursoHoy: enCursoHoy);
    final resumen = resumenEstadoDia(day);
    final mostrarDetalle = !expandible || expanded;

    final child = Padding(
      padding: EdgeInsets.all(compact ? 14 : 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  tituloText,
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: compact ? 14 : 15,
                  ),
                ),
              ),
              if (expandible)
                AnimatedRotation(
                  turns: expanded ? 0.5 : 0,
                  duration: const Duration(milliseconds: 200),
                  child: Icon(
                    Icons.expand_more,
                    color: AppColors.muted,
                    size: compact ? 22 : 24,
                  ),
                ),
            ],
          ),
          if (!mostrarDetalle) ...[
            const SizedBox(height: 8),
            Text(
              resumen,
              style: TextStyle(
                fontSize: compact ? 12 : 13,
                color: day.tieneAlguna ? AppColors.navy : AppColors.muted,
                fontWeight: day.tieneAlguna ? FontWeight.w600 : FontWeight.normal,
              ),
            ),
            if (day.jornadaCompleta)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  'Tiempo: $duracion',
                  style: const TextStyle(fontSize: 12, color: AppColors.muted),
                ),
              ),
            if (expandible)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(
                  'Toca para ver entrada y salida',
                  style: TextStyle(
                    fontSize: 11,
                    color: AppColors.muted.withValues(alpha: 0.9),
                  ),
                ),
              ),
          ],
          AnimatedCrossFade(
            firstCurve: Curves.easeOut,
            secondCurve: Curves.easeOut,
            sizeCurve: Curves.easeOut,
            duration: const Duration(milliseconds: 220),
            crossFadeState:
                mostrarDetalle ? CrossFadeState.showFirst : CrossFadeState.showSecond,
            firstChild: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(height: compact ? 10 : 14),
                Row(
                  children: [
                    Expanded(
                      child: _HoraMarcacionTile(
                        icon: Icons.login,
                        label: 'Entrada',
                        time: entrada,
                        done: day.entrada != null,
                        compact: compact,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _HoraMarcacionTile(
                        icon: Icons.logout,
                        label: 'Salida',
                        time: salida,
                        done: day.salida != null,
                        compact: compact,
                      ),
                    ),
                  ],
                ),
                SizedBox(height: compact ? 10 : 12),
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
            secondChild: const SizedBox.shrink(),
          ),
        ],
      ),
    );

    return Card(
      clipBehavior: Clip.antiAlias,
      child: expandible
          ? InkWell(
              onTap: onTap,
              child: child,
            )
          : child,
    );
  }
}

class _HoraMarcacionTile extends StatelessWidget {
  const _HoraMarcacionTile({
    required this.icon,
    required this.label,
    required this.time,
    required this.done,
    this.compact = false,
  });

  final IconData icon;
  final String label;
  final String time;
  final bool done;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(compact ? 10 : 12),
      decoration: BoxDecoration(
        color: done ? AppColors.okSoft : AppColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.line),
      ),
      child: Column(
        children: [
          Icon(icon, size: compact ? 16 : 18, color: done ? AppColors.ok : AppColors.info),
          SizedBox(height: compact ? 4 : 6),
          Text(label, style: const TextStyle(color: AppColors.muted, fontSize: 12)),
          SizedBox(height: compact ? 2 : 4),
          Text(
            time,
            style: TextStyle(
              fontSize: compact ? 16 : 18,
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
