import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../theme/app_theme.dart';

class ThemeProvider extends ChangeNotifier {
  static const _prefsKey = 'andina.theme.dark';

  bool _isDark = false;
  bool _ready = false;
  Timer? _uiTimer;

  bool get isDark => _isDark;
  bool get ready => _ready;
  ThemeMode get themeMode => _isDark ? ThemeMode.dark : ThemeMode.light;
  Brightness get brightness => _isDark ? Brightness.dark : Brightness.light;

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    _isDark = prefs.getBool(_prefsKey) ?? false;
    _ready = true;
    SystemChrome.setSystemUIOverlayStyle(systemOverlayFor(brightness));
    notifyListeners();
  }

  Future<void> setDark(bool value) async {
    if (_isDark == value) return;
    _isDark = value;
    notifyListeners();

    // Status bar al final corto de la animación (sin flash).
    _uiTimer?.cancel();
    _uiTimer = Timer(kThemeAnimDuration, () {
      SystemChrome.setSystemUIOverlayStyle(systemOverlayFor(brightness));
    });

    unawaited(_persist(value));
  }

  Future<void> toggle() async {
    HapticFeedback.selectionClick();
    await setDark(!_isDark);
  }

  Future<void> _persist(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_prefsKey, value);
  }

  @override
  void dispose() {
    _uiTimer?.cancel();
    super.dispose();
  }
}
