import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'providers/accounts_provider.dart';
import 'providers/auth_provider.dart';
import 'providers/chat_provider.dart';
import 'screens/bills/bills_screen.dart';
import 'screens/loans/loans_screen.dart';
import 'screens/main_shell.dart';
import 'screens/payees/payees_screen.dart';
import 'screens/profile/profile_screen.dart';
import 'screens/splash/splash_screen.dart';
import 'screens/auth/login_screen.dart';
import 'screens/auth/signup_screen.dart';
import 'services/api_client.dart';
import 'theme/app_theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final apiClient = ApiClient();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider(apiClient)),
        ChangeNotifierProvider(create: (_) => BankDataProvider(apiClient)),
        ChangeNotifierProvider(create: (_) => ChatProvider(apiClient)),
      ],
      child: const AgenticBankApp(),
    ),
  );
}

class AgenticBankApp extends StatelessWidget {
  const AgenticBankApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Agentic Bank',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      initialRoute: '/',
      routes: {
        '/': (context) => const SplashScreen(),
        '/login': (context) => const LoginScreen(),
        '/signup': (context) => const SignupScreen(),
        '/main': (context) => const MainShell(),
        '/bills': (context) => const BillsScreen(),
        '/loans': (context) => const LoansScreen(),
        '/payees': (context) => const PayeesScreen(),
        '/profile': (context) => const ProfileScreen(),
      },
    );
  }
}