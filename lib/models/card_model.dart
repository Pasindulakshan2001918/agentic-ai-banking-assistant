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
    return CardModel(
      id: json['id']?.toString() ?? '',
      cardType: json['card_type'] ?? json['cardType'] ?? 'DEBIT',
      network: json['network'] ?? 'VISA',
      cardLast4: json['card_last4'] ?? json['cardLast4'] ?? '****',
      creditLimit: (json['credit_limit'] ?? json['creditLimit'] as num?)?.toDouble(),
      outstandingBalance: (json['outstanding_balance'] ?? json['outstandingBalance'] as num?)?.toDouble(),
      status: json['status'] ?? 'ACTIVE',
    );
  }

  bool get isBlocked => status == 'BLOCKED';
  String get maskedNumber => '**** **** **** $cardLast4';
}