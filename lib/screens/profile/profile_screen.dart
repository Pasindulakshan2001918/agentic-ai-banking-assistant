import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../config/api_config.dart';
import '../../providers/accounts_provider.dart';
import '../../providers/auth_provider.dart';
import '../../theme/app_theme.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  bool _biometricEnabled = true;
  bool _notificationsEnabled = true;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Text(
          'Profile & Settings',
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: Colors.white,
          ),
        ),
      ),
      body: Consumer2<AuthProvider, BankDataProvider>(
        builder: (context, auth, bank, _) {
          final profile = bank.profile;
          final displayName = auth.customerName ?? profile?.fullName ?? 'Valued Customer';
          final customerId = auth.customerId ?? profile?.customerId ?? 'N/A';

          return ListView(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            children: [
              // ── User Header Card ──
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  gradient: AppTheme.primaryGradient,
                  borderRadius: BorderRadius.circular(22),
                  boxShadow: [
                    BoxShadow(
                      color: AppTheme.primary.withOpacity(0.25),
                      blurRadius: 18,
                      offset: const Offset(0, 8),
                    ),
                  ],
                ),
                child: Row(
                  children: [
                    Container(
                      width: 60,
                      height: 60,
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.2),
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white38, width: 2),
                      ),
                      child: Center(
                        child: Text(
                          (displayName.isNotEmpty ? displayName[0] : 'U').toUpperCase(),
                          style: GoogleFonts.poppins(
                            fontSize: 26,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Flexible(
                                child: Text(
                                  displayName,
                                  style: GoogleFonts.poppins(
                                    fontSize: 18,
                                    fontWeight: FontWeight.w700,
                                    color: Colors.white,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const SizedBox(width: 6),
                              const Icon(Icons.verified_rounded,
                                  color: Color(0xFF64FFDA), size: 18),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Customer ID: $customerId',
                            style: GoogleFonts.poppins(
                              fontSize: 12,
                              color: Colors.white70,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            profile?.mobile ?? '+94 77 123 4567',
                            style: GoogleFonts.poppins(
                              fontSize: 12,
                              color: Colors.white60,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 28),

              // ── Security Section ──
              _buildSectionHeader('Security & Access'),
              _buildSettingTile(
                icon: Icons.fingerprint_rounded,
                title: 'Biometric Login',
                subtitle: 'Enable Fingerprint or Face ID',
                trailing: Switch.adaptive(
                  value: _biometricEnabled,
                  activeColor: AppTheme.primary,
                  onChanged: (val) => setState(() => _biometricEnabled = val),
                ),
              ),
              _buildSettingTile(
                icon: Icons.security_rounded,
                title: 'Two-Factor Authentication',
                subtitle: 'SMS OTP & Token verification active',
                trailing: const Icon(Icons.check_circle_rounded, color: AppTheme.success, size: 22),
              ),
              _buildSettingTile(
                icon: Icons.lock_outline_rounded,
                title: 'Change Password / PIN',
                subtitle: 'Keep your digital banking safe',
                trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 16, color: AppTheme.textSecondary),
                onTap: () => _showComingSoon('Password update service'),
              ),

              const SizedBox(height: 24),

              // ── Preferences Section ──
              _buildSectionHeader('Preferences'),
              _buildSettingTile(
                icon: Icons.language_rounded,
                title: 'Language / භාෂාව / மொழி',
                subtitle: 'English (Default)',
                trailing: const Icon(Icons.arrow_forward_ios_rounded, size: 16, color: AppTheme.textSecondary),
                onTap: _showLanguageSelector,
              ),
              _buildSettingTile(
                icon: Icons.notifications_outlined,
                title: 'Push Notifications',
                subtitle: 'Instant alerts on deposits & debits',
                trailing: Switch.adaptive(
                  value: _notificationsEnabled,
                  activeColor: AppTheme.primary,
                  onChanged: (val) => setState(() => _notificationsEnabled = val),
                ),
              ),

              const SizedBox(height: 24),

              // ── Server & Developer Settings ──
              _buildSectionHeader('System & Server Configuration'),
              _buildSettingTile(
                icon: Icons.dns_rounded,
                title: 'API Endpoints',
                subtitle: 'Bank: ${ApiConfig.bankApiBase}',
                trailing: const Icon(Icons.tune_rounded, size: 20, color: AppTheme.primary),
                onTap: _showServerConfigDialog,
              ),
              _buildSettingTile(
                icon: Icons.smart_toy_outlined,
                title: 'AI Agent Gateway',
                subtitle: 'Agent: ${ApiConfig.agentGatewayBase}',
                trailing: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppTheme.success.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    'CONNECTED',
                    style: GoogleFonts.poppins(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: AppTheme.success,
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 32),

              // ── Logout Button ──
              SizedBox(
                width: double.infinity,
                height: 52,
                child: OutlinedButton.icon(
                  style: OutlinedButton.styleFrom(
                    side: const BorderSide(color: AppTheme.error),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  icon: const Icon(Icons.logout_rounded, color: AppTheme.error),
                  label: Text(
                    'Sign Out',
                    style: GoogleFonts.poppins(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: AppTheme.error,
                    ),
                  ),
                  onPressed: () => _handleLogout(context),
                ),
              ),

              const SizedBox(height: 20),

              Center(
                child: Text(
                  'Agentic Banking App v1.0.0 (Build 1)',
                  style: GoogleFonts.poppins(
                    fontSize: 12,
                    color: AppTheme.textSecondary.withOpacity(0.6),
                  ),
                ),
              ),

              const SizedBox(height: 40),
            ],
          );
        },
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12, left: 4),
      child: Text(
        title,
        style: GoogleFonts.poppins(
          fontSize: 15,
          fontWeight: FontWeight.w600,
          color: Colors.white70,
        ),
      ),
    );
  }

  Widget _buildSettingTile({
    required IconData icon,
    required String title,
    required String subtitle,
    Widget? trailing,
    VoidCallback? onTap,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFF222238)),
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 2),
        leading: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: AppTheme.primary.withOpacity(0.12),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(icon, color: AppTheme.primary, size: 20),
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
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: trailing,
        onTap: onTap,
      ),
    );
  }

  void _showLanguageSelector() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppTheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Select Language',
                style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.w600, color: Colors.white)),
            const SizedBox(height: 16),
            ListTile(
              title: Text('English (US)', style: GoogleFonts.poppins(color: Colors.white)),
              trailing: const Icon(Icons.check_circle_rounded, color: AppTheme.primary),
              onTap: () => Navigator.pop(ctx),
            ),
            ListTile(
              title: Text('සිංහල (Sinhala)', style: GoogleFonts.poppins(color: Colors.white)),
              onTap: () => Navigator.pop(ctx),
            ),
            ListTile(
              title: Text('தமிழ் (Tamil)', style: GoogleFonts.poppins(color: Colors.white)),
              onTap: () => Navigator.pop(ctx),
            ),
          ],
        ),
      ),
    );
  }

  void _showServerConfigDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
        title: Text('API Configuration', style: GoogleFonts.poppins(color: Colors.white)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Default settings in lib/config/api_config.dart:',
                style: GoogleFonts.poppins(fontSize: 12, color: AppTheme.textSecondary)),
            const SizedBox(height: 12),
            Text('• Android Emulator: 10.0.2.2\n• Physical Device: Use LAN IP (e.g. 192.168.1.x)\n• Bank Backend Port: 8081\n• Agent Gateway Port: 8290',
                style: GoogleFonts.spaceMono(fontSize: 11, color: Colors.white70)),
          ],
        ),
        actions: [
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.primary,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
            onPressed: () => Navigator.pop(ctx),
            child: Text('Close', style: GoogleFonts.poppins(color: Colors.white)),
          ),
        ],
      ),
    );
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('$feature will be available in the next release.', style: GoogleFonts.poppins()),
        backgroundColor: AppTheme.surface,
      ),
    );
  }

  Future<void> _handleLogout(BuildContext context) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text('Logout', style: GoogleFonts.poppins(color: Colors.white)),
        content: Text('Are you sure you want to sign out?',
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
      Navigator.pushNamedAndRemoveUntil(context, '/login', (route) => false);
    }
  }
}