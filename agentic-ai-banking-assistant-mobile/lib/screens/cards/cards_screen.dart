import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../models/card_model.dart';
import '../../providers/accounts_provider.dart';
import '../../providers/auth_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/empty_state.dart';

class CardsScreen extends StatefulWidget {
  const CardsScreen({super.key});

  @override
  State<CardsScreen> createState() => _CardsScreenState();
}

class _CardsScreenState extends State<CardsScreen> {
  int _selectedCardIndex = 0;
  bool _showCvv = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Text(
          'My Cards',
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: Colors.white,
          ),
        ),
      ),
      body: Consumer<BankDataProvider>(
        builder: (context, bank, _) {
          if (bank.loading && bank.cards.isEmpty) {
            return const Center(
              child: CircularProgressIndicator(color: AppTheme.primary),
            );
          }

          if (bank.cards.isEmpty) {
            return RefreshIndicator(
              color: AppTheme.primary,
              onRefresh: () => bank.refreshCards(),
              child: ListView(
                children: const [
                  SizedBox(height: 100),
                  EmptyState(
                    icon: Icons.credit_card_off_rounded,
                    title: 'No Cards Found',
                    subtitle: 'You do not have any active debit or credit cards linked yet.',
                  ),
                ],
              ),
            );
          }

          final selectedCard = bank.cards[
              _selectedCardIndex.clamp(0, bank.cards.length - 1)];

          return RefreshIndicator(
            color: AppTheme.primary,
            onRefresh: () => bank.refreshCards(),
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(height: 12),

                  // ── Card Carousel / Selector ──
                  SizedBox(
                    height: 220,
                    child: PageView.builder(
                      itemCount: bank.cards.length,
                      onPageChanged: (index) {
                        setState(() {
                          _selectedCardIndex = index;
                          _showCvv = false;
                        });
                      },
                      itemBuilder: (context, index) {
                        return Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 4),
                          child: _buildCreditCard(bank.cards[index], index),
                        );
                      },
                    ),
                  ),

                  // ── Page Dots ──
                  if (bank.cards.length > 1) ...[
                    const SizedBox(height: 12),
                    Row(
                      mainAxisAlignment: MainContextCenter(context),
                      children: List.generate(
                        bank.cards.length,
                        (index) => Container(
                          width: index == _selectedCardIndex ? 20 : 8,
                          height: 8,
                          margin: const EdgeInsets.symmetric(horizontal: 3),
                          decoration: BoxDecoration(
                            color: index == _selectedCardIndex
                                ? AppTheme.primary
                                : const Color(0xFF33334D),
                            borderRadius: BorderRadius.circular(4),
                          ),
                        ),
                      ),
                    ),
                  ],

                  const SizedBox(height: 28),

                  // ── Card Details & Balance ──
                  _buildCardDetails(selectedCard),

                  const SizedBox(height: 28),

                  // ── Security & Controls ──
                  Text(
                    'Card Controls',
                    style: GoogleFonts.poppins(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 14),

                  _buildControlTile(
                    icon: Icons.lock_outline_rounded,
                    title: 'Freeze / Block Card',
                    subtitle: selectedCard.isBlocked
                        ? 'Card is temporarily blocked'
                        : 'Temporarily lock card transactions',
                    isDestructive: !selectedCard.isBlocked,
                    trailing: Switch.adaptive(
                      value: selectedCard.isBlocked,
                      activeColor: AppTheme.error,
                      onChanged: (val) => _handleBlockCard(selectedCard),
                    ),
                  ),

                  _buildControlTile(
                    icon: Icons.visibility_outlined,
                    title: 'Show CVV',
                    subtitle: _showCvv ? 'CVV: 842' : 'Tap to reveal 3-digit CVV',
                    trailing: IconButton(
                      icon: Icon(
                        _showCvv ? Icons.visibility_off : Icons.visibility,
                        color: AppTheme.textSecondary,
                      ),
                      onPressed: () {
                        setState(() => _showCvv = !_showCvv);
                      },
                    ),
                  ),

                  _buildControlTile(
                    icon: Icons.pin_outlined,
                    title: 'Change Card PIN',
                    subtitle: 'Update your 4-digit ATM / POS PIN',
                    trailing: const Icon(Icons.arrow_forward_ios_rounded,
                        size: 16, color: AppTheme.textSecondary),
                    onTap: () {
                      _showSnackBar('PIN change request submitted.');
                    },
                  ),

                  _buildControlTile(
                    icon: Icons.language_rounded,
                    title: 'Online & International Usage',
                    subtitle: 'Allowed for e-commerce and POS abroad',
                    trailing: Switch.adaptive(
                      value: true,
                      activeColor: AppTheme.primary,
                      onChanged: (val) {},
                    ),
                  ),

                  const SizedBox(height: 40),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  MainAxisAlignment MainContextCenter(BuildContext context) =>
      MainAxisAlignment.center;

  Widget _buildCreditCard(CardModel card, int index) {
    final isDebit = card.cardType.toUpperCase() == 'DEBIT';
    final gradient = isDebit
        ? const LinearGradient(
            colors: [Color(0xFF1E3C72), Color(0xFF2A5298)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          )
        : const LinearGradient(
            colors: [Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          );

    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        gradient: gradient,
        borderRadius: BorderRadius.circular(22),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.35),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'AGENTIC BANK',
                style: GoogleFonts.poppins(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 1.5,
                  color: Colors.white,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: card.isBlocked
                      ? AppTheme.error.withOpacity(0.8)
                      : Colors.white.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  card.isBlocked ? 'BLOCKED' : card.cardType.toUpperCase(),
                  style: GoogleFonts.poppins(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: Colors.white,
                  ),
                ),
              ),
            ],
          ),

          Row(
            children: [
              const Icon(Icons.nfc_rounded, color: Colors.white70, size: 28),
              const SizedBox(width: 8),
              Container(
                width: 38,
                height: 28,
                decoration: BoxDecoration(
                  color: const Color(0xFFFFD700).withOpacity(0.85),
                  borderRadius: BorderRadius.circular(6),
                ),
              ),
            ],
          ),

          Text(
            card.maskedNumber,
            style: GoogleFonts.spaceMono(
              fontSize: 20,
              fontWeight: FontWeight.w700,
              letterSpacing: 3,
              color: Colors.white,
            ),
          ),

          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'CARD HOLDER',
                    style: GoogleFonts.poppins(
                      fontSize: 9,
                      color: Colors.white60,
                      letterSpacing: 1,
                    ),
                  ),
                  Text(
                    context.watch<AuthProvider>().customerName?.toUpperCase() ?? 'VALUED CUSTOMER',
                    style: GoogleFonts.poppins(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                ],
              ),
              Text(
                card.network.toUpperCase(),
                style: GoogleFonts.poppins(
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                  fontStyle: FontStyle.italic,
                  color: Colors.white,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildCardDetails(CardModel card) {
    final isCredit = card.cardType.toUpperCase() == 'CREDIT';

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFF222238)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            isCredit ? 'Credit Limit Details' : 'Card Information',
            style: GoogleFonts.poppins(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 16),
          if (isCredit && card.creditLimit != null) ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Total Limit',
                    style: GoogleFonts.poppins(
                        fontSize: 13, color: AppTheme.textSecondary)),
                Text(
                  'LKR ${card.creditLimit!.toStringAsFixed(2)}',
                  style: GoogleFonts.poppins(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Outstanding Balance',
                    style: GoogleFonts.poppins(
                        fontSize: 13, color: AppTheme.textSecondary)),
                Text(
                  'LKR ${(card.outstandingBalance ?? 0.0).toStringAsFixed(2)}',
                  style: GoogleFonts.poppins(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppTheme.error,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: LinearProgressIndicator(
                value: (card.outstandingBalance ?? 0.0) /
                    (card.creditLimit! > 0 ? card.creditLimit! : 1.0),
                minHeight: 8,
                backgroundColor: const Color(0xFF33334D),
                valueColor: const AlwaysStoppedAnimation(AppTheme.primary),
              ),
            ),
          ] else ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Card Network',
                    style: GoogleFonts.poppins(
                        fontSize: 13, color: AppTheme.textSecondary)),
                Text(card.network,
                    style: GoogleFonts.poppins(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: Colors.white)),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Status',
                    style: GoogleFonts.poppins(
                        fontSize: 13, color: AppTheme.textSecondary)),
                Text(card.status,
                    style: GoogleFonts.poppins(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: card.isBlocked ? AppTheme.error : AppTheme.success)),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildControlTile({
    required IconData icon,
    required String title,
    required String subtitle,
    Widget? trailing,
    VoidCallback? onTap,
    bool isDestructive = false,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFF222238)),
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        leading: Container(
          width: 42,
          height: 42,
          decoration: BoxDecoration(
            color: (isDestructive ? AppTheme.error : AppTheme.primary)
                .withOpacity(0.12),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(
            icon,
            color: isDestructive ? AppTheme.error : AppTheme.primary,
            size: 22,
          ),
        ),
        title: Text(
          title,
          style: GoogleFonts.poppins(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: Colors.white,
          ),
        ),
        subtitle: Text(
          subtitle,
          style: GoogleFonts.poppins(
            fontSize: 12,
            color: AppTheme.textSecondary,
          ),
        ),
        trailing: trailing,
        onTap: onTap,
      ),
    );
  }

  Future<void> _handleBlockCard(CardModel card) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text(
          card.isBlocked ? 'Unblock Card' : 'Block Card',
          style: GoogleFonts.poppins(color: Colors.white),
        ),
        content: Text(
          card.isBlocked
              ? 'Are you sure you want to unblock this card?'
              : 'Blocking this card will immediately stop all ATM, online, and POS transactions. Continue?',
          style: GoogleFonts.poppins(color: AppTheme.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text('Cancel',
                style: GoogleFonts.poppins(color: AppTheme.textSecondary)),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(
              card.isBlocked ? 'Unblock' : 'Block',
              style: GoogleFonts.poppins(
                  color: card.isBlocked ? AppTheme.success : AppTheme.error),
            ),
          ),
        ],
      ),
    );

    if (confirm == true && mounted) {
      final success = await context.read<BankDataProvider>().blockCard(card.id);
      _showSnackBar(success
          ? 'Card status updated successfully.'
          : 'Failed to update card status.');
    }
  }

  void _showSnackBar(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message, style: GoogleFonts.poppins()),
        backgroundColor: AppTheme.surface,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }
}