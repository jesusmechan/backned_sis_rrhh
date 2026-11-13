import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';

class AppColors {
  static const navy = Color(0xFF15233D);
  static const navyHover = Color(0xFF1E3358);
  static const surface = Color(0xFFF5F7FB);
  static const line = Color(0xFFE2E8F0);
  static const muted = Color(0xFF64748B);
  static const danger = Color(0xFFDC2626);
  static const dangerSoft = Color(0xFFFEF2F2);
  static const ok = Color(0xFF047857);
  static const okSoft = Color(0xFFECFDF5);
  static const warn = Color(0xFFB45309);
  static const warnSoft = Color(0xFFFFFBEB);
  static const info = Color(0xFF0369A1);
  static const infoSoft = Color(0xFFF0F9FF);

  static AndinaColors tokens(BuildContext context) =>
      Theme.of(context).extension<AndinaColors>() ?? AndinaColors.light;

  static Color mutedOf(BuildContext context) => tokens(context).muted;
  static Color lineOf(BuildContext context) => tokens(context).line;
  static Color cardOf(BuildContext context) => tokens(context).card;
  static Color inkOf(BuildContext context) => tokens(context).ink;
  static Color softInfoOf(BuildContext context) => tokens(context).softInfo;
  static Color softOkOf(BuildContext context) => tokens(context).softOk;
  static Color softWarnOf(BuildContext context) => tokens(context).softWarn;
  static Color softDangerOf(BuildContext context) => tokens(context).softDanger;
  static Color softNeutralOf(BuildContext context) => tokens(context).softNeutral;
}

/// Colores de marca que interpolan durante el cambio de tema (sin “salto”).
@immutable
class AndinaColors extends ThemeExtension<AndinaColors> {
  const AndinaColors({
    required this.ink,
    required this.muted,
    required this.line,
    required this.card,
    required this.surface,
    required this.softInfo,
    required this.softOk,
    required this.softWarn,
    required this.softDanger,
    required this.softNeutral,
    required this.okFg,
    required this.warnFg,
    required this.dangerFg,
    required this.infoFg,
    required this.navBar,
    required this.navBarBorder,
    required this.brand,
    required this.iconTint,
  });

  final Color ink;
  final Color muted;
  final Color line;
  final Color card;
  final Color surface;
  final Color softInfo;
  final Color softOk;
  final Color softWarn;
  final Color softDanger;
  final Color softNeutral;
  final Color okFg;
  final Color warnFg;
  final Color dangerFg;
  final Color infoFg;
  final Color navBar;
  final Color navBarBorder;
  final Color brand;
  final Color iconTint;

  static const light = AndinaColors(
    ink: AppColors.navy,
    muted: AppColors.muted,
    line: AppColors.line,
    card: Colors.white,
    surface: AppColors.surface,
    softInfo: AppColors.infoSoft,
    softOk: AppColors.okSoft,
    softWarn: AppColors.warnSoft,
    softDanger: AppColors.dangerSoft,
    softNeutral: AppColors.surface,
    okFg: AppColors.ok,
    warnFg: AppColors.warn,
    dangerFg: AppColors.danger,
    infoFg: AppColors.info,
    navBar: Color(0xFFF1F5F9),
    navBarBorder: Color(0xFFE2E8F0),
    brand: Color(0xFF1D4ED8),
    iconTint: AppColors.info,
  );

  static const dark = AndinaColors(
    ink: Color(0xFFE2E8F0),
    muted: Color(0xFF94A3B8),
    line: Color(0xFF2A364A),
    card: Color(0xFF152033),
    surface: Color(0xFF0B1220),
    softInfo: Color(0xFF1E3A5F),
    softOk: Color(0xFF064E3B),
    softWarn: Color(0xFF78350F),
    softDanger: Color(0xFF7F1D1D),
    softNeutral: Color(0xFF1A2436),
    okFg: Color(0xFF6EE7B7),
    warnFg: Color(0xFFFBBF24),
    dangerFg: Color(0xFFFCA5A5),
    infoFg: Color(0xFF93C5FD),
    navBar: Color(0xFF152033),
    navBarBorder: Color(0xFF2A364A),
    brand: Color(0xFF93C5FD),
    iconTint: Color(0xFF93C5FD),
  );

  @override
  AndinaColors copyWith({
    Color? ink,
    Color? muted,
    Color? line,
    Color? card,
    Color? surface,
    Color? softInfo,
    Color? softOk,
    Color? softWarn,
    Color? softDanger,
    Color? softNeutral,
    Color? okFg,
    Color? warnFg,
    Color? dangerFg,
    Color? infoFg,
    Color? navBar,
    Color? navBarBorder,
    Color? brand,
    Color? iconTint,
  }) {
    return AndinaColors(
      ink: ink ?? this.ink,
      muted: muted ?? this.muted,
      line: line ?? this.line,
      card: card ?? this.card,
      surface: surface ?? this.surface,
      softInfo: softInfo ?? this.softInfo,
      softOk: softOk ?? this.softOk,
      softWarn: softWarn ?? this.softWarn,
      softDanger: softDanger ?? this.softDanger,
      softNeutral: softNeutral ?? this.softNeutral,
      okFg: okFg ?? this.okFg,
      warnFg: warnFg ?? this.warnFg,
      dangerFg: dangerFg ?? this.dangerFg,
      infoFg: infoFg ?? this.infoFg,
      navBar: navBar ?? this.navBar,
      navBarBorder: navBarBorder ?? this.navBarBorder,
      brand: brand ?? this.brand,
      iconTint: iconTint ?? this.iconTint,
    );
  }

  @override
  AndinaColors lerp(ThemeExtension<AndinaColors>? other, double t) {
    if (other is! AndinaColors) return this;
    return AndinaColors(
      ink: Color.lerp(ink, other.ink, t)!,
      muted: Color.lerp(muted, other.muted, t)!,
      line: Color.lerp(line, other.line, t)!,
      card: Color.lerp(card, other.card, t)!,
      surface: Color.lerp(surface, other.surface, t)!,
      softInfo: Color.lerp(softInfo, other.softInfo, t)!,
      softOk: Color.lerp(softOk, other.softOk, t)!,
      softWarn: Color.lerp(softWarn, other.softWarn, t)!,
      softDanger: Color.lerp(softDanger, other.softDanger, t)!,
      softNeutral: Color.lerp(softNeutral, other.softNeutral, t)!,
      okFg: Color.lerp(okFg, other.okFg, t)!,
      warnFg: Color.lerp(warnFg, other.warnFg, t)!,
      dangerFg: Color.lerp(dangerFg, other.dangerFg, t)!,
      infoFg: Color.lerp(infoFg, other.infoFg, t)!,
      navBar: Color.lerp(navBar, other.navBar, t)!,
      navBarBorder: Color.lerp(navBarBorder, other.navBarBorder, t)!,
      brand: Color.lerp(brand, other.brand, t)!,
      iconTint: Color.lerp(iconTint, other.iconTint, t)!,
    );
  }
}

/// Duración única del morph de tema en toda la app (rápido y fluido).
const kThemeAnimDuration = Duration(milliseconds: 280);
const kThemeAnimCurve = Curves.easeOutCubic;

SystemUiOverlayStyle systemOverlayFor(Brightness brightness) {
  final isDark = brightness == Brightness.dark;
  return SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
    statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
    systemNavigationBarColor:
        isDark ? const Color(0xFF0B1220) : AppColors.surface,
    systemNavigationBarIconBrightness:
        isDark ? Brightness.light : Brightness.dark,
    systemNavigationBarDividerColor: Colors.transparent,
  );
}

/// Temas cacheados: GoogleFonts / ColorScheme.fromSeed no se recalculan al toggle.
final ThemeData appLightTheme = _buildTheme(Brightness.light);
final ThemeData appDarkTheme = _buildTheme(Brightness.dark);

ThemeData buildAppTheme() => appLightTheme;
ThemeData buildDarkAppTheme() => appDarkTheme;

ThemeData _buildTheme(Brightness brightness) {
  final isDark = brightness == Brightness.dark;
  final tokens = isDark ? AndinaColors.dark : AndinaColors.light;
  final surface = tokens.surface;
  final card = tokens.card;
  final ink = tokens.ink;
  final line = tokens.line;
  final muted = tokens.muted;
  final primary = isDark ? const Color(0xFF60A5FA) : AppColors.navy;

  final base = ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: ColorScheme.fromSeed(
      seedColor: AppColors.navy,
      primary: primary,
      surface: card,
      onSurface: ink,
      onPrimary: Colors.white,
      brightness: brightness,
    ),
    scaffoldBackgroundColor: surface,
    extensions: <ThemeExtension<dynamic>>[tokens],
  );

  // Inter una sola vez por tema (cacheado en appLightTheme / appDarkTheme).
  final inter = GoogleFonts.interTextTheme(base.textTheme).apply(
    bodyColor: ink,
    displayColor: ink,
  );

  return base.copyWith(
    textTheme: inter,
    appBarTheme: AppBarTheme(
      backgroundColor: card,
      foregroundColor: ink,
      elevation: 0,
      scrolledUnderElevation: 0.5,
      centerTitle: false,
      systemOverlayStyle: systemOverlayFor(brightness),
      titleTextStyle: GoogleFonts.inter(
        fontSize: 18,
        fontWeight: FontWeight.w700,
        color: ink,
      ),
    ),
    cardTheme: CardThemeData(
      color: card,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: line),
      ),
    ),
    dividerColor: line,
    dividerTheme: DividerThemeData(color: line, thickness: 1),
    dialogTheme: DialogThemeData(
      backgroundColor: card,
      titleTextStyle: GoogleFonts.inter(
        fontSize: 18,
        fontWeight: FontWeight.w700,
        color: ink,
      ),
      contentTextStyle: GoogleFonts.inter(fontSize: 14, color: ink),
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: isDark ? const Color(0xFF1E293B) : AppColors.navy,
      contentTextStyle: const TextStyle(color: Colors.white),
      behavior: SnackBarBehavior.floating,
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: isDark ? const Color(0xFF1A2436) : Colors.white,
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      hintStyle: TextStyle(color: muted),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: line),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: line),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: primary, width: 1.5),
      ),
      labelStyle: TextStyle(color: muted),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: isDark ? const Color(0xFF2563EB) : AppColors.navy,
        foregroundColor: Colors.white,
        disabledBackgroundColor:
            isDark ? const Color(0xFF334155) : const Color(0xFFCBD5E1),
        disabledForegroundColor:
            isDark ? const Color(0xFF94A3B8) : const Color(0xFF64748B),
        minimumSize: const Size.fromHeight(48),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        textStyle: GoogleFonts.inter(
          fontWeight: FontWeight.w700,
          fontSize: 15,
        ),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: ink,
        minimumSize: const Size.fromHeight(48),
        side: BorderSide(color: line),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(foregroundColor: primary),
    ),
    iconTheme: IconThemeData(color: tokens.iconTint),
    listTileTheme: ListTileThemeData(
      iconColor: tokens.iconTint,
      textColor: ink,
      subtitleTextStyle: TextStyle(color: muted, fontSize: 13),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: card,
      indicatorColor: tokens.softInfo,
      labelTextStyle: WidgetStatePropertyAll(
        GoogleFonts.inter(fontSize: 12, fontWeight: FontWeight.w600, color: ink),
      ),
    ),
  );
}
