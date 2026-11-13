import 'package:flutter_test/flutter_test.dart';
import 'package:andina_rrhh/main.dart';
import 'package:andina_rrhh/providers/theme_provider.dart';

void main() {
  testWidgets('App arranca', (tester) async {
    final theme = ThemeProvider();
    await tester.pumpWidget(AndinaApp(themeProvider: theme));
    await tester.pump();
    expect(find.byType(AndinaApp), findsOneWidget);
  });
}
