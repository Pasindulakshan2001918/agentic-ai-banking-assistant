import 'api_client.dart';
import 'storage_service.dart';

/// Communicates with the Python AI Agent Gateway for chat.
class ChatService {
  final ApiClient _api;

  ChatService(this._api);

  /// Send a message to the AI agent and get a reply.
  Future<String> sendMessage(String message) async {
    String? sessionId = await StorageService.getSessionId();
    final token = await StorageService.getToken();
    final lang = await StorageService.getLang() ?? 'en';

    if ((sessionId == null || sessionId.isEmpty) && token != null && token.isNotEmpty) {
      try {
        final gwResp = await _api.gatewayPost('/login', body: {
          'token': token,
          'lang': lang,
        });
        sessionId = gwResp['session_id'] as String?;
        if (sessionId != null) {
          await StorageService.saveSessionId(sessionId);
        }
      } catch (_) {}
    }

    try {
      final resp = await _api.gatewayPost('/chat', body: {
        'session_id': sessionId ?? '',
        'token': token ?? '',
        'message': message,
      });
      return resp['reply'] as String? ?? 'No response from agent.';
    } on ApiException catch (e) {
      if (e.statusCode == 404 && token != null && token.isNotEmpty) {
        // Re-login to gateway to establish a fresh session and retry
        final gwResp = await _api.gatewayPost('/login', body: {
          'token': token,
          'lang': lang,
        });
        final newSid = gwResp['session_id'] as String;
        await StorageService.saveSessionId(newSid);

        final retryResp = await _api.gatewayPost('/chat', body: {
          'session_id': newSid,
          'token': token,
          'message': message,
        });
        return retryResp['reply'] as String? ?? 'No response from agent.';
      }
      rethrow;
    }
  }

  /// Get current session info from the gateway.
  Future<Map<String, dynamic>> getSessionInfo() async {
    final sessionId = await StorageService.getSessionId();
    if (sessionId == null || sessionId.isEmpty) {
      throw Exception('No chat session.');
    }
    return await _api.gatewayGet('/session/$sessionId');
  }

  /// Check if the gateway is healthy.
  Future<bool> isHealthy() async {
    try {
      final resp = await _api.gatewayGet('/health');
      return resp['status'] == 'ok';
    } catch (_) {
      return false;
    }
  }
}