class PayeeModel {
  final String id;
  final String nickname;
  final String beneficiaryName;
  final String bankName;
  final String maskedAccount;
  final String? addedAt;

  PayeeModel({
    required this.id,
    required this.nickname,
    required this.beneficiaryName,
    required this.bankName,
    required this.maskedAccount,
    this.addedAt,
  });

  factory PayeeModel.fromJson(Map<String, dynamic> json) {
    return PayeeModel(
      id: json['id']?.toString() ?? '',
      nickname: json['nickname'] ?? '',
      beneficiaryName: json['beneficiaryName'] ?? '',
      bankName: json['bankName'] ?? '',
      maskedAccount: json['maskedAccount'] ?? '',
      addedAt: json['addedAt'],
    );
  }
}