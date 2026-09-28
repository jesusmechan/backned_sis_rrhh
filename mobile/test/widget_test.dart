import 'package:flutter_test/flutter_test.dart';
import 'package:andina_rrhh/main.dart';

void main() {
  testWidgets('App arranca', (tester) async {
    await tester.pumpWidget(const AndinaApp());
    await tester.pump();
    expect(find.byType(AndinaApp), findsOneWidget);
  });
}
