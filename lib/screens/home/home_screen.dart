import '../main_shell.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../providers/accounts_provider.dart';
import '../../providers/auth_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/gradient_card.dart';
import '../../widgets/account_summary_card.dart';
import '../../widgets/quick_action_grid.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<BankDataProvider>().loadAll();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Consumer<BankDataProvider>(
        builder: (context, bank, _) {
          if (bank.loading && bank.accounts.isEmpty) {
            return const Center(
              child: CircularProgressIndicator(color: AppTheme.primary),
            );
          }
          return RefreshIndicator(
            color: AppTheme.primary,
            onRefresh: () => bank.loadAll(),
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(height: MediaQuery.of(context).padding.top + 16),

                  // ── Header ────────────────────────────────────────
                  Row(
                    children: [
                      Container(
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(
                          gradient: AppTheme.primaryGradient,
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Center(
                          child: Text(
                            (context.read<AuthProvider>().customerName ?? 'U')[0].toUpperCase(),
                            style: GoogleFonts.poppins(
                              fontSize: 20,
                              fontWeight: FontWeight.w700,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Welcome back,',
                              style: GoogleFonts.poppins(
                                fontSize: 13,
                                color: AppTheme.textSecondary,
                              ),
                            ),
                            Text(
                              context.read<AuthProvider>().customerName ?? 'User',
                              style: GoogleFonts.poppins(
                                fontSize: 18,
                                fontWeight: FontWeight.w600,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                      ),
                      IconButton(
                        onPressed: () {
                          // Navigate to notifications or settings
                        },
                        icon: const Icon(
                          Icons.notifications_none_rounded,
                          color: AppTheme.textSecondary,
                          size: 28,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 28),

                  // ── Total Balance Card ────────────────────────────
                  GradientCard(
                    gradient: AppTheme.primaryGradient,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.account_balance_wallet_rounded,
                                color: Colors.white70, size: 20),
                            const SizedBox(width: 8),
                            Text(
                              'Total Balance',
                              style: GoogleFonts.poppins(
                                fontSize: 14,
                                color: Colors.white70,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Text(
                          'LKR ${_formatBalance(bank.totalBalance)}',
                          style: GoogleFonts.poppins(
                            fontSize: 32,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                            letterSpacing: -0.5,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          '${bank.accounts.length} account(s)',
                          style: GoogleFonts.poppins(
                            fontSize: 13,
                            color: Colors.white60,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 28),

                  // ── Quick Actions ─────────────────────────────────
                  Text(
                    'Quick Actions',
                    style: GoogleFonts.poppins(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 16),
                  QuickActionGrid(
                    actions: [
                      QuickAction(
                        icon: Icons.swap_horiz_rounded,
                        label: 'Transfer',
                        gradient: AppTheme.primaryGradient,
                        onTap: () => _navigateToTab(2),
                      ),
                      QuickAction(
                        icon: Icons.credit_card_rounded,
                        label: 'Cards',
                        gradient: AppTheme.accentGradient,
                        onTap: () => _navigateToTab(3),
                      ),
                      QuickAction(
                        icon: Icons.receipt_long_rounded,
                        label: 'Bills',
                        gradient: const LinearGradient(
                          colors: [Color(0xFFFF6B6B), Color(0xFFEE5A24)],
                        ),
                        onTap: () => Navigator.pushNamed(context, '/bills'),
                      ),
                      QuickAction(
                        icon: Icons.smart_toy_rounded,
                        label: 'AI Chat',
                        gradient: const LinearGradient(
                          colors: [Color(0xFF7C4DFF), Color(0xFF536DFE)],
                        ),
                        onTap: () => _navigateToTab(4),
                      ),
                      QuickAction(
                        icon: Icons.people_outline_rounded,
                        label: 'Payees',
                        gradient: const LinearGradient(
                          colors: [Color(0xFF00BCD4), Color(0xFF0097A7)],
                        ),
                        onTap: () => Navigator.pushNamed(context, '/payees'),
                      ),
                      QuickAction(
                        icon: Icons.account_balance_rounded,
                        label: 'Loans',
                        gradient: const LinearGradient(
                          colors: [Color(0xFFFF9800), Color(0xFFF57C00)],
                        ),
                        onTap: () => Navigator.pushNamed(context, '/loans'),
                      ),
                      QuickAction(
                        icon: Icons.person_outline_rounded,
                        label: 'Profile',
                        gradient: const LinearGradient(
                          colors: [Color(0xFF9C27B0), Color(0xFF7B1FA2)],
                        ),
                        onTap: () => Navigator.pushNamed(context, '/profile'),
                      ),
                      QuickAction(
                        icon: Icons.logout_rounded,
                        label: 'Logout',
                        gradient: const LinearGradient(
                          colors: [Color(0xFF616161), Color(0xFF424242)],
                        ),
                        onTap: () => _handleLogout(),
                      ),
                    ],
                  ),
                  const SizedBox(height: 28),

                  // ── Accounts List ─────────────────────────────────
                  Text(
                    'My Accounts',
                    style: GoogleFonts.poppins(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 14),
                  ...bank.accounts.map((account) => AccountSummaryCard(
                    account: account,
                    onTap: () => _navigateToTab(1),
                  )),

                  // ── AI Assistant Promo ─────────────────────────────
                  const SizedBox(height: 20),
                  GradientCard(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF7C4DFF), Color(0xFF536DFE)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    onTap: () => _navigateToTab(4),
                    child: Row(
                      children: [
                        Container(
                          width: 52,
                          height: 52,
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.2),
                            borderRadius: BorderRadius.circular(14),
                          ),
                          child: const Icon(
                            Icons.smart_toy_rounded,
                            color: Colors.white,
                            size: 28,
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'AI Banking Assistant',
                                style: GoogleFonts.poppins(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.white,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                'Chat in English, Sinhala or Tamil to do banking tasks',
                                style: GoogleFonts.poppins(
                                  fontSize: 12,
                                  color: Colors.white70,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const Icon(Icons.arrow_forward_ios_rounded,
                            color: Colors.white70, size: 18),
                      ],
                    ),
                  ),
                  const SizedBox(height: 100),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  String _formatBalance(double balance) {
    final formatted = balance.toStringAsFixed(2);
    final parts = formatted.split('.');
    final intPart = parts[0].replaceAllMapped(
      RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
      (m) => '${m[1]},',
    );
    return '$intPart.${parts[1]}';
  }

  void _navigateToTab(int index) {
    MainShell.switchToTab(context, index);
  }

  Future<void> _handleLogout() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text('Logout', style: GoogleFonts.poppins(color: Colors.white)),
        content: Text('Are you sure you want to log out?',
            style: GoogleFonts.poppins(color: AppTheme.textSecondary)),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text('Cancel', style: GoogleFonts.poppins(color: AppTheme.textSecondary)),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text('Logout', style: GoogleFonts.poppins(color: AppTheme.error)),
          ),
        ],
      ),
    );
    if (confirm == true && mounted) {
      context.read<AuthProvider>().logout();
      context.read<BankDataProvider>().clear();
      Navigator.pushReplacementNamed(context, '/login');
    }
  }
}

