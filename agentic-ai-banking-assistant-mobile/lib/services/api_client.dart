import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import 'storage_service.dart';

/// Thin wrapper around [http.Client] with automatic JWT injection
/// and dual USB/Wi-Fi connection auto-detection.
class ApiClient {
  final http.Client _client = http.Client();

  Future<Map<String, String>> _headers({bool auth = true}) async {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (auth) {
      final token = await StorageService.getToken();
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
      }
    }
    return headers;
  }

  /// GET against the Spring Boot core API.
  Future<dynamic> get(String path, {bool auth = true}) async {
    final h = await _headers(auth: auth);
    final hosts = [ApiConfig.activeHost, ...ApiConfig.candidateHosts.where((x) => x != ApiConfig.activeHost)];
    for (final host in hosts) {
      try {
        final uri = Uri.parse('http://$host:8081/api/v1$path');
        final resp = await _client.get(uri, headers: h).timeout(const Duration(seconds: 8));
        ApiConfig.activeHost = host;
        return _handleResponse(resp);
      } on ApiException {
        rethrow; // Server replied with valid response (e.g. 400/401/404)
      } catch (e) {
        if (host == hosts.last) rethrow;
      }
    }
  }

  /// POST against the Spring Boot core API.
  Future<dynamic> post(
    String path, {
    Map<String, dynamic>? body,
    bool auth = true,
    Map<String, String>? extraHeaders,
  }) async {
    final h = await _headers(auth: auth);
    if (extraHeaders != null) h.addAll(extraHeaders);
    final encoded = body != null ? jsonEncode(body) : null;
    final hosts = [ApiConfig.activeHost, ...ApiConfig.candidateHosts.where((x) => x != ApiConfig.activeHost)];

    for (final host in hosts) {
      try {
        final uri = Uri.parse('http://$host:8081/api/v1$path');
        final resp = await _client.post(uri, headers: h, body: encoded).timeout(const Duration(seconds: 8));
        ApiConfig.activeHost = host;
        return _handleResponse(resp);
      } on ApiException {
        rethrow; // Server replied with valid response (e.g. 400 CIF already exists)
      } catch (e) {
        if (host == hosts.last) rethrow;
      }
    }
  }

  /// GET against the Python agent gateway.
  Future<dynamic> gatewayGet(String path) async {
    final token = await StorageService.getToken();
    final headers = <String, String>{'Accept': 'application/json'};
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }

    final hosts = [ApiConfig.activeHost, ...ApiConfig.candidateHosts.where((x) => x != ApiConfig.activeHost)];
    for (final host in hosts) {
      try {
        final uri = Uri.parse('http://$host:8290$path');
        final resp = await _client.get(uri, headers: headers).timeout(const Duration(seconds: 45));
        ApiConfig.activeHost = host;
        return _handleResponse(resp);
      } on ApiException {
        rethrow;
      } catch (e) {
        if (host == hosts.last) rethrow;
      }
    }
  }

  /// POST against the Python agent gateway.
  Future<dynamic> gatewayPost(String path, {Map<String, dynamic>? body}) async {
    final token = await StorageService.getToken();
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }

    final encoded = body != null ? jsonEncode(body) : null;
    final hosts = [ApiConfig.activeHost, ...ApiConfig.candidateHosts.where((x) => x != ApiConfig.activeHost)];

    for (final host in hosts) {
      try {
        final uri = Uri.parse('http://$host:8290$path');
        final resp = await _client.post(
          uri,
          headers: headers,
          body: encoded,
        ).timeout(const Duration(seconds: 45));
        ApiConfig.activeHost = host;
        return _handleResponse(resp);
      } on ApiException {
        rethrow;
      } catch (e) {
        if (host == hosts.last) rethrow;
      }
    }
  }

  dynamic _handleResponse(http.Response resp) {
    if (resp.statusCode >= 200 && resp.statusCode < 300) {
      if (resp.body.isEmpty) return {};
      return jsonDecode(resp.body);
    } else {
      String message = 'Request failed (${resp.statusCode})';
      try {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map) {
          if (decoded['error'] is Map) {
            message = decoded['error']['message'] ?? message;
          } else {
            message = decoded['detail'] ?? decoded['message'] ?? decoded['error'] ?? message;
          }
        }
      } catch (_) {}
      throw ApiException(resp.statusCode, message);
    }
  }

  void dispose() => _client.close();
}

class ApiException implements Exception {
  final int statusCode;
  final String message;
  ApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}