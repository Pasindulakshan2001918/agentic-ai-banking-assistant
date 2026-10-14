import 'package:flutter_test/flutter_test.dart';
import 'package:agentic_bank/main.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const AgenticBankApp());
    expect(find.byType(AgenticBankApp), findsOneWidget);
  });
}