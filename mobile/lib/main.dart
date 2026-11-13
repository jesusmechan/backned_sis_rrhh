import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

import 'providers/auth_provider.dart';
import 'providers/theme_provider.dart';
import 'screens/splash_screen.dart';
import 'services/api_client.dart';
import 'services/marcacion_reminder_service.dart';
import 'services/server_link.dart';
import 'theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('es');
  // Precalienta temas (GoogleFonts) antes del primer frame.
  final _ = (appLightTheme, appDarkTheme);
  final theme = ThemeProvider();
  await theme.load();
  // Recordatorios locales de marcación (no bloquea el arranque si falla).
  try {
    await MarcacionReminderService.instance.init();
  } catch (_) {}
  runApp(AndinaApp(themeProvider: theme));
}

class AndinaApp extends StatelessWidget {
  const AndinaApp({super.key, required this.themeProvider});

  final ThemeProvider themeProvider;

  @override
  Widget build(BuildContext context) {
    final api = ApiClient();
    return MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: themeProvider),
        ChangeNotifierProvider(create: (_) => AuthProvider(api)..bootstrap()),
      ],
      child: ServerLink(
        api: api,
        child: ListenableBuilder(
          listenable: themeProvider,
          builder: (context, _) {
            return MaterialApp(
              title: 'Andina RR. HH.',
              debugShowCheckedModeBanner: false,
              theme: appLightTheme,
              darkTheme: appDarkTheme,
              themeMode: themeProvider.themeMode,
              // Morph rápido del ThemeData (ColorScheme + ThemeExtension).
              themeAnimationDuration: kThemeAnimDuration,
              themeAnimationCurve: kThemeAnimCurve,
              locale: const Locale('es'),
              supportedLocales: const [Locale('es'), Locale('en')],
              localizationsDelegates: const [
                GlobalMaterialLocalizations.delegate,
                GlobalWidgetsLocalizations.delegate,
                GlobalCupertinoLocalizations.delegate,
              ],
              builder: (context, child) {
                // Suaviza el cambio con un leve fade del árbol visible.
                return _ThemeSoftFade(
                  isDark: themeProvider.isDark,
                  child: child ?? const SizedBox.shrink(),
                );
              },
              home: const SplashScreen(),
            );
          },
        ),
      ),
    );
  }
}

/// Fade corto al cambiar tema: evita el “corte” visual sin añadir lag.
class _ThemeSoftFade extends StatefulWidget {
  const _ThemeSoftFade({required this.isDark, required this.child});

  final bool isDark;
  final Widget child;

  @override
  State<_ThemeSoftFade> createState() => _ThemeSoftFadeState();
}

class _ThemeSoftFadeState extends State<_ThemeSoftFade>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;
  late final Animation<double> _opacity;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 160),
      value: 1,
    );
    _opacity = CurvedAnimation(parent: _ctrl, curve: Curves.easeOut);
  }

  @override
  void didUpdateWidget(covariant _ThemeSoftFade oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.isDark != widget.isDark) {
      _ctrl
        ..value = 0.82
        ..forward();
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(opacity: _opacity, child: widget.child);
  }
}
