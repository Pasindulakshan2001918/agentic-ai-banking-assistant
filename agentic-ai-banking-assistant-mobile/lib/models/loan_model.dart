class LoanModel {
  final String id;
  final String loanNo;
  final String productType;
  final double principal;
  final double outstanding;
  final String status;

  LoanModel({
    required this.id,
    required this.loanNo,
    required this.productType,
    required this.principal,
    required this.outstanding,
    required this.status,
  });

  factory LoanModel.fromJson(Map<String, dynamic> json) {
    final rawPrincipal = json['principal'];
    final rawOutstanding = json['outstanding'];
    final double parsedPrincipal = rawPrincipal is num
        ? rawPrincipal.toDouble()
        : (double.tryParse(rawPrincipal?.toString() ?? '') ?? 0.0);
    final double parsedOutstanding = rawOutstanding is num
        ? rawOutstanding.toDouble()
        : (double.tryParse(rawOutstanding?.toString() ?? '') ?? 0.0);

    return LoanModel(
      id: json['id']?.toString() ?? '',
      loanNo: (json['loan_no'] ?? json['loanNo'])?.toString() ?? '',
      productType: (json['product_type'] ?? json['productType'])?.toString() ?? 'LOAN',
      principal: parsedPrincipal,
      outstanding: parsedOutstanding,
      status: json['status']?.toString() ?? 'ACTIVE',
    );
  }

  double get paidPercentage => principal > 0 ? ((principal - outstanding) / principal) * 100 : 0;
}