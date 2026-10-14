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
    return LoanModel(
      id: json['id']?.toString() ?? '',
      loanNo: json['loan_no'] ?? json['loanNo'] ?? '',
      productType: json['product_type'] ?? json['productType'] ?? 'LOAN',
      principal: (json['principal'] as num?)?.toDouble() ?? 0.0,
      outstanding: (json['outstanding'] as num?)?.toDouble() ?? 0.0,
      status: json['status'] ?? 'ACTIVE',
    );
  }

  double get paidPercentage => principal > 0 ? ((principal - outstanding) / principal) * 100 : 0;
}