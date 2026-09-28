import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'home_screen.dart';
import 'marcar_screen.dart';
import 'notificaciones_screen.dart';
import 'perfil_screen.dart';
import 'solicitudes_screen.dart';

class ShellHost extends StatefulWidget {
  const ShellHost({super.key});

  @override
  State<ShellHost> createState() => _ShellHostState();
}

class _ShellHostState extends State<ShellHost> with WidgetsBindingObserver {
  static const _tabAvisos = 3;

  int _index = 0;
  int _unread = 0;
  Timer? _unreadTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((_) => _refreshUnread());
    _startUnreadTimer();
  }

  void _startUnreadTimer() {
    _unreadTimer?.cancel();
    _unreadTimer = Timer.periodic(const Duration(seconds: 20), (_) => _refreshUnread());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _unreadTimer?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _refreshUnread();
      _startUnreadTimer();
    } else if (state == AppLifecycleState.paused) {
      _unreadTimer?.cancel();
    }
  }

  Future<void> _refreshUnread() async {
    try {
      final n = await context.read<AuthProvider>().contarNoLeidas();
      if (mounted) setState(() => _unread = n);
    } catch (_) {}
  }

  void _goTo(int i) {
    setState(() => _index = i);
    _refreshUnread();
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      HomeScreen(onOpenTab: _goTo, active: _index == 0, onChanged: _refreshUnread),
      MarcarScreen(active: _index == 1),
      SolicitudesScreen(active: _index == 2),
      NotificacionesScreen(active: _index == _tabAvisos, onChanged: _refreshUnread),
      const PerfilScreen(),
    ];

    return Scaffold(
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: _goTo,
        indicatorColor: AppColors.infoSoft,
        destinations: [
          const NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Inicio',
          ),
          const NavigationDestination(
            icon: Icon(Icons.fingerprint_outlined),
            selectedIcon: Icon(Icons.fingerprint),
            label: 'Marcar',
          ),
          const NavigationDestination(
            icon: Icon(Icons.assignment_outlined),
            selectedIcon: Icon(Icons.assignment),
            label: 'Solicitudes',
          ),
          NavigationDestination(
            icon: Badge(
              isLabelVisible: _unread > 0,
              label: Text('$_unread'),
              child: const Icon(Icons.notifications_outlined),
            ),
            selectedIcon: Badge(
              isLabelVisible: _unread > 0,
              label: Text('$_unread'),
              child: const Icon(Icons.notifications),
            ),
            label: 'Avisos',
          ),
          const NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Perfil',
          ),
        ],
      ),
    );
  }
}
