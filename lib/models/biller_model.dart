class BillerModel {
  final String id;
  final String? nickname;
  final String billerName;
  final String? referenceNo;
  final double? amountDue;
  final String? dueDate;
  final String? amountDueFormatted;

  BillerModel({
    required this.id,
    this.nickname,
    required this.billerName,
    this.referenceNo,
    this.amountDue,
    this.dueDate,
    this.amountDueFormatted,
  });

  factory BillerModel.fromJson(Map<String, dynamic> json) {
    return BillerModel(
      id: json['id']?.toString() ?? '',
      nickname: json['nickname'],
      billerName: json['billerName'] ?? json['name'] ?? '',
      referenceNo: json['referenceNo'],
      amountDue: (json['amountDue'] as num?)?.toDouble(),
      dueDate: json['dueDate'],
      amountDueFormatted: json['amountDueFormatted'],
    );
  }
}