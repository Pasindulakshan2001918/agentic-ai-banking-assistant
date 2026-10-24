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
    final rawDue = json['amountDue'];
    final double? parsedDue = rawDue == null
        ? null
        : (rawDue is num ? rawDue.toDouble() : double.tryParse(rawDue.toString()));

    return BillerModel(
      id: json['id']?.toString() ?? '',
      nickname: json['nickname']?.toString(),
      billerName: (json['billerName'] ?? json['name'])?.toString() ?? '',
      referenceNo: json['referenceNo']?.toString(),
      amountDue: parsedDue,
      dueDate: json['dueDate']?.toString(),
      amountDueFormatted: json['amountDueFormatted']?.toString(),
    );
  }
}