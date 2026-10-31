import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

import 'providers/auth_provider.dart';
import 'screens/splash_screen.dart';
import 'services/api_client.dart';
import 'services/server_link.dart';
import 'theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('es');
  runApp(const AndinaApp());
}

class AndinaApp extends StatelessWidget {
  const AndinaApp({super.key});

  @override
  Widget build(BuildContext context) {
    final api = ApiClient();
    return ChangeNotifierProvider(
      create: (_) => AuthProvider(api)..bootstrap(),
      child: ServerLink(
        api: api,
        child: MaterialApp(
          title: 'Andina RR. HH.',
          debugShowCheckedModeBanner: false,
          theme: buildAppTheme(),
          locale: const Locale('es'),
          supportedLocales: const [Locale('es'), Locale('en')],
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          home: const SplashScreen(),
        ),
      ),
    );
  }
}
