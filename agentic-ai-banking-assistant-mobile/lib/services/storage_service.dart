import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Secure local storage for JWT token and session data.
class StorageService {
  static const _storage = FlutterSecureStorage();

  static const _keyToken = 'jwt_token';
  static const _keyCif = 'cif';
  static const _keySessionId = 'session_id';
  static const _keyCustomerName = 'customer_name';
  static const _keyLang = 'lang';

  // ── Token ───────────────────────────────────────────────────────────
  static Future<void> saveToken(String token) =>
      _storage.write(key: _keyToken, value: token);

  static Future<String?> getToken() =>
      _storage.read(key: _keyToken);

  static Future<void> deleteToken() =>
      _storage.delete(key: _keyToken);

  // ── CIF ─────────────────────────────────────────────────────────────
  static Future<void> saveCif(String cif) =>
      _storage.write(key: _keyCif, value: cif);

  static Future<String?> getCif() =>
      _storage.read(key: _keyCif);

  // ── Session ID (for AI chat) ────────────────────────────────────────
  static Future<void> saveSessionId(String sid) =>
      _storage.write(key: _keySessionId, value: sid);

  static Future<String?> getSessionId() =>
      _storage.read(key: _keySessionId);

  // ── Customer Name ───────────────────────────────────────────────────
  static Future<void> saveCustomerName(String name) =>
      _storage.write(key: _keyCustomerName, value: name);

  static Future<String?> getCustomerName() =>
      _storage.read(key: _keyCustomerName);

  // ── Language ────────────────────────────────────────────────────────
  static Future<void> saveLang(String lang) =>
      _storage.write(key: _keyLang, value: lang);

  static Future<String?> getLang() =>
      _storage.read(key: _keyLang);

  // ── Clear all ───────────────────────────────────────────────────────
  static Future<void> clearAll() => _storage.deleteAll();
}