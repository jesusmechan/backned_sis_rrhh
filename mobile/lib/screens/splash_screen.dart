import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'auth_gate.dart';

/// Splash con logo grande fijo + loader. Sin animación de escala.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  static const _minVisible = Duration(milliseconds: 2000);
  static const _logoAsset = 'assets/branding/app_icon_1024.png';

  bool _minElapsed = false;
  bool _navigated = false;
  bool _imageReady = false;

  @override
  void initState() {
    super.initState();
    unawaited(_waitMinThenGo());
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Precarga para que el primer frame ya tenga el logo grande.
    precacheImage(const AssetImage(_logoAsset), context).then((_) {
      if (mounted) setState(() => _imageReady = true);
    });
  }

  Future<void> _waitMinThenGo() async {
    await Future<void>.delayed(_minVisible);
    if (!mounted) return;
    _minElapsed = true;
    _tryNavigate();
  }

  void _tryNavigate() {
    if (_navigated || !_minElapsed || !mounted) return;
    if (context.read<AuthProvider>().booting) return;
    _navigated = true;
    Navigator.of(context).pushReplacement(
      PageRouteBuilder<void>(
        transitionDuration: const Duration(milliseconds: 350),
        pageBuilder: (_, _, _) => const AuthGate(),
        transitionsBuilder: (_, anim, _, child) {
          return FadeTransition(opacity: anim, child: child);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    context.watch<AuthProvider>();
    WidgetsBinding.instance.addPostFrameCallback((_) => _tryNavigate());

    return Scaffold(
      backgroundColor: AppColors.navy,
      body: SafeArea(
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Tamaño fijo grande; sin Transform.scale ni fade del logo.
              SizedBox(
                width: 220,
                height: 220,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(44),
                  child: Image.asset(
                    _logoAsset,
                    fit: BoxFit.cover,
                    gaplessPlayback: true,
                    filterQuality: FilterQuality.medium,
                    // Evita frame vacío: color navy mientras decodifica.
                    frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
                      if (wasSynchronouslyLoaded || frame != null || _imageReady) {
                        return child;
                      }
                      return const ColoredBox(color: AppColors.navy);
                    },
                  ),
                ),
              ),
              const SizedBox(height: 28),
              const Text(
                'Andina RR. HH.',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 0.3,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Asistencia · Permisos · Notificaciones',
                style: TextStyle(
                  color: Color(0xFF94A3B8),
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 40),
              const SizedBox(
                width: 26,
                height: 26,
                child: CircularProgressIndicator(
                  strokeWidth: 2.4,
                  color: Color(0xFF94A3B8),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
