/// Central configuration for API endpoints.
/// Dynamically selects between USB direct (127.0.0.1 via adb reverse)
/// and Wi-Fi LAN (192.168.1.6) or emulator (10.0.2.2).
class ApiConfig {
  ApiConfig._();

  static String activeHost = '127.0.0.1';
  static const List<String> candidateHosts = ['127.0.0.1', '192.168.1.6', '10.0.2.2'];

  /// Spring Boot core API (port 8081)
  static String get bankApiBase => 'http://$activeHost:8081/api/v1';

  /// Python AI Agent gateway (port 8290)
  static String get agentGatewayBase => 'http://$activeHost:8290';

  /// WebSocket live event endpoint (port 8081)
  static String get wsBase => 'ws://$activeHost:8081/api/v1/ws';

  static const Duration connectTimeout = Duration(seconds: 4);
  static const Duration receiveTimeout = Duration(seconds: 30);
}