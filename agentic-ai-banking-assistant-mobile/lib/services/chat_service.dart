import 'api_client.dart';
import 'storage_service.dart';

/// Communicates with the Python AI Agent Gateway for chat.
class ChatService {
  final ApiClient _api;

  ChatService(this._api);

  /// Send a message to the AI agent and get a reply.
  Future<String> sendMessage(String message) async {
    final sessionId = await StorageService.getSessionId();
    if (sessionId == null || sessionId.isEmpty) {
      throw Exception('No chat session. Please log in again.');
    }
    final resp = await _api.gatewayPost('/chat', body: {
      'session_id': sessionId,
      'message': message,
    });
    return resp['reply'] as String? ?? 'No response from agent.';
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