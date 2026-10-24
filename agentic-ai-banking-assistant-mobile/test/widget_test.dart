import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:agentic_bank/main.dart';
import 'package:agentic_bank/providers/accounts_provider.dart';
import 'package:agentic_bank/providers/auth_provider.dart';
import 'package:agentic_bank/providers/chat_provider.dart';
import 'package:agentic_bank/services/api_client.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    final apiClient = ApiClient();
    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider(create: (_) => AuthProvider(apiClient)),
          ChangeNotifierProvider(create: (_) => BankDataProvider(apiClient)),
          ChangeNotifierProvider(create: (_) => ChatProvider(apiClient)),
        ],
        child: const AgenticBankApp(),
      ),
    );
    await tester.pump(const Duration(seconds: 4));
    expect(find.byType(AgenticBankApp), findsOneWidget);
  });
}