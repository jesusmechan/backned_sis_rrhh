import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import 'login_screen.dart';
import 'shell_screen.dart';

/// Escucha la sesión: login ↔ home sin quedarse atrapado tras autenticarse.
class AuthGate extends StatelessWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    if (auth.isAuth) return const ShellHost();
    return const LoginScreen();
  }
}
