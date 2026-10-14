class AccountModel {
  final String id;
  final String accountNo;
  final String masked;
  final String productType;
  final String nickname;
  final String currency;
  final double balance;
  final String status;

  AccountModel({
    required this.id,
    required this.accountNo,
    required this.masked,
    required this.productType,
    required this.nickname,
    required this.currency,
    required this.balance,
    required this.status,
  });

  factory AccountModel.fromJson(Map<String, dynamic> json) {
    return AccountModel(
      id: json['id']?.toString() ?? '',
      accountNo: json['accountNo'] ?? '',
      masked: json['masked'] ?? '',
      productType: json['productType'] ?? 'SAVINGS',
      nickname: json['nickname'] ?? '',
      currency: json['currency'] ?? 'LKR',
      balance: (json['balance'] as num?)?.toDouble() ?? 0.0,
      status: json['status'] ?? 'ACTIVE',
    );
  }

  // Aliases for compatibility
  String get accountType => productType;
  String get maskedAccount => masked;

  String get balanceFormatted {
    final formatted = balance.toStringAsFixed(2);
    // Add thousand separators
    final parts = formatted.split('.');
    final intPart = parts[0].replaceAllMapped(
      RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
      (m) => '${m[1]},',
    );
    return '$currency $intPart.${parts[1]}';
  }
}