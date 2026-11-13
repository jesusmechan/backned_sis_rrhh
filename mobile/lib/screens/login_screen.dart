import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/api_config.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/common.dart';
import '../widgets/server_config.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _userCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  final _userFocus = FocusNode();
  final _passFocus = FocusNode();
  final _formKey = GlobalKey<FormState>();
  bool _showPass = false;
  String? _error;
  String _apiUrl = ApiConfig.baseUrl;
  bool _probing = false;
  String? _probeOk;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadApiUrl());
  }

  Future<void> _loadApiUrl() async {
    await context.read<AuthProvider>().api.ensureReady();
    if (!mounted) return;
    setState(() => _apiUrl = context.read<AuthProvider>().api.baseUrlLabel);
  }

  Future<void> _editServer() async {
    final ok = await showServerConfigDialog(context);
    if (!ok || !mounted) return;
    setState(() {
      _apiUrl = context.read<AuthProvider>().api.baseUrlLabel;
      _probeOk = null;
    });
  }

  Future<void> _testServer() async {
    setState(() {
      _probing = true;
      _probeOk = null;
      _error = null;
    });
    final api = context.read<AuthProvider>().api;
    final msg = await probeServerMessage(api);
    if (!mounted) return;
    setState(() {
      _probing = false;
      _apiUrl = api.baseUrlLabel;
      if (msg.startsWith('Conexión correcta')) {
        _probeOk = msg;
      } else {
        _error = msg;
      }
    });
  }

  Future<void> _autoServer() async {
    await ApiConfig.clearManualBaseUrl();
    if (!mounted) return;
    final api = context.read<AuthProvider>().api;
    api.resetBaseUrl();
    await api.ensureReady();
    if (!mounted) return;
    setState(() {
      _apiUrl = api.baseUrlLabel;
      _probeOk = null;
      _error = null;
    });
    await _testServer();
  }

  /// USB + `adb reverse tcp:8080 tcp:8080` → el celular habla con la Mac en :8080.
  Future<void> _useUsbLocalhost() async {
    await ApiConfig.saveCustomBaseUrl('http://127.0.0.1:8080');
    if (!mounted) return;
    final api = context.read<AuthProvider>().api;
    api.resetBaseUrl();
    await api.ensureReady();
    if (!mounted) return;
    setState(() {
      _apiUrl = api.baseUrlLabel;
      _probeOk = null;
      _error = null;
    });
    await _testServer();
  }

  @override
  void dispose() {
    _userCtrl.dispose();
    _passCtrl.dispose();
    _userFocus.dispose();
    _passFocus.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _error = null);
    FocusScope.of(context).unfocus();
    final auth = context.read<AuthProvider>();
    try {
      await auth.login(_userCtrl.text, _passCtrl.text);
    } catch (e) {
      if (mounted) setState(() => _error = auth.error ?? e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final busy = auth.busy;
    return Scaffold(
      resizeToAvoidBottomInset: true,
      body: Container(
        width: double.infinity,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFFCBD5E1)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 420),
                child: Material(
                  color: Colors.white,
                  elevation: 0,
                  borderRadius: BorderRadius.circular(18),
                  child: Container(
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: AppColors.line),
                    ),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'CONSULTORA CONTABLE ANDINA',
                            style: TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.w700,
                              letterSpacing: 1.4,
                              color: AppColors.muted,
                            ),
                          ),
                          const SizedBox(height: 6),
                          const Text(
                            'Ingreso móvil',
                            style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800),
                          ),
                          const SizedBox(height: 4),
                          const Text(
                            'Gestión de RR. HH. — marcar, permisos y avisos.',
                            style: TextStyle(color: AppColors.muted),
                          ),
                          const SizedBox(height: 20),
                          ErrorBanner(_error ?? auth.error),
                          TextFormField(
                            controller: _userCtrl,
                            focusNode: _userFocus,
                            enabled: !busy,
                            autofocus: true,
                            keyboardType: TextInputType.text,
                            textInputAction: TextInputAction.next,
                            autocorrect: false,
                            enableSuggestions: false,
                            decoration: const InputDecoration(
                              labelText: 'Usuario',
                              prefixIcon: Icon(Icons.person_outline),
                            ),
                            onFieldSubmitted: (_) => _passFocus.requestFocus(),
                            validator: (v) =>
                                (v == null || v.trim().isEmpty) ? 'Indique el usuario' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _passCtrl,
                            focusNode: _passFocus,
                            enabled: !busy,
                            obscureText: !_showPass,
                            keyboardType: TextInputType.text,
                            textInputAction: TextInputAction.done,
                            autocorrect: false,
                            enableSuggestions: false,
                            onFieldSubmitted: (_) => _submit(),
                            decoration: InputDecoration(
                              labelText: 'Contraseña',
                              prefixIcon: const Icon(Icons.lock_outline),
                              suffixIcon: IconButton(
                                onPressed: () => setState(() => _showPass = !_showPass),
                                icon: Icon(_showPass ? Icons.visibility_off : Icons.visibility),
                              ),
                            ),
                            validator: (v) =>
                                (v == null || v.isEmpty) ? 'Indique la contraseña' : null,
                          ),
                          const SizedBox(height: 20),
                          FilledButton(
                            onPressed: busy ? null : _submit,
                            child: Text(busy ? 'Ingresando…' : 'Ingresar'),
                          ),
                          const SizedBox(height: 16),
                          Row(
                            children: [
                              const Icon(Icons.dns_outlined, size: 18, color: AppColors.muted),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  'Servidor: $_apiUrl',
                                  style: const TextStyle(fontSize: 12, color: AppColors.muted),
                                ),
                              ),
                              TextButton(
                                onPressed: busy ? null : _editServer,
                                child: const Text('Cambiar'),
                              ),
                              TextButton(
                                onPressed: busy ? null : _useUsbLocalhost,
                                child: const Text('USB'),
                              ),
                              TextButton(
                                onPressed: busy ? null : _autoServer,
                                child: const Text('Auto'),
                              ),
                            ],
                          ),
                          Align(
                            alignment: Alignment.centerLeft,
                            child: TextButton.icon(
                              onPressed: (_probing || busy) ? null : _testServer,
                              icon: _probing
                                  ? const SizedBox(
                                      width: 16,
                                      height: 16,
                                      child: CircularProgressIndicator(strokeWidth: 2),
                                    )
                                  : const Icon(Icons.wifi_tethering, size: 18),
                              label: Text(_probing ? 'Probando…' : 'Probar conexión'),
                            ),
                          ),
                          if (_probeOk != null)
                            Padding(
                              padding: const EdgeInsets.only(bottom: 8),
                              child: Text(
                                _probeOk!,
                                style: const TextStyle(fontSize: 12, color: AppColors.ok),
                              ),
                            ),
                          if (kDebugMode) ...[
                            const SizedBox(height: 6),
                            const Text(
                              'Usuario de prueba: juan.espinoza / Andina2026',
                              style: TextStyle(fontSize: 12, color: AppColors.muted),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
