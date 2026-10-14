import 'package:flutter/material.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../services/storage_service.dart';

enum AuthStatus { initial, loading, authenticated, unauthenticated, error }

class AuthProvider extends ChangeNotifier {
  final AuthService _authService;

  AuthStatus _status = AuthStatus.initial;
  String? _customerId;
  String? _customerName;
  String? _sessionId;
  String? _errorMessage;
  String _lang = 'en';

  AuthProvider(ApiClient api) : _authService = AuthService(api);

  AuthStatus get status => _status;
  String? get customerId => _customerId;
  String? get customerName => _customerName;
  String? get sessionId => _sessionId;
  String? get errorMessage => _errorMessage;
  String get lang => _lang;

  /// Check if user has a saved session.
  Future<void> checkAuth() async {
    _status = AuthStatus.loading;
    notifyListeners();

    final loggedIn = await _authService.isLoggedIn();
    if (loggedIn) {
      _customerId = await StorageService.getCif();
      _customerName = await StorageService.getCustomerName();
      _sessionId = await StorageService.getSessionId();
      _lang = await StorageService.getLang() ?? 'en';
      _status = AuthStatus.authenticated;
    } else {
      _status = AuthStatus.unauthenticated;
    }
    notifyListeners();
  }

  /// Login with CIF.
  Future<bool> login({required String cif, String lang = 'en'}) async {
    _status = AuthStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await _authService.login(cif: cif, lang: lang);
      _customerId = cif;
      _customerName = result['customerName'];
      _sessionId = result['sessionId'];
      _lang = lang;
      _status = AuthStatus.authenticated;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      _status = AuthStatus.error;
      notifyListeners();
      return false;
    }
  }

  /// Signup with full details.
  Future<bool> signup({
    required String cif,
    required String name,
    required String mobile,
    String lang = 'en',
  }) async {
    _status = AuthStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await _authService.signup(
        cif: cif,
        name: name,
        mobile: mobile,
        lang: lang,
      );
      _customerId = cif;
      _customerName = result['customerName'];
      _sessionId = result['sessionId'];
      _lang = lang;
      _status = AuthStatus.authenticated;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      _status = AuthStatus.error;
      notifyListeners();
      return false;
    }
  }

  /// Logout.
  Future<void> logout() async {
    await _authService.logout();
    _customerId = null;
    _customerName = null;
    _sessionId = null;
    _status = AuthStatus.unauthenticated;
    notifyListeners();
  }
}