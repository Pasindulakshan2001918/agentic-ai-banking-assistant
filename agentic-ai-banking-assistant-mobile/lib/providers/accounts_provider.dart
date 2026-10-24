import 'dart:async';
import 'package:flutter/material.dart';
import '../models/account_model.dart';
import '../models/card_model.dart';
import '../models/loan_model.dart';
import '../models/payee_model.dart';
import '../models/biller_model.dart';
import '../models/user_model.dart';
import '../services/api_client.dart';
import '../services/bank_service.dart';
import '../services/websocket_service.dart';

/// Provides all banking data (accounts, cards, loans, payees, billers).
class BankDataProvider extends ChangeNotifier {
  final BankService _bankService;
  StreamSubscription? _wsSubscription;

  UserModel? _profile;
  List<AccountModel> _accounts = [];
  List<CardModel> _cards = [];
  List<LoanModel> _loans = [];
  List<PayeeModel> _payees = [];
  List<BillerModel> _billers = [];
  bool _loading = false;
  String? _error;

  BankDataProvider(ApiClient api) : _bankService = BankService(api) {
    _initWebSocketListener();
  }

  void _initWebSocketListener() {
    _wsSubscription = WebSocketService().stream.listen((payload) {
      final eventType = payload['event'] ?? payload['type'];
      debugPrint('[BankDataProvider] WS event received: $eventType');
      if (eventType == 'BALANCE_UPDATED' ||
          eventType == 'TRANSACTION_CREATED' ||
          eventType == 'PAYMENT_RECEIVED') {
        refreshAccounts();
      } else if (eventType == 'CARD_STATUS_CHANGED' || eventType == 'CARD_ISSUED') {
        refreshCards();
      } else if (eventType == 'CUSTOMER_UPDATED') {
        loadAll();
      }
    });
  }

  UserModel? get profile => _profile;
  List<AccountModel> get accounts => _accounts;
  List<CardModel> get cards => _cards;
  List<LoanModel> get loans => _loans;
  List<PayeeModel> get payees => _payees;
  List<BillerModel> get billers => _billers;
  bool get loading => _loading;
  String? get error => _error;

  double get totalBalance =>
      _accounts.fold(0.0, (sum, a) => sum + a.balance);

  /// Load all data for the dashboard.
  Future<void> loadAll() async {
    _loading = true;
    _error = null;
    notifyListeners();

    try {
      final results = await Future.wait([
        _bankService.getProfile().catchError((e) {
          debugPrint('getProfile error: $e');
          return UserModel(customerId: '', fullName: '', preferredLanguage: 'EN');
        }),
        _bankService.getAccounts().catchError((e) {
          debugPrint('getAccounts error: $e');
          return <AccountModel>[];
        }),
        _bankService.getCards().catchError((e) {
          debugPrint('getCards error: $e');
          return <CardModel>[];
        }),
        _bankService.getLoans().catchError((e) {
          debugPrint('getLoans error: $e');
          return <LoanModel>[];
        }),
        _bankService.getPayees().catchError((e) {
          debugPrint('getPayees error: $e');
          return <PayeeModel>[];
        }),
        _bankService.getRegisteredBillers().catchError((e) {
          debugPrint('getRegisteredBillers error: $e');
          return <BillerModel>[];
        }),
      ]);
      _profile = results[0] as UserModel;
      _accounts = results[1] as List<AccountModel>;
      _cards = results[2] as List<CardModel>;
      _loans = results[3] as List<LoanModel>;
      _payees = results[4] as List<PayeeModel>;
      _billers = results[5] as List<BillerModel>;
    } catch (e) {
      _error = e.toString();
    }

    _loading = false;
    notifyListeners();
  }

  /// Refresh just accounts.
  Future<void> refreshAccounts() async {
    try {
      _accounts = await _bankService.getAccounts();
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  /// Refresh just cards.
  Future<void> refreshCards() async {
    try {
      _cards = await _bankService.getCards();
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  /// Block a card.
  Future<bool> blockCard(String cardId) async {
    try {
      await _bankService.blockCard(cardId);
      await refreshCards();
      return true;
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      return false;
    }
  }

  /// Add a new payee.
  Future<void> addPayee({
    required String nickname,
    required String beneficiaryName,
    required String accountNo,
    String bankName = 'Bank of Ceylon',
  }) async {
    await _bankService.addPayee(
      nickname: nickname,
      beneficiaryName: beneficiaryName,
      accountNo: accountNo,
      bankName: bankName,
    );
    _payees = await _bankService.getPayees();
    notifyListeners();
  }

  /// Prepare a fund transfer.
  Future<Map<String, dynamic>> prepareTransfer({
    required String fromAccountId,
    required String toPayeeId,
    required double amount,
    String? remark,
  }) async {
    return _bankService.prepareTransfer(
      fromAccountId: fromAccountId,
      toPayeeId: toPayeeId,
      amount: amount,
      remark: remark,
    );
  }

  /// Confirm an action with OTP.
  Future<Map<String, dynamic>> confirmAction({
    required String actionId,
    required String confirmationToken,
    required String otp,
    required String idempotencyKey,
  }) async {
    final result = await _bankService.confirmAction(
      actionId: actionId,
      confirmationToken: confirmationToken,
      otp: otp,
      idempotencyKey: idempotencyKey,
    );
    await refreshAccounts();
    return result;
  }

  void clear() {
    _profile = null;
    _accounts = [];
    _cards = [];
    _loans = [];
    _payees = [];
    _billers = [];
    notifyListeners();
  }

  @override
  void dispose() {
    _wsSubscription?.cancel();
    super.dispose();
  }
}