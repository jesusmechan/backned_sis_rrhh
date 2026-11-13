import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../widgets/droplet_nav_bar.dart';
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
  int _index = ShellTabs.inicio;
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
      SolicitudesScreen(active: _index == ShellTabs.solicitudes),
      MarcarScreen(active: _index == ShellTabs.marcar),
      HomeScreen(
        onOpenTab: _goTo,
        active: _index == ShellTabs.inicio,
        onChanged: _refreshUnread,
      ),
      NotificacionesScreen(
        active: _index == ShellTabs.avisos,
        onChanged: _refreshUnread,
      ),
      const PerfilScreen(),
    ];

    return Scaffold(
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: DropletNavBar(
        index: _index,
        onTap: _goTo,
        items: [
          const DropletNavItem(
            icon: Icons.description_outlined,
            activeIcon: Icons.description_rounded,
            label: 'Solicitudes',
          ),
          const DropletNavItem(
            icon: Icons.fingerprint,
            activeIcon: Icons.fingerprint,
            label: 'Marcar',
          ),
          const DropletNavItem(
            icon: Icons.home_outlined,
            activeIcon: Icons.home_rounded,
            label: 'Inicio',
          ),
          DropletNavItem(
            icon: Icons.notifications_none_rounded,
            activeIcon: Icons.notifications_rounded,
            label: 'Avisos',
            showDot: _unread > 0,
          ),
          const DropletNavItem(
            icon: Icons.person_outline_rounded,
            activeIcon: Icons.person_rounded,
            label: 'Perfil',
          ),
        ],
      ),
    );
  }
}
