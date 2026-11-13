import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/theme_provider.dart';
import '../theme/app_theme.dart';

/// Switch claro/oscuro con sol y luna.
class ThemeSunMoonSwitch extends StatelessWidget {
  const ThemeSunMoonSwitch({super.key});

  @override
  Widget build(BuildContext context) {
    final isDark = context.select<ThemeProvider, bool>((t) => t.isDark);

    return Semantics(
      label: isDark ? 'Tema oscuro' : 'Tema claro',
      toggled: isDark,
      button: true,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => context.read<ThemeProvider>().toggle(),
          borderRadius: BorderRadius.circular(20),
          child: AnimatedContainer(
            duration: kThemeAnimDuration,
            curve: kThemeAnimCurve,
            width: 68,
            height: 36,
            padding: const EdgeInsets.all(3),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(20),
              gradient: LinearGradient(
                colors: isDark
                    ? const [Color(0xFF0F172A), Color(0xFF1E3A5F)]
                    : const [Color(0xFFFEF3C7), Color(0xFFFDE68A)],
              ),
              border: Border.all(
                color: isDark ? const Color(0xFF334155) : const Color(0xFFF59E0B),
                width: 1.2,
              ),
            ),
            child: AnimatedAlign(
              duration: kThemeAnimDuration,
              curve: kThemeAnimCurve,
              alignment: isDark ? Alignment.centerRight : Alignment.centerLeft,
              child: Container(
                width: 30,
                height: 30,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white,
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.16),
                      blurRadius: 5,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: Icon(
                  isDark ? Icons.dark_mode_rounded : Icons.wb_sunny_rounded,
                  size: 17,
                  color: isDark
                      ? const Color(0xFF1E3A5F)
                      : const Color(0xFFF59E0B),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
