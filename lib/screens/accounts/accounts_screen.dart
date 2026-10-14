import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../providers/accounts_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/account_summary_card.dart';
import '../../widgets/empty_state.dart';

class AccountsScreen extends StatelessWidget {
  const AccountsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Accounts'),
        automaticallyImplyLeading: false,
      ),
      body: Consumer<BankDataProvider>(
        builder: (context, bank, _) {
          if (bank.loading && bank.accounts.isEmpty) {
            return const Center(
              child: CircularProgressIndicator(color: AppTheme.primary),
            );
          }
          if (bank.accounts.isEmpty) {
            return const EmptyState(
              icon: Icons.account_balance_outlined,
              title: 'No Accounts',
              subtitle: 'You don\'t have any accounts yet.',
            );
          }
          return RefreshIndicator(
            color: AppTheme.primary,
            onRefresh: () => bank.refreshAccounts(),
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                // Total balance header
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    gradient: AppTheme.primaryGradient,
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: Column(
                    children: [
                      Text(
                        'Total Balance',
                        style: GoogleFonts.poppins(
                          fontSize: 14, color: Colors.white70,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'LKR ${_formatBalance(bank.totalBalance)}',
                        style: GoogleFonts.poppins(
                          fontSize: 28,
                          fontWeight: FontWeight.w700,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  'All Accounts',
                  style: GoogleFonts.poppins(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 14),
                ...bank.accounts.map((a) => AccountSummaryCard(account: a)),
              ],
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
}