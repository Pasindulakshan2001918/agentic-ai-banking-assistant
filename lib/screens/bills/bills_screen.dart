import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../models/account_model.dart';
import '../../models/biller_model.dart';
import '../../providers/accounts_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/empty_state.dart';

class BillsScreen extends StatefulWidget {
  const BillsScreen({super.key});

  @override
  State<BillsScreen> createState() => _BillsScreenState();
}

class _BillsScreenState extends State<BillsScreen> {
  final _amountController = TextEditingController();
  final _otpController = TextEditingController();

  BillerModel? _selectedBiller;
  AccountModel? _selectedAccount;
  bool _isProcessing = false;
  
  @override
  void dispose() {
    _amountController.dispose();
    _otpController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Text(
          'Bill Payments',
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: Colors.white,
          ),
        ),
      ),
      body: Consumer<BankDataProvider>(
        builder: (context, bank, _) {
          if (bank.loading && bank.billers.isEmpty) {
            return const Center(
              child: CircularProgressIndicator(color: AppTheme.primary),
            );
          }

          if (bank.billers.isEmpty) {
            return RefreshIndicator(
              color: AppTheme.primary,
              onRefresh: () => bank.loadAll(),
              child: ListView(
                children: const [
                  SizedBox(height: 100),
                  EmptyState(
                    icon: Icons.receipt_long_rounded,
                    title: 'No Registered Billers',
                    subtitle: 'You do not have any saved billers. Ask the AI assistant or add one to get started.',
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            color: AppTheme.primary,
            onRefresh: () => bank.loadAll(),
            child: ListView(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              children: [
                // ── Header Banner ──
                Container(
                  padding: const EdgeInsets.all(18),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFFFF6B6B), Color(0xFFEE5A24)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.2),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: const Icon(
                          Icons.bolt_rounded,
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
                              'Instant Bill Settlements',
                              style: GoogleFonts.poppins(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                color: Colors.white,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Zero fees on utility and telecom payments.',
                              style: GoogleFonts.poppins(
                                fontSize: 12,
                                color: Colors.white70,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 24),

                Text(
                  'Your Registered Billers (${bank.billers.length})',
                  style: GoogleFonts.poppins(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 14),

                ...bank.billers.map((biller) => _buildBillerCard(biller, bank.accounts)),

                const SizedBox(height: 40),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildBillerCard(BillerModel biller, List<AccountModel> accounts) {
    IconData icon = Icons.receipt_rounded;
    Color iconColor = AppTheme.primary;
    final nameLower = biller.billerName.toLowerCase();

    if (nameLower.contains('ceb') || nameLower.contains('electric') || nameLower.contains('leco')) {
      icon = Icons.electric_bolt_rounded;
      iconColor = const Color(0xFFFFB300);
    } else if (nameLower.contains('water')) {
      icon = Icons.water_drop_rounded;
      iconColor = const Color(0xFF03A9F4);
    } else if (nameLower.contains('dialog') || nameLower.contains('mobitel') || nameLower.contains('telecom') || nameLower.contains('slt')) {
      icon = Icons.phone_android_rounded;
      iconColor = const Color(0xFF4CAF50);
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFF222238)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: iconColor.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: iconColor, size: 24),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      biller.nickname ?? biller.billerName,
                      style: GoogleFonts.poppins(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                      ),
                    ),
                    if (biller.nickname != null)
                      Text(
                        biller.billerName,
                        style: GoogleFonts.poppins(
                          fontSize: 12,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                    if (biller.referenceNo != null)
                      Text(
                        'Ref: ${biller.referenceNo}',
                        style: GoogleFonts.poppins(
                          fontSize: 11,
                          color: AppTheme.textSecondary.withOpacity(0.7),
                        ),
                      ),
                  ],
                ),
              ),
              if (biller.amountDue != null)
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      'LKR ${biller.amountDue!.toStringAsFixed(2)}',
                      style: GoogleFonts.poppins(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: AppTheme.error,
                      ),
                    ),
                    if (biller.dueDate != null)
                      Text(
                        'Due: ${biller.dueDate}',
                        style: GoogleFonts.poppins(
                          fontSize: 10,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                  ],
                ),
            ],
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primary,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                padding: const EdgeInsets.symmetric(vertical: 12),
              ),
              onPressed: () => _openPayDialog(biller, accounts),
              child: Text(
                'Pay Bill',
                style: GoogleFonts.poppins(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _openPayDialog(BillerModel biller, List<AccountModel> accounts) {
    if (accounts.isEmpty) {
      _showSnackBar('No bank accounts available to pay bill.');
      return;
    }

    _selectedBiller = biller;
    _selectedAccount = accounts.first;
    _amountController.text = biller.amountDue != null ? biller.amountDue!.toStringAsFixed(2) : '';

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppTheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return StatefulBuilder(
          builder: (context, setSheetState) {
            return Padding(
              padding: EdgeInsets.only(
                left: 20,
                right: 20,
                top: 24,
                bottom: MediaQuery.of(context).viewInsets.bottom + 24,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 40,
                      height: 4,
                      decoration: BoxDecoration(
                        color: const Color(0xFF44445A),
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Pay ${biller.nickname ?? biller.billerName}',
                    style: GoogleFonts.poppins(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 18),

                  // ── Source Account ──
                  Text('From Account',
                      style: GoogleFonts.poppins(
                          fontSize: 13, color: AppTheme.textSecondary)),
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14),
                    decoration: BoxDecoration(
                      color: AppTheme.background,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: const Color(0xFF33334D)),
                    ),
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<AccountModel>(
                        value: _selectedAccount,
                        dropdownColor: AppTheme.surface,
                        isExpanded: true,
                        items: accounts.map((acc) {
                          return DropdownMenuItem(
                            value: acc,
                            child: Text(
                              '${acc.accountType} (${acc.maskedAccount}) - LKR ${acc.balance.toStringAsFixed(2)}',
                              style: GoogleFonts.poppins(
                                  color: Colors.white, fontSize: 13),
                            ),
                          );
                        }).toList(),
                        onChanged: (acc) {
                          setSheetState(() => _selectedAccount = acc);
                        },
                      ),
                    ),
                  ),

                  const SizedBox(height: 18),

                  // ── Amount ──
                  Text('Amount (LKR)',
                      style: GoogleFonts.poppins(
                          fontSize: 13, color: AppTheme.textSecondary)),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _amountController,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    style: GoogleFonts.poppins(color: Colors.white, fontSize: 16),
                    decoration: InputDecoration(
                      filled: true,
                      fillColor: AppTheme.background,
                      hintText: '0.00',
                      hintStyle: GoogleFonts.poppins(color: AppTheme.textSecondary),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: const BorderSide(color: Color(0xFF33334D)),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: const BorderSide(color: Color(0xFF33334D)),
                      ),
                    ),
                  ),

                  const SizedBox(height: 24),

                  SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primary,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                        ),
                      ),
                      onPressed: () {
                        Navigator.pop(ctx);
                        _showOtpAndConfirm();
                      },
                      child: Text(
                        'Proceed to Confirmation',
                        style: GoogleFonts.poppins(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  void _showOtpAndConfirm() {
    _otpController.clear();
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
        title: Text('Security Verification',
            style: GoogleFonts.poppins(color: Colors.white)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Enter the 6-digit OTP sent to your registered mobile number to approve this payment.',
              style: GoogleFonts.poppins(
                  fontSize: 13, color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _otpController,
              keyboardType: TextInputType.number,
              maxLength: 6,
              style: GoogleFonts.spaceMono(
                color: Colors.white,
                fontSize: 20,
                letterSpacing: 4,
              ),
              decoration: InputDecoration(
                filled: true,
                fillColor: AppTheme.background,
                hintText: '123456',
                hintStyle: GoogleFonts.poppins(color: AppTheme.textSecondary),
                counterText: '',
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: const BorderSide(color: Color(0xFF33334D)),
                ),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text('Cancel',
                style: GoogleFonts.poppins(color: AppTheme.textSecondary)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.primary,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
            ),
            onPressed: () {
              Navigator.pop(ctx);
              _finalizeBillPayment();
            },
            child: Text('Confirm',
                style: GoogleFonts.poppins(
                    color: Colors.white, fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }

  Future<void> _finalizeBillPayment() async {
    setState(() => _isProcessing = true);
    await Future.delayed(const Duration(seconds: 1)); // Simulating network confirm
    if (mounted) {
      setState(() => _isProcessing = false);
      _showSnackBar('Bill payment completed successfully!');
      context.read<BankDataProvider>().loadAll();
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