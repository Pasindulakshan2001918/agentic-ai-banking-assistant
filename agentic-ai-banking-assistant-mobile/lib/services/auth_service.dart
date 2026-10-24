import 'api_client.dart';
import 'storage_service.dart';
import 'websocket_service.dart';

/// Handles real password-based login and account registration.
class AuthService {
  final ApiClient _api;

  AuthService(this._api);

  // ── Login ──────────────────────────────────────────────────────────────────

  /// Login with username + password.
  /// 1. Calls POST /auth/login on Spring Boot → get JWT + fullName
  /// 2. Opens agent session on Python gateway using the JWT
  Future<Map<String, dynamic>> login({
    required String username,
    required String password,
    String lang = 'en',
  }) async {
    // 1. Authenticate with Spring Boot
    final bankResp = await _api.post(
      '/auth/login',
      body: {'username': username, 'password': password},
      auth: false,
    );

    final token      = bankResp['accessToken'] as String;
    final fullName   = bankResp['fullName'] as String? ?? '';
    final prefLang   = (bankResp['preferredLanguage'] as String? ?? 'EN').toLowerCase();
    final customerId = bankResp['customerId']?.toString() ?? '';

    await StorageService.saveToken(token);
    await StorageService.saveCif(customerId);
    await StorageService.saveCustomerName(fullName);
    await StorageService.saveLang(lang);

    // Connect WebSocket
    WebSocketService().connect(token);

    // 2. Open agent gateway session using the JWT (no CIF needed in production)
    final gwResp = await _api.gatewayPost('/login', body: {
      'token': token,
      'lang': lang.isNotEmpty ? lang : prefLang,
    });

    final sessionId    = gwResp['session_id'] as String;
    final customerName = gwResp['customer_name'] as String? ?? fullName;
    await StorageService.saveSessionId(sessionId);
    await StorageService.saveCustomerName(customerName);

    return {
      'token': token,
      'sessionId': sessionId,
      'customerName': customerName,
      'customerId': customerId,
      'lang': lang,
    };
  }

  // ── Register ───────────────────────────────────────────────────────────────

  /// Register by linking to an admin-created account.
  /// 1. Calls POST /auth/register on Spring Boot → get JWT
  /// 2. Opens agent gateway session
  Future<Map<String, dynamic>> register({
    required String accountNo,
    required String username,
    required String password,
    String lang = 'en',
  }) async {
    // 1. Register on Spring Boot
    final bankResp = await _api.post(
      '/auth/register',
      body: {'accountNo': accountNo, 'username': username, 'password': password},
      auth: false,
    );

    final token      = bankResp['accessToken'] as String;
    final fullName   = bankResp['fullName'] as String? ?? '';
    final customerId = bankResp['customerId']?.toString() ?? '';

    await StorageService.saveToken(token);
    await StorageService.saveCif(customerId);
    await StorageService.saveCustomerName(fullName);
    await StorageService.saveLang(lang);

    // Connect WebSocket
    WebSocketService().connect(token);

    // 2. Open agent session with the new JWT
    final gwResp = await _api.gatewayPost('/login', body: {
      'token': token,
      'lang': lang,
    });

    final sessionId    = gwResp['session_id'] as String;
    final customerName = gwResp['customer_name'] as String? ?? fullName;
    await StorageService.saveSessionId(sessionId);
    await StorageService.saveCustomerName(customerName);

    return {
      'token': token,
      'sessionId': sessionId,
      'customerName': customerName,
      'customerId': customerId,
      'lang': lang,
    };
  }

  // ── Logout ─────────────────────────────────────────────────────────────────

  Future<void> logout() async {
    WebSocketService().disconnect();
    await StorageService.clearAll();
  }

  // ── Check ──────────────────────────────────────────────────────────────────

  Future<bool> isLoggedIn() async {
    final token = await StorageService.getToken();
    return token != null && token.isNotEmpty;
  }
}