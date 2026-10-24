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
    final rawBalance = json['balance'];
    final double parsedBalance = rawBalance is num
        ? rawBalance.toDouble()
        : (double.tryParse(rawBalance?.toString() ?? '') ?? 0.0);

    return AccountModel(
      id: json['id']?.toString() ?? '',
      accountNo: json['accountNo']?.toString() ?? '',
      masked: json['masked']?.toString() ?? '',
      productType: json['productType']?.toString() ?? 'SAVINGS',
      nickname: json['nickname']?.toString() ?? '',
      currency: json['currency']?.toString() ?? 'LKR',
      balance: parsedBalance,
      status: json['status']?.toString() ?? 'ACTIVE',
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

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AccountModel && runtimeType == other.runtimeType && id == other.id;

  @override
  int get hashCode => id.hashCode;
}