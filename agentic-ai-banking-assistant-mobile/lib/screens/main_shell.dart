import 'dart:async';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../services/websocket_service.dart';
import '../theme/app_theme.dart';
import 'home/home_screen.dart';
import 'accounts/accounts_screen.dart';
import 'transfer/transfer_screen.dart';
import 'cards/cards_screen.dart';
import 'chat/chat_screen.dart';

class MainShell extends StatefulWidget {
  final int initialTab;
  const MainShell({super.key, this.initialTab = 0});

  static void switchToTab(BuildContext context, int index) {
    final state = context.findAncestorStateOfType<MainShellState>();
    state?.navigateToTab(index);
  }

  @override
  State<MainShell> createState() => MainShellState();
}

class MainShellState extends State<MainShell> {
  late int _currentIndex;
  StreamSubscription? _wsSub;

  @override
  void initState() {
    super.initState();
    _currentIndex = widget.initialTab;

    // Ensure connection is active
    WebSocketService().connect();

    // Listen to real-time events for instant in-app alerts
    _wsSub = WebSocketService().stream.listen((event) {
      if (!mounted) return;
      final eventType = event['event'] ?? event['type'];
      final rawData = event['data'];
      final data = rawData is Map<String, dynamic> ? rawData : <String, dynamic>{};

      if (eventType == 'PAYMENT_RECEIVED') {
        final amount = data['amount'] ?? 0;
        final sender = data['sender'] ?? 'Bank Transfer';
        _showLiveBanner(
          icon: Icons.arrow_downward_rounded,
          iconColor: const Color(0xFF10B981),
          title: 'Money Received! 💰',
          message: 'Received LKR $amount from $sender',
        );
      } else if (eventType == 'BALANCE_UPDATED') {
        final changeType = data['changeType'] ?? '';
        final amount = data['amount'] ?? 0;
        if (changeType == 'DEPOSIT') {
          _showLiveBanner(
            icon: Icons.account_balance_rounded,
            iconColor: const Color(0xFF10B981),
            title: 'Deposit Credited 🏦',
            message: 'LKR $amount has been added to your balance',
          );
        }
      } else if (eventType == 'CARD_STATUS_CHANGED') {
        final status = data['status'] ?? 'UPDATED';
        _showLiveBanner(
          icon: Icons.credit_card_rounded,
          iconColor: const Color(0xFF818CF8),
          title: 'Card Status Updated',
          message: 'Your card status has been set to $status',
        );
      }
    });
  }

  void _showLiveBanner({
    required IconData icon,
    required Color iconColor,
    required String title,
    required String message,
  }) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        backgroundColor: const Color(0xFF1E2336),
        duration: const Duration(seconds: 4),
        content: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: iconColor.withOpacity(0.18),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: iconColor, size: 22),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: GoogleFonts.poppins(
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    message,
                    style: GoogleFonts.poppins(
                      color: const Color(0xFF94A3B8),
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _wsSub?.cancel();
    super.dispose();
  }

  void navigateToTab(int index) {
    if (index >= 0 && index < 5) {
      setState(() => _currentIndex = index);
    }
  }

  final List<Widget> _screens = const [
    HomeScreen(),
    AccountsScreen(),
    TransferScreen(),
    CardsScreen(),
    ChatScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      body: IndexedStack(
        index: _currentIndex,
        children: _screens,
      ),
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          color: AppTheme.surface,
          border: Border(
            top: BorderSide(color: Color(0xFF222238), width: 1),
          ),
        ),
        child: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildNavItem(0, Icons.home_rounded, 'Home'),
                _buildNavItem(1, Icons.account_balance_wallet_rounded, 'Accounts'),
                _buildNavItem(2, Icons.swap_horiz_rounded, 'Transfer'),
                _buildNavItem(3, Icons.credit_card_rounded, 'Cards'),
                _buildAiNavItem(4, Icons.smart_toy_rounded, 'AI Agent'),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildNavItem(int index, IconData icon, String label) {
    final isSelected = _currentIndex == index;
    return InkWell(
      onTap: () => setState(() => _currentIndex = index),
      borderRadius: BorderRadius.circular(16),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              color: isSelected ? AppTheme.primary : AppTheme.textSecondary,
              size: 24,
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: GoogleFonts.poppins(
                fontSize: 11,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                color: isSelected ? AppTheme.primary : AppTheme.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAiNavItem(int index, IconData icon, String label) {
    final isSelected = _currentIndex == index;
    return InkWell(
      onTap: () => setState(() => _currentIndex = index),
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: isSelected
            ? BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF7C4DFF), Color(0xFF536DFE)],
                ),
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF7C4DFF).withOpacity(0.4),
                    blurRadius: 10,
                    offset: const Offset(0, 3),
                  ),
                ],
              )
            : null,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              color: isSelected ? Colors.white : const Color(0xFF9E86FF),
              size: 24,
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: GoogleFonts.poppins(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: isSelected ? Colors.white : const Color(0xFF9E86FF),
              ),
            ),
          ],
        ),
      ),
    );
  }
}