import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../models/payee_model.dart';
import '../../providers/accounts_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/empty_state.dart';

class PayeesScreen extends StatefulWidget {
  const PayeesScreen({super.key});

  @override
  State<PayeesScreen> createState() => _PayeesScreenState();
}

class _PayeesScreenState extends State<PayeesScreen> {
  final _searchController = TextEditingController();
  String _searchQuery = '';

  @override
  void dispose() {
    _searchController.dispose();
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
          'Saved Payees',
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: Colors.white,
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppTheme.primary,
        icon: const Icon(Icons.person_add_rounded, color: Colors.white),
        label: Text('Add Payee',
            style: GoogleFonts.poppins(fontWeight: FontWeight.w600, color: Colors.white)),
        onPressed: _showAddPayeeSheet,
      ),
      body: Consumer<BankDataProvider>(
        builder: (context, bank, _) {
          if (bank.loading && bank.payees.isEmpty) {
            return const Center(
              child: CircularProgressIndicator(color: AppTheme.primary),
            );
          }

          final filteredPayees = bank.payees.where((p) {
            final query = _searchQuery.toLowerCase();
            return p.nickname.toLowerCase().contains(query) ||
                p.beneficiaryName.toLowerCase().contains(query) ||
                p.bankName.toLowerCase().contains(query);
          }).toList();

          return RefreshIndicator(
            color: AppTheme.primary,
            onRefresh: () => bank.loadAll(),
            child: ListView(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              children: [
                // ── Search Field ──
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14),
                  decoration: BoxDecoration(
                    color: AppTheme.surface,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: const Color(0xFF222238)),
                  ),
                  child: TextField(
                    controller: _searchController,
                    style: GoogleFonts.poppins(color: Colors.white, fontSize: 14),
                    onChanged: (val) => setState(() => _searchQuery = val),
                    decoration: InputDecoration(
                      icon: const Icon(Icons.search_rounded, color: AppTheme.textSecondary),
                      hintText: 'Search by nickname, name or bank...',
                      hintStyle: GoogleFonts.poppins(color: AppTheme.textSecondary, fontSize: 13),
                      border: InputBorder.none,
                      suffixIcon: _searchQuery.isNotEmpty
                          ? IconButton(
                              icon: const Icon(Icons.clear, color: AppTheme.textSecondary, size: 18),
                              onPressed: () {
                                _searchController.clear();
                                setState(() => _searchQuery = '');
                              },
                            )
                          : null,
                    ),
                  ),
                ),

                const SizedBox(height: 20),

                if (filteredPayees.isEmpty) ...[
                  const SizedBox(height: 60),
                  EmptyState(
                    icon: Icons.people_outline_rounded,
                    title: _searchQuery.isEmpty ? 'No Payees Saved' : 'No Results Found',
                    subtitle: _searchQuery.isEmpty
                        ? 'Add frequent recipients to transfer funds in seconds.'
                        : 'No payee matches your search query.',
                  ),
                ] else ...[
                  Text(
                    'All Payees (${filteredPayees.length})',
                    style: GoogleFonts.poppins(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 12),
                  ...filteredPayees.map((payee) => _buildPayeeTile(payee)),
                ],

                const SizedBox(height: 80),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildPayeeTile(PayeeModel payee) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFF222238)),
      ),
      child: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF00BCD4), Color(0xFF0097A7)],
              ),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Center(
              child: Text(
                (payee.nickname.isNotEmpty ? payee.nickname[0] : 'P').toUpperCase(),
                style: GoogleFonts.poppins(
                  fontSize: 18,
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
                  payee.nickname,
                  style: GoogleFonts.poppins(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
                Text(
                  payee.beneficiaryName,
                  style: GoogleFonts.poppins(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
                  ),
                ),
                Text(
                  '${payee.bankName} · ${payee.maskedAccount}',
                  style: GoogleFonts.poppins(
                    fontSize: 11,
                    color: AppTheme.textSecondary.withOpacity(0.7),
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.send_rounded, color: AppTheme.primary),
            tooltip: 'Transfer',
            onPressed: () {
              Navigator.pop(context);
            },
          ),
        ],
      ),
    );
  }

  static const List<String> sriLankanBanks = [
  'Amana Bank PLC',
  'Bank of Ceylon',
  'Bank of China Limited',
  'Cargills Bank PLC',
  'Citibank, N.A.',
  'Commercial Bank of Ceylon PLC',
  'Deutsche Bank AG',
  'DFCC Bank PLC',
  'Habib Bank Limited',
  'Hatton National Bank PLC (HNB)',
  'Indian Bank',
  'Indian Overseas Bank',
  'MCB Bank Limited',
  'National Development Bank PLC (NDB)',
  'Nations Trust Bank PLC (NTB)',
  'Pan Asia Banking Corporation PLC',
  "People's Bank",
  'Public Bank Berhad',
  'Sampath Bank PLC',
  'Seylan Bank PLC',
  'Standard Chartered Bank',
  'State Bank of India',
  'The Hongkong and Shanghai Banking Corporation Ltd (HSBC)',
  'Union Bank of Colombo PLC',
];

  void _showAddPayeeSheet() {
    final nameCtrl = TextEditingController();
    final accCtrl = TextEditingController();
    String selectedBank = 'Bank of Ceylon';

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppTheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setModalState) {
            return Padding(
              padding: EdgeInsets.only(
                left: 20,
                right: 20,
                top: 24,
                bottom: MediaQuery.of(ctx).viewInsets.bottom + 24,
              ),
              child: SingleChildScrollView(
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
                      'Add Beneficiary',
                      style: GoogleFonts.poppins(
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 16),
                    _buildField('Beneficiary Name', 'e.g. John Silva', nameCtrl),
                    const SizedBox(height: 12),
                    _buildField('Account Number', 'e.g. 8001234567', accCtrl),
                    const SizedBox(height: 12),
                    Text(
                      'Select Bank',
                      style: GoogleFonts.poppins(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                    const SizedBox(height: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14),
                      decoration: BoxDecoration(
                        color: AppTheme.background,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFF33334D)),
                      ),
                      child: DropdownButtonHideUnderline(
                        child: DropdownButton<String>(
                          value: selectedBank,
                          isExpanded: true,
                          dropdownColor: AppTheme.surface,
                          icon: const Icon(Icons.keyboard_arrow_down_rounded, color: AppTheme.textSecondary),
                          style: GoogleFonts.poppins(color: Colors.white, fontSize: 13),
                          items: sriLankanBanks.map((bankName) {
                            return DropdownMenuItem<String>(
                              value: bankName,
                              child: Text(
                                bankName,
                                overflow: TextOverflow.ellipsis,
                                style: GoogleFonts.poppins(color: Colors.white, fontSize: 13),
                              ),
                            );
                          }).toList(),
                          onChanged: (val) {
                            if (val != null) {
                              setModalState(() => selectedBank = val);
                            }
                          },
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
                        onPressed: () async {
                          final name = nameCtrl.text.trim();
                          final acc = accCtrl.text.trim();
                          if (name.isEmpty || acc.isEmpty) return;
                          final messenger = ScaffoldMessenger.of(context);
                          Navigator.pop(ctx);
                          try {
                            await context.read<BankDataProvider>().addPayee(
                              nickname: name,
                              beneficiaryName: name,
                              accountNo: acc,
                              bankName: selectedBank,
                            );
                            messenger.showSnackBar(
                              SnackBar(
                                content: Text(
                                  'Beneficiary "$name" ($selectedBank) added successfully!',
                                  style: GoogleFonts.poppins(),
                                ),
                                backgroundColor: AppTheme.success,
                              ),
                            );
                          } catch (e) {
                            messenger.showSnackBar(
                              SnackBar(
                                content: Text('Failed to save payee: $e', style: GoogleFonts.poppins()),
                                backgroundColor: AppTheme.error,
                              ),
                            );
                          }
                        },
                        child: Text(
                          'Save Payee',
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
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildField(String label, String hint, TextEditingController ctrl) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: GoogleFonts.poppins(fontSize: 12, color: AppTheme.textSecondary)),
        const SizedBox(height: 6),
        TextField(
          controller: ctrl,
          style: GoogleFonts.poppins(color: Colors.white, fontSize: 14),
          decoration: InputDecoration(
            filled: true,
            fillColor: AppTheme.background,
            hintText: hint,
            hintStyle: GoogleFonts.poppins(color: AppTheme.textSecondary, fontSize: 13),
            contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Color(0xFF33334D)),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Color(0xFF33334D)),
            ),
          ),
        ),
      ],
    );
  }
}