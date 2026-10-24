import 'dart:math';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
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
  String? _selectedAccountId;
  String? _selectedPayeeId;
  bool _isProcessing = false;
  String? _error;

  @override
  void dispose() {
    _amountController.dispose();
    _remarkController.dispose();
    super.dispose();
  }

  Future<void> _executeTransfer() async {
    if (_selectedAccountId == null || _selectedPayeeId == null) {
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
        fromAccountId: _selectedAccountId!,
        toPayeeId: _selectedPayeeId!,
        amount: amount,
        remark: _remarkController.text.isNotEmpty ? _remarkController.text : null,
      );

      final actionId = result['pendingActionId']?.toString() ?? result['actionId']?.toString() ?? '';
      final confirmToken = result['confirmationToken']?.toString() ?? '';
      final idemKey = 'txn-${Random().nextInt(999999)}';

      final confirmRes = await bank.confirmAction(
        actionId: actionId,
        confirmationToken: confirmToken,
        otp: '',
        idempotencyKey: idemKey,
      );

      if (mounted) {
        final txnRef = confirmRes['transaction']?['txnRef'] ?? confirmRes['transactionReference'] ?? confirmRes['reference'] ?? 'Success';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Transfer successful! Ref: $txnRef', style: GoogleFonts.poppins()),
            backgroundColor: AppTheme.success,
          ),
        );
        _amountController.clear();
        _remarkController.clear();
        setState(() => _isProcessing = false);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _isProcessing = false;
        });
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
                    child: DropdownButton<String>(
                      value: bank.accounts.any((a) => a.id == _selectedAccountId) ? _selectedAccountId : null,
                      isExpanded: true,
                      dropdownColor: AppTheme.surfaceLight,
                      hint: Text('Select account', style: GoogleFonts.poppins(color: AppTheme.textSecondary)),
                      items: bank.accounts.map((a) {
                        return DropdownMenuItem<String>(
                          value: a.id,
                          child: Text(
                            '${a.nickname} (${a.masked}) - ${a.balanceFormatted}',
                            style: GoogleFonts.poppins(fontSize: 13, color: Colors.white),
                          ),
                        );
                      }).toList(),
                      onChanged: (v) => setState(() => _selectedAccountId = v),
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
                    child: DropdownButton<String>(
                      value: bank.payees.any((p) => p.id == _selectedPayeeId) ? _selectedPayeeId : null,
                      isExpanded: true,
                      dropdownColor: AppTheme.surfaceLight,
                      hint: Text('Select payee', style: GoogleFonts.poppins(color: AppTheme.textSecondary)),
                      items: bank.payees.map((p) {
                        return DropdownMenuItem<String>(
                          value: p.id,
                          child: Text(
                            '${p.nickname} - ${p.maskedAccount}',
                            style: GoogleFonts.poppins(fontSize: 13, color: Colors.white),
                          ),
                        );
                      }).toList(),
                      onChanged: (v) => setState(() => _selectedPayeeId = v),
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
                  onPressed: _isProcessing ? null : _executeTransfer,
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
