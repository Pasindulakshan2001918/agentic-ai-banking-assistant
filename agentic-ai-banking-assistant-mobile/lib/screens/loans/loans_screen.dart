import 'dart:math';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../models/loan_model.dart';
import '../../providers/accounts_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/empty_state.dart';

class LoansScreen extends StatefulWidget {
  const LoansScreen({super.key});

  @override
  State<LoansScreen> createState() => _LoansScreenState();
}

class _LoansScreenState extends State<LoansScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  // EMI Calculator state
  double _calcAmount = 500000;
  double _calcRate = 12.5;
  double _calcMonths = 36;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  // Monthly EMI Calculation formula: P * r * (1 + r)^n / ((1 + r)^n - 1)
  double get _monthlyEmi {
    final p = _calcAmount;
    final r = (_calcRate / 12) / 100;
    final n = _calcMonths;
    if (r == 0) return p / n;
    final emi = (p * r * pow(1 + r, n)) / (pow(1 + r, n) - 1);
    return emi;
  }

  double get _totalPayable => _monthlyEmi * _calcMonths;
  double get _totalInterest => _totalPayable - _calcAmount;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Text(
          'Loans & Financing',
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: Colors.white,
          ),
        ),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppTheme.primary,
          labelColor: Colors.white,
          unselectedLabelColor: AppTheme.textSecondary,
          labelStyle: GoogleFonts.poppins(fontWeight: FontWeight.w600, fontSize: 13),
          tabs: const [
            Tab(text: 'My Loans'),
            Tab(text: 'EMI Calculator'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildMyLoansTab(),
          _buildEmiCalculatorTab(),
        ],
      ),
    );
  }

  Widget _buildMyLoansTab() {
    return Consumer<BankDataProvider>(
      builder: (context, bank, _) {
        if (bank.loading && bank.loans.isEmpty) {
          return const Center(
            child: CircularProgressIndicator(color: AppTheme.primary),
          );
        }

        if (bank.loans.isEmpty) {
          return RefreshIndicator(
            color: AppTheme.primary,
            onRefresh: () => bank.loadAll(),
            child: ListView(
              children: const [
                SizedBox(height: 100),
                EmptyState(
                  icon: Icons.account_balance_rounded,
                  title: 'No Active Loans',
                  subtitle: 'You do not have any active loans. Use the EMI calculator to explore flexible loan offers.',
                ),
              ],
            ),
          );
        }

        return RefreshIndicator(
          color: AppTheme.primary,
          onRefresh: () => bank.loadAll(),
          child: ListView(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            children: [
              // Banner
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFFFF9800), Color(0xFFF57C00)],
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
                        Icons.verified_user_rounded,
                        color: Colors.white,
                        size: 28,
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Pre-Approved Personal Loan',
                            style: GoogleFonts.poppins(
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Up to LKR 1,500,000 at 11.5% interest rate.',
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
                'Active Facilities (${bank.loans.length})',
                style: GoogleFonts.poppins(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 14),

              ...bank.loans.map((loan) => _buildLoanCard(loan)),

              const SizedBox(height: 40),
            ],
          ),
        );
      },
    );
  }

  Widget _buildLoanCard(LoanModel loan) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFF222238)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                loan.productType,
                style: GoogleFonts.poppins(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: AppTheme.success.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  loan.status,
                  style: GoogleFonts.poppins(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: AppTheme.success,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            'Loan No: ${loan.loanNo}',
            style: GoogleFonts.poppins(
              fontSize: 12,
              color: AppTheme.textSecondary,
            ),
          ),
          const SizedBox(height: 18),

          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Principal Amount',
                    style: GoogleFonts.poppins(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'LKR ${loan.principal.toStringAsFixed(2)}',
                    style: GoogleFonts.poppins(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                ],
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    'Remaining Balance',
                    style: GoogleFonts.poppins(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'LKR ${loan.outstanding.toStringAsFixed(2)}',
                    style: GoogleFonts.poppins(
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                      color: const Color(0xFFFFB300),
                    ),
                  ),
                ],
              ),
            ],
          ),

          const SizedBox(height: 16),

          // Progress bar
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: (loan.paidPercentage / 100).clamp(0.0, 1.0),
              minHeight: 8,
              backgroundColor: const Color(0xFF33334D),
              valueColor: const AlwaysStoppedAnimation(AppTheme.primary),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '${loan.paidPercentage.toStringAsFixed(1)}% Repaid',
                style: GoogleFonts.poppins(fontSize: 11, color: AppTheme.textSecondary),
              ),
              Text(
                'On Track',
                style: GoogleFonts.poppins(fontSize: 11, color: AppTheme.success),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildEmiCalculatorTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // EMI Result Card
          Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              gradient: AppTheme.primaryGradient,
              borderRadius: BorderRadius.circular(20),
              boxShadow: [
                BoxShadow(
                  color: AppTheme.primary.withOpacity(0.3),
                  blurRadius: 16,
                  offset: const Offset(0, 8),
                ),
              ],
            ),
            child: Column(
              children: [
                Text(
                  'Estimated Monthly EMI',
                  style: GoogleFonts.poppins(fontSize: 14, color: Colors.white70),
                ),
                const SizedBox(height: 8),
                Text(
                  'LKR ${_monthlyEmi.toStringAsFixed(2)}',
                  style: GoogleFonts.poppins(
                    fontSize: 30,
                    fontWeight: FontWeight.w800,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 16),
                const Divider(color: Colors.white24, height: 1),
                const SizedBox(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Total Interest',
                            style: GoogleFonts.poppins(fontSize: 12, color: Colors.white70)),
                        const SizedBox(height: 2),
                        Text(
                          'LKR ${_totalInterest.toStringAsFixed(2)}',
                          style: GoogleFonts.poppins(
                              fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
                        ),
                      ],
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text('Total Payable',
                            style: GoogleFonts.poppins(fontSize: 12, color: Colors.white70)),
                        const SizedBox(height: 2),
                        Text(
                          'LKR ${_totalPayable.toStringAsFixed(2)}',
                          style: GoogleFonts.poppins(
                              fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
                        ),
                      ],
                    ),
                  ],
                ),
              ],
            ),
          ),

          const SizedBox(height: 28),

          // Sliders
          Text(
            'Loan Amount: LKR ${_calcAmount.toStringAsFixed(0)}',
            style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
          ),
          Slider(
            value: _calcAmount,
            min: 50000,
            max: 5000000,
            divisions: 99,
            activeColor: AppTheme.primary,
            inactiveColor: const Color(0xFF33334D),
            onChanged: (val) => setState(() => _calcAmount = val),
          ),

          const SizedBox(height: 16),

          Text(
            'Interest Rate: ${_calcRate.toStringAsFixed(1)}% p.a.',
            style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
          ),
          Slider(
            value: _calcRate,
            min: 5.0,
            max: 25.0,
            divisions: 40,
            activeColor: AppTheme.primary,
            inactiveColor: const Color(0xFF33334D),
            onChanged: (val) => setState(() => _calcRate = val),
          ),

          const SizedBox(height: 16),

          Text(
            'Tenure: ${_calcMonths.toInt()} Months (${(_calcMonths / 12).toStringAsFixed(1)} Yrs)',
            style: GoogleFonts.poppins(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
          ),
          Slider(
            value: _calcMonths,
            min: 6,
            max: 84,
            divisions: 13,
            activeColor: AppTheme.primary,
            inactiveColor: const Color(0xFF33334D),
            onChanged: (val) => setState(() => _calcMonths = val),
          ),

          const SizedBox(height: 32),

          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primary,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(
                      'Loan inquiry submitted. Our agent will contact you shortly.',
                      style: GoogleFonts.poppins(),
                    ),
                    backgroundColor: AppTheme.surface,
                    behavior: SnackBarBehavior.floating,
                  ),
                );
              },
              child: Text(
                'Apply for this Loan',
                style: GoogleFonts.poppins(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}