import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/widgets.dart';

import '../config/api_config.dart';
import 'api_client.dart';

/// Si cambia la red (Wi‑Fi, datos, etc.), vuelve a detectar el servidor accesible.
class ServerLink extends StatefulWidget {
  const ServerLink({super.key, required this.api, required this.child});

  final ApiClient api;
  final Widget child;

  @override
  State<ServerLink> createState() => _ServerLinkState();
}

class _ServerLinkState extends State<ServerLink> with WidgetsBindingObserver {
  StreamSubscription<List<ConnectivityResult>>? _connectivitySub;
  String? _last;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _connectivitySub = Connectivity().onConnectivityChanged.listen(_onConnectivity);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _connectivitySub?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _redetect();
    }
  }

  void _onConnectivity(List<ConnectivityResult> results) {
    final signature = results.map((e) => e.name).join(',');
    final connected = results.any((r) => r != ConnectivityResult.none);
    if (_last != null && _last != signature && connected) {
      _redetect();
    }
    _last = signature;
  }

  Future<void> _redetect() async {
    widget.api.resetBaseUrl();
    await ApiConfig.redetectBaseUrl();
    await widget.api.ensureReady();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
