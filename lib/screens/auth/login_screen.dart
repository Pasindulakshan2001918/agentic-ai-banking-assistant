import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/custom_text_field.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _cifController = TextEditingController();
  String _selectedLang = 'en';

  @override
  void dispose() {
    _cifController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    if (!_formKey.currentState!.validate()) return;

    final auth = context.read<AuthProvider>();
    final success = await auth.login(
      cif: _cifController.text.trim(),
      lang: _selectedLang,
    );

    if (success && mounted) {
      Navigator.pushReplacementNamed(context, '/main');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF1A1A35), AppTheme.background],
          ),
        ),
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 60),

                  // Logo
                  Center(
                    child: Container(
                      width: 80,
                      height: 80,
                      decoration: BoxDecoration(
                        gradient: AppTheme.primaryGradient,
                        borderRadius: BorderRadius.circular(22),
                        boxShadow: [
                          BoxShadow(
                            color: AppTheme.primary.withOpacity(0.35),
                            blurRadius: 20,
                            offset: const Offset(0, 8),
                          ),
                        ],
                      ),
                      child: const Icon(
                        Icons.account_balance_rounded,
                        size: 40,
                        color: Colors.white,
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),

                  Text(
                    'Welcome Back',
                    textAlign: TextAlign.center,
                    style: GoogleFonts.poppins(
                      fontSize: 28,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Sign in with your Customer ID',
                    textAlign: TextAlign.center,
                    style: GoogleFonts.poppins(
                      fontSize: 15,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 48),

                  // CIF Field
                  CustomTextField(
                    controller: _cifController,
                    label: 'Customer ID (CIF)',
                    hint: 'e.g. CIF0001',
                    prefixIcon: Icons.person_outline_rounded,
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'Please enter your CIF' : null,
                  ),
                  const SizedBox(height: 20),

                  // Language selector
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppTheme.surfaceLight,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: AppTheme.divider),
                    ),
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<String>(
                        value: _selectedLang,
                        isExpanded: true,
                        dropdownColor: AppTheme.surfaceLight,
                        icon: const Icon(Icons.language_rounded, color: AppTheme.textSecondary),
                        style: GoogleFonts.poppins(fontSize: 14, color: AppTheme.textPrimary),
                        items: const [
                          DropdownMenuItem(value: 'en', child: Text('English')),
                          DropdownMenuItem(value: 'si', child: Text('Sinhala (සිංහල)')),
                          DropdownMenuItem(value: 'ta', child: Text('Tamil (தமிழ்)')),
                        ],
                        onChanged: (v) => setState(() => _selectedLang = v!),
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),

                  // Error message
                  Consumer<AuthProvider>(
                    builder: (context, auth, _) {
                      if (auth.status == AuthStatus.error && auth.errorMessage != null) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 16),
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: AppTheme.error.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: AppTheme.error.withOpacity(0.3)),
                            ),
                            child: Text(
                              auth.errorMessage!,
                              style: GoogleFonts.poppins(color: AppTheme.error, fontSize: 13),
                            ),
                          ),
                        );
                      }
                      return const SizedBox.shrink();
                    },
                  ),

                  // Login button
                  Consumer<AuthProvider>(
                    builder: (context, auth, _) {
                      final isLoading = auth.status == AuthStatus.loading;
                      return ElevatedButton(
                        onPressed: isLoading ? null : _handleLogin,
                        child: isLoading
                            ? const SizedBox(
                                width: 22, height: 22,
                                child: CircularProgressIndicator(
                                  color: Colors.white, strokeWidth: 2.5,
                                ),
                              )
                            : const Text('Sign In'),
                      );
                    },
                  ),
                  const SizedBox(height: 24),

                  // Signup link
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        "Don't have an account? ",
                        style: GoogleFonts.poppins(
                          fontSize: 14,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                      GestureDetector(
                        onTap: () => Navigator.pushNamed(context, '/signup'),
                        child: Text(
                          'Sign Up',
                          style: GoogleFonts.poppins(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: AppTheme.primary,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 32),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}