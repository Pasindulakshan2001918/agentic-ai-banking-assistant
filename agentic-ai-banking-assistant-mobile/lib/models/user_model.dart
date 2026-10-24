class UserModel {
  final String customerId;
  final String fullName;
  final String preferredLanguage;
  final String? mobile;

  UserModel({
    required this.customerId,
    required this.fullName,
    required this.preferredLanguage,
    this.mobile,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      customerId: json['customerId']?.toString() ?? '',
      fullName: json['fullName'] ?? '',
      preferredLanguage: json['preferredLanguage'] ?? 'EN',
      mobile: json['mobile'],
    );
  }
}