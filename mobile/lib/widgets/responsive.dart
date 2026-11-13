import 'package:flutter/material.dart';

/// Anchos de contenido según tamaño de pantalla (teléfono / tablet / desktop).
class Breakpoints {
  static const phone = 600.0;
  static const tablet = 900.0;

  static bool isPhone(BuildContext context) =>
      MediaQuery.sizeOf(context).width < phone;

  static bool isTablet(BuildContext context) {
    final w = MediaQuery.sizeOf(context).width;
    return w >= phone && w < tablet;
  }

  static double contentMaxWidth(BuildContext context) {
    final w = MediaQuery.sizeOf(context).width;
    if (w >= tablet) return 720;
    if (w >= phone) return 560;
    return w;
  }

  static EdgeInsets pagePadding(BuildContext context) {
    final w = MediaQuery.sizeOf(context).width;
    final horizontal = w >= tablet
        ? 32.0
        : w >= phone
            ? 24.0
            : 20.0;
    return EdgeInsets.fromLTRB(horizontal, 12, horizontal, 28);
  }
}

/// Centra el contenido y limita el ancho en tablets.
class ResponsiveBody extends StatelessWidget {
  const ResponsiveBody({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: Breakpoints.contentMaxWidth(context)),
        child: child,
      ),
    );
  }
}
