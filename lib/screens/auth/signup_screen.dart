import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/custom_text_field.dart';

class SignupScreen extends StatefulWidget {
  const SignupScreen({super.key});

  @override
  State<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends State<SignupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _cifController = TextEditingController();
  final _nameController = TextEditingController();
  final _mobileController = TextEditingController();
  String _selectedLang = 'en';

  @override
  void dispose() {
    _cifController.dispose();
    _nameController.dispose();
    _mobileController.dispose();
    super.dispose();
  }

  Future<void> _handleSignup() async {
    if (!_formKey.currentState!.validate()) return;

    final auth = context.read<AuthProvider>();
    final success = await auth.signup(
      cif: _cifController.text.trim(),
      name: _nameController.text.trim(),
      mobile: _mobileController.text.trim(),
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
                  const SizedBox(height: 24),

                  // Back button
                  Align(
                    alignment: Alignment.centerLeft,
                    child: IconButton(
                      onPressed: () => Navigator.pop(context),
                      icon: const Icon(Icons.arrow_back_ios_rounded, color: Colors.white),
                    ),
                  ),
                  const SizedBox(height: 16),

                  Text(
                    'Create Account',
                    textAlign: TextAlign.center,
                    style: GoogleFonts.poppins(
                      fontSize: 28,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Open your new bank account in seconds',
                    textAlign: TextAlign.center,
                    style: GoogleFonts.poppins(
                      fontSize: 15,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 40),

                  // CIF
                  CustomTextField(
                    controller: _cifController,
                    label: 'Customer ID (CIF)',
                    hint: 'e.g. CIF0010',
                    prefixIcon: Icons.badge_outlined,
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'Please enter a CIF' : null,
                  ),
                  const SizedBox(height: 18),

                  // Full Name
                  CustomTextField(
                    controller: _nameController,
                    label: 'Full Name',
                    hint: 'e.g. Kamal Perera',
                    prefixIcon: Icons.person_outline_rounded,
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'Please enter your name' : null,
                  ),
                  const SizedBox(height: 18),

                  // Mobile
                  CustomTextField(
                    controller: _mobileController,
                    label: 'Mobile Number',
                    hint: 'e.g. 0771234567',
                    prefixIcon: Icons.phone_outlined,
                    keyboardType: TextInputType.phone,
                    validator: (v) =>
                        v == null || v.trim().length < 10 ? 'Enter a valid mobile number' : null,
                  ),
                  const SizedBox(height: 18),

                  // Language
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

                  // Signup button
                  Consumer<AuthProvider>(
                    builder: (context, auth, _) {
                      final isLoading = auth.status == AuthStatus.loading;
                      return ElevatedButton(
                        onPressed: isLoading ? null : _handleSignup,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.accent,
                        ),
                        child: isLoading
                            ? const SizedBox(
                                width: 22, height: 22,
                                child: CircularProgressIndicator(
                                  color: Colors.white, strokeWidth: 2.5,
                                ),
                              )
                            : const Text('Create Account'),
                      );
                    },
                  ),
                  const SizedBox(height: 24),

                  // Login link
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        'Already have an account? ',
                        style: GoogleFonts.poppins(
                          fontSize: 14,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                      GestureDetector(
                        onTap: () => Navigator.pop(context),
                        child: Text(
                          'Sign In',
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

                  // Info box
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppTheme.primary.withOpacity(0.08),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppTheme.primary.withOpacity(0.2)),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.info_outline_rounded, color: AppTheme.primary, size: 20),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            'Your account will be created with a Savings account (LKR 100,000), a Loan, and a Credit Card.',
                            style: GoogleFonts.poppins(
                              fontSize: 12,
                              color: AppTheme.textSecondary,
                              height: 1.5,
                            ),
                          ),
                        ),
                      ],
                    ),
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