import 'api_client.dart';
import 'storage_service.dart';

/// Handles login and signup through both Spring Boot (JWT) and Gateway (session).
class AuthService {
  final ApiClient _api;

  AuthService(this._api);

  /// Login: authenticates with Spring Boot for JWT, then with Gateway for chat session.
  Future<Map<String, dynamic>> login({
    required String cif,
    String lang = 'en',
  }) async {
    // 1. Login to Spring Boot -> get JWT
    final bankResp = await _api.post(
      '/auth/dev-login',
      body: {'cif': cif},
      auth: false,
    );
    final token = bankResp['accessToken'] as String;
    await StorageService.saveToken(token);
    await StorageService.saveCif(cif);

    // 2. Login to Gateway -> get session_id for AI chat
    final gwResp = await _api.gatewayPost('/login', body: {
      'cif': cif,
      'lang': lang,
    });
    final sessionId = gwResp['session_id'] as String;
    final customerName = gwResp['customer_name'] ?? '';
    await StorageService.saveSessionId(sessionId);
    await StorageService.saveCustomerName(customerName);
    await StorageService.saveLang(lang);

    return {
      'token': token,
      'sessionId': sessionId,
      'customerName': customerName,
      'lang': lang,
    };
  }

  /// Signup: creates an account via Spring Boot, then opens a Gateway session.
  Future<Map<String, dynamic>> signup({
    required String cif,
    required String name,
    required String mobile,
    String lang = 'en',
  }) async {
    // 1. Signup on Spring Boot -> creates customer + accounts, returns JWT
    final bankResp = await _api.post(
      '/auth/dev-signup',
      body: {'cif': cif, 'name': name, 'mobile': mobile, 'lang': lang},
      auth: false,
    );
    final token = bankResp['accessToken'] as String;
    await StorageService.saveToken(token);
    await StorageService.saveCif(cif);

    // 2. Authenticate session with Gateway using newly created CIF
    final gwResp = await _api.gatewayPost('/login', body: {
      'cif': cif,
      'lang': lang,
    });
    final sessionId = gwResp['session_id'] as String;
    final customerName = gwResp['customer_name'] ?? name;
    await StorageService.saveSessionId(sessionId);
    await StorageService.saveCustomerName(customerName);
    await StorageService.saveLang(lang);

    return {
      'token': token,
      'sessionId': sessionId,
      'customerName': customerName,
      'lang': lang,
    };
  }

  /// Logout: clear all stored credentials.
  Future<void> logout() async {
    await StorageService.clearAll();
  }

  /// Check if user is already logged in.
  Future<bool> isLoggedIn() async {
    final token = await StorageService.getToken();
    return token != null && token.isNotEmpty;
  }
}