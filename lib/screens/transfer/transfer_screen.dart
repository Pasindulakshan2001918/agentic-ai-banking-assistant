import 'dart:math';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../models/account_model.dart';
import '../../models/payee_model.dart';
import '../../providers/accounts_provider.dart';
import '../../theme/app_theme.dart';

class TransferScreen extends StatefulWidget {
  const TransferScreen({super.key});

  @override
  State<TransferScreen> createState() => _TransferScreenState();
}

class _TransferScreenState extends State<TransferScreen> {
  final _amountController = TextEditingController();
  final _remarkController = TextEditingController();
  AccountModel? _selectedAccount;
  PayeeModel? _selectedPayee;
  bool _isProcessing = false;
  String? _error;

  // Transfer flow states
  Map<String, dynamic>? _prepareResult;
  bool _showOtpDialog = false;
  final _otpController = TextEditingController();

  @override
  void dispose() {
    _amountController.dispose();
    _remarkController.dispose();
    _otpController.dispose();
    super.dispose();
  }

  Future<void> _prepareTransfer() async {
    if (_selectedAccount == null || _selectedPayee == null) {
      setState(() => _error = 'Please select an account and payee.');
      return;
    }
    final amount = double.tryParse(_amountController.text);
    if (amount == null || amount <= 0) {
      setState(() => _error = 'Enter a valid amount.');
      return;
    }

    setState(() {
      _isProcessing = true;
      _error = null;
    });

    try {
      final bank = context.read<BankDataProvider>();
      final result = await bank.prepareTransfer(
        fromAccountId: _selectedAccount!.id,
        toPayeeId: _selectedPayee!.id,
        amount: amount,
        remark: _remarkController.text.isNotEmpty ? _remarkController.text : null,
      );
      setState(() {
        _prepareResult = result;
        _showOtpDialog = true;
        _isProcessing = false;
      });
      _showOtpSheet();
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isProcessing = false;
      });
    }
  }

  void _showOtpSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppTheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.only(
            left: 28,
            right: 28,
            top: 28,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 28,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: AppTheme.divider,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(height: 24),
              const Icon(Icons.lock_outline_rounded, color: AppTheme.accent, size: 48),
              const SizedBox(height: 16),
              Text(
                'Enter OTP',
                style: GoogleFonts.poppins(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Check your console/logs for the OTP code',
                style: GoogleFonts.poppins(fontSize: 13, color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 24),
              TextField(
                controller: _otpController,
                keyboardType: TextInputType.number,
                textAlign: TextAlign.center,
                style: GoogleFonts.poppins(
                  fontSize: 28,
                  fontWeight: FontWeight.w700,
                  color: Colors.white,
                  letterSpacing: 12,
                ),
                decoration: const InputDecoration(hintText: '------'),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: () => _confirmTransfer(ctx),
                child: const Text('Confirm Transfer'),
              ),
            ],
          ),
        );
      },
    );
  }

  Future<void> _confirmTransfer(BuildContext sheetCtx) async {
    if (_otpController.text.isEmpty || _prepareResult == null) return;

    try {
      final bank = context.read<BankDataProvider>();
      final actionId = _prepareResult!['actionId']?.toString() ?? '';
      final confirmToken = _prepareResult!['confirmationToken']?.toString() ?? '';
      final idemKey = 'txn-${Random().nextInt(999999)}';

      await bank.confirmAction(
        actionId: actionId,
        confirmationToken: confirmToken,
        otp: _otpController.text,
        idempotencyKey: idemKey,
      );

      if (sheetCtx.mounted) Navigator.pop(sheetCtx);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Transfer successful!', style: GoogleFonts.poppins()),
            backgroundColor: AppTheme.success,
          ),
        );
        _amountController.clear();
        _remarkController.clear();
        _otpController.clear();
        setState(() => _prepareResult = null);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error: $e', style: GoogleFonts.poppins()),
            backgroundColor: AppTheme.error,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Transfer'),
        automaticallyImplyLeading: false,
      ),
      body: Consumer<BankDataProvider>(
        builder: (context, bank, _) {
          return SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // From account
                Text('From Account', style: GoogleFonts.poppins(
                  fontSize: 14, fontWeight: FontWeight.w500, color: AppTheme.textSecondary,
                )),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  decoration: BoxDecoration(
                    color: AppTheme.surfaceLight,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: AppTheme.divider),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<AccountModel>(
                      value: _selectedAccount,
                      isExpanded: true,
                      dropdownColor: AppTheme.surfaceLight,
                      hint: Text('Select account', style: GoogleFonts.poppins(color: AppTheme.textSecondary)),
                      items: bank.accounts.map((a) {
                        return DropdownMenuItem(
                          value: a,
                          child: Text(
                            '${a.nickname} (${a.masked}) - ${a.balanceFormatted}',
                            style: GoogleFonts.poppins(fontSize: 13, color: Colors.white),
                          ),
                        );
                      }).toList(),
                      onChanged: (v) => setState(() => _selectedAccount = v),
                    ),
                  ),
                ),
                const SizedBox(height: 20),

                // To payee
                Text('To Payee', style: GoogleFonts.poppins(
                  fontSize: 14, fontWeight: FontWeight.w500, color: AppTheme.textSecondary,
                )),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  decoration: BoxDecoration(
                    color: AppTheme.surfaceLight,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: AppTheme.divider),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<PayeeModel>(
                      value: _selectedPayee,
                      isExpanded: true,
                      dropdownColor: AppTheme.surfaceLight,
                      hint: Text('Select payee', style: GoogleFonts.poppins(color: AppTheme.textSecondary)),
                      items: bank.payees.map((p) {
                        return DropdownMenuItem(
                          value: p,
                          child: Text(
                            '${p.nickname} - ${p.maskedAccount}',
                            style: GoogleFonts.poppins(fontSize: 13, color: Colors.white),
                          ),
                        );
                      }).toList(),
                      onChanged: (v) => setState(() => _selectedPayee = v),
                    ),
                  ),
                ),
                const SizedBox(height: 20),

                // Amount
                TextField(
                  controller: _amountController,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  style: GoogleFonts.poppins(fontSize: 24, fontWeight: FontWeight.w600, color: Colors.white),
                  decoration: InputDecoration(
                    labelText: 'Amount (LKR)',
                    prefixText: 'LKR ',
                    prefixStyle: GoogleFonts.poppins(fontSize: 18, color: AppTheme.textSecondary),
                  ),
                ),
                const SizedBox(height: 16),

                // Remark
                TextField(
                  controller: _remarkController,
                  style: GoogleFonts.poppins(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Remark (optional)'),
                ),
                const SizedBox(height: 24),

                if (_error != null)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: Text(_error!, style: GoogleFonts.poppins(color: AppTheme.error, fontSize: 13)),
                  ),

                ElevatedButton(
                  onPressed: _isProcessing ? null : _prepareTransfer,
                  child: _isProcessing
                      ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                      : const Text('Send Money'),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
