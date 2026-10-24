import '../models/account_model.dart';
import '../models/card_model.dart';
import '../models/loan_model.dart';
import '../models/payee_model.dart';
import '../models/biller_model.dart';
import '../models/user_model.dart';
import 'api_client.dart';

/// All normal banking operations (accounts, cards, loans, payees, billers, transfers).
class BankService {
  final ApiClient _api;

  BankService(this._api);

  // ── Profile ───────────────────────────────────────────────────────────
  Future<UserModel> getProfile() async {
    final data = await _api.get('/me');
    return UserModel.fromJson(data);
  }

  // ── Accounts ──────────────────────────────────────────────────────────
  Future<List<AccountModel>> getAccounts() async {
    final data = await _api.get('/accounts');
    final list = data['accounts'] as List<dynamic>;
    return list.map((j) => AccountModel.fromJson(j)).toList();
  }

  // ── Cards ─────────────────────────────────────────────────────────────
  Future<List<CardModel>> getCards() async {
    final data = await _api.get('/cards');
    final list = data['cards'] as List<dynamic>;
    return list.map((j) => CardModel.fromJson(j)).toList();
  }

  Future<Map<String, dynamic>> blockCard(String cardId) async {
    return await _api.post('/cards/$cardId/block');
  }

  // ── Loans ─────────────────────────────────────────────────────────────
  Future<List<LoanModel>> getLoans() async {
    final data = await _api.get('/loans');
    final list = data['loans'] as List<dynamic>;
    return list.map((j) => LoanModel.fromJson(j)).toList();
  }

  // ── Payees ────────────────────────────────────────────────────────────
  Future<List<PayeeModel>> getPayees({String? query}) async {
    final path = query != null && query.isNotEmpty ? '/payees?q=$query' : '/payees';
    final data = await _api.get(path);
    final list = data['payees'] as List<dynamic>;
    return list.map((j) => PayeeModel.fromJson(j)).toList();
  }

  Future<Map<String, dynamic>> addPayee({
    required String nickname,
    required String beneficiaryName,
    required String accountNo,
    String bankName = 'Bank of Ceylon',
    String bankCode = '7010',
  }) async {
    return await _api.post('/payees', body: {
      'nickname': nickname,
      'beneficiaryName': beneficiaryName,
      'accountNo': accountNo,
      'bankName': bankName,
      'bankCode': bankCode,
    });
  }

  // ── Transactions ──────────────────────────────────────────────────────
  Future<List<Map<String, dynamic>>> getTransactions({int limit = 20}) async {
    final data = await _api.get('/transactions?limit=$limit');
    final list = data['transactions'] as List<dynamic>? ?? [];
    return list.cast<Map<String, dynamic>>();
  }

  // ── Billers ───────────────────────────────────────────────────────────
  Future<List<BillerModel>> getRegisteredBillers() async {
    final data = await _api.get('/billers/registered');
    final list = data['registeredBillers'] as List<dynamic>;
    return list.map((j) => BillerModel.fromJson(j)).toList();
  }

  // ── Transfer (Prepare → OTP → Confirm) ────────────────────────────────
  Future<Map<String, dynamic>> prepareTransfer({
    required String fromAccountId,
    required String toPayeeId,
    required double amount,
    String? remark,
  }) async {
    return await _api.post('/actions/prepare', body: {
      'actionType': 'FUND_TRANSFER',
      'params': {
        'fromAccountId': fromAccountId,
        'toPayeeId': toPayeeId,
        'amount': amount,
        if (remark != null) 'remark': remark,
      },
    });
  }

  Future<Map<String, dynamic>> confirmAction({
    required String actionId,
    required String confirmationToken,
    required String otp,
    required String idempotencyKey,
  }) async {
    return await _api.post(
      '/actions/$actionId/confirm',
      body: {
        'confirmationToken': confirmationToken,
        'otp': otp,
      },
      extraHeaders: {'Idempotency-Key': idempotencyKey},
    );
  }

  Future<Map<String, dynamic>> cancelAction(String actionId) async {
    return await _api.post('/actions/$actionId/cancel');
  }

  // ── Bill Payment ──────────────────────────────────────────────────────
  Future<Map<String, dynamic>> prepareBillPayment({
    required String fromAccountId,
    required String registeredBillerId,
    required double amount,
  }) async {
    return await _api.post('/actions/prepare', body: {
      'actionType': 'BILL_PAYMENT',
      'params': {
        'fromAccountId': fromAccountId,
        'registeredBillerId': registeredBillerId,
        'amount': amount,
      },
    });
  }
}