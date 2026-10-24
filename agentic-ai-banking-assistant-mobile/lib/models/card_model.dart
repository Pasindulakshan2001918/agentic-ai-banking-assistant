class CardModel {
  final String id;
  final String cardType;
  final String network;
  final String cardLast4;
  final double? creditLimit;
  final double? outstandingBalance;
  final String status;

  CardModel({
    required this.id,
    required this.cardType,
    required this.network,
    required this.cardLast4,
    this.creditLimit,
    this.outstandingBalance,
    required this.status,
  });

  factory CardModel.fromJson(Map<String, dynamic> json) {
    final rawLimit = json['credit_limit'] ?? json['creditLimit'];
    final rawOutstanding = json['outstanding_balance'] ?? json['outstandingBalance'];
    final double? parsedLimit = rawLimit == null
        ? null
        : (rawLimit is num ? rawLimit.toDouble() : double.tryParse(rawLimit.toString()));
    final double? parsedOutstanding = rawOutstanding == null
        ? null
        : (rawOutstanding is num ? rawOutstanding.toDouble() : double.tryParse(rawOutstanding.toString()));

    return CardModel(
      id: json['id']?.toString() ?? '',
      cardType: (json['card_type'] ?? json['cardType'])?.toString() ?? 'DEBIT',
      network: json['network']?.toString() ?? 'VISA',
      cardLast4: (json['card_last4'] ?? json['cardLast4'])?.toString() ?? '****',
      creditLimit: parsedLimit,
      outstandingBalance: parsedOutstanding,
      status: json['status']?.toString() ?? 'ACTIVE',
    );
  }

  bool get isBlocked => status == 'BLOCKED';
  String get maskedNumber => '**** **** **** $cardLast4';
}