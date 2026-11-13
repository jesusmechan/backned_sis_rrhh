import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

class DropletNavItem {
  const DropletNavItem({
    required this.icon,
    required this.activeIcon,
    required this.label,
    this.showDot = false,
  });

  final IconData icon;
  final IconData activeIcon;
  final String label;
  final bool showDot;
}

/// Barra inferior: la gota navy se mueve y eleva el ícono activo.
class DropletNavBar extends StatelessWidget {
  const DropletNavBar({
    super.key,
    required this.items,
    required this.index,
    required this.onTap,
  });

  final List<DropletNavItem> items;
  final int index;
  final ValueChanged<int> onTap;

  static const barHeight = 70.0;
  static const liftRoom = 22.0;
  static const iconBox = 48.0;
  static const lift = 16.0;
  /// Distancia del borde inferior de la barra al centro del ícono en reposo.
  static const iconCenterFromBarBottom = 38.0;
  static const anim = Duration(milliseconds: 340);
  static const curve = Curves.easeOutCubic;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tokens = AppColors.tokens(context);
    final bottom = MediaQuery.paddingOf(context).bottom;
    final screenW = MediaQuery.sizeOf(context).width;
    final maxBar = screenW >= 900
        ? 520.0
        : screenW >= 600
            ? 480.0
            : screenW - 24;

    final totalH = barHeight + liftRoom;

    return ColoredBox(
      color: theme.scaffoldBackgroundColor,
      child: Padding(
        padding: EdgeInsets.fromLTRB(12, 0, 12, bottom > 0 ? bottom : 10),
        child: SizedBox(
          height: totalH,
          width: double.infinity,
          child: Align(
            alignment: Alignment.bottomCenter,
            child: SizedBox(
              width: maxBar.clamp(0, screenW - 24),
              height: totalH,
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final slot = constraints.maxWidth / items.length;
                  // Centro del ícono activo (elevado).
                  final centerYFromBottom =
                      iconCenterFromBarBottom + lift;
                  final blobTop =
                      totalH - centerYFromBottom - iconBox / 2;
                  final blobLeft = index * slot + (slot - iconBox) / 2;

                  return Stack(
                    clipBehavior: Clip.none,
                    children: [
                      Positioned(
                        left: 0,
                        right: 0,
                        bottom: 0,
                        height: barHeight,
                        child: DecoratedBox(
                          decoration: BoxDecoration(
                            color: tokens.navBar,
                            borderRadius: BorderRadius.circular(24),
                            border: Border.all(color: tokens.navBarBorder),
                            boxShadow: [
                              BoxShadow(
                                color: AppColors.navy.withValues(alpha: 0.08),
                                blurRadius: 18,
                                offset: const Offset(0, 6),
                              ),
                            ],
                          ),
                        ),
                      ),
                      AnimatedPositioned(
                        duration: anim,
                        curve: curve,
                        left: blobLeft,
                        top: blobTop,
                        width: iconBox,
                        height: iconBox,
                        child: IgnorePointer(
                          child: Stack(
                            alignment: Alignment.center,
                            clipBehavior: Clip.none,
                            children: [
                              Container(
                                width: iconBox + 16,
                                height: iconBox + 16,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  gradient: RadialGradient(
                                    colors: [
                                      const Color(0xFF3B82F6)
                                          .withValues(alpha: 0.28),
                                      const Color(0xFF2563EB)
                                          .withValues(alpha: 0.08),
                                      Colors.transparent,
                                    ],
                                  ),
                                ),
                              ),
                              Container(
                                width: iconBox,
                                height: iconBox,
                                decoration: BoxDecoration(
                                  color: AppColors.navy,
                                  shape: BoxShape.circle,
                                  border: Border.all(
                                    color: Colors.white,
                                    width: 3.5,
                                  ),
                                  boxShadow: [
                                    BoxShadow(
                                      color: AppColors.navy
                                          .withValues(alpha: 0.35),
                                      blurRadius: 14,
                                      offset: const Offset(0, 5),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                      Positioned.fill(
                        child: Row(
                          children: [
                            for (var i = 0; i < items.length; i++)
                              Expanded(
                                child: _DropletTab(
                                  item: items[i],
                                  selected: i == index,
                                  onTap: () => onTap(i),
                                ),
                              ),
                          ],
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _DropletTab extends StatelessWidget {
  const _DropletTab({
    required this.item,
    required this.selected,
    required this.onTap,
  });

  final DropletNavItem item;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final tokens = AppColors.tokens(context);
    final inactive = tokens.muted;
    final labelActive = tokens.brand;

    final centerFromBottom = DropletNavBar.iconCenterFromBarBottom +
        (selected ? DropletNavBar.lift : 0);
    final iconBottom = centerFromBottom - DropletNavBar.iconBox / 2;
    // Label más arriba cuando está activo (Inicio y el resto).
    final labelBottom = selected ? 9.0 : 7.0;

    return InkWell(
      onTap: onTap,
      splashColor: Colors.transparent,
      highlightColor: Colors.transparent,
      child: Stack(
        alignment: Alignment.bottomCenter,
        clipBehavior: Clip.none,
        children: [
          AnimatedPositioned(
            duration: DropletNavBar.anim,
            curve: DropletNavBar.curve,
            bottom: iconBottom,
            left: 0,
            right: 0,
            height: DropletNavBar.iconBox,
            child: Center(
              child: SizedBox(
                width: DropletNavBar.iconBox,
                height: DropletNavBar.iconBox,
                child: Stack(
                  alignment: Alignment.center,
                  clipBehavior: Clip.none,
                  children: [
                    AnimatedSwitcher(
                      duration: const Duration(milliseconds: 260),
                      child: Icon(
                        selected ? item.activeIcon : item.icon,
                        key: ValueKey('${item.label}-$selected'),
                        size: selected ? 24 : 22,
                        color: selected ? Colors.white : inactive,
                      ),
                    ),
                    if (item.showDot && !selected)
                      Positioned(
                        right: 8,
                        top: 8,
                        child: Container(
                          width: 7,
                          height: 7,
                          decoration: BoxDecoration(
                            color: const Color(0xFFEF4444),
                            shape: BoxShape.circle,
                            border: Border.all(color: Colors.white, width: 1.2),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
          AnimatedPositioned(
            duration: DropletNavBar.anim,
            curve: DropletNavBar.curve,
            bottom: labelBottom,
            left: 2,
            right: 2,
            child: AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 260),
              curve: DropletNavBar.curve,
              style: TextStyle(
                fontSize: selected ? 11 : 10,
                height: 1.05,
                fontWeight: selected ? FontWeight.w800 : FontWeight.w500,
                color: selected ? labelActive : inactive,
              ),
              child: Text(
                item.label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
