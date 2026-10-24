import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import '../config/api_config.dart';
import 'storage_service.dart';

/// Singleton service managing a persistent, real-time WebSocket connection to Spring Boot Core API.
class WebSocketService {
  static final WebSocketService _instance = WebSocketService._internal();
  factory WebSocketService() => _instance;
  WebSocketService._internal();

  WebSocketChannel? _channel;
  StreamSubscription? _subscription;
  Timer? _pingTimer;
  Timer? _reconnectTimer;

  bool _isConnected = false;
  bool _isConnecting = false;

  final _eventController = StreamController<Map<String, dynamic>>.broadcast();

  /// Stream of incoming real-time events from the server.
  Stream<Map<String, dynamic>> get stream => _eventController.stream;

  bool get isConnected => _isConnected;

  /// Connect to the WebSocket server using the active JWT token.
  Future<void> connect([String? explicitToken]) async {
    if (_isConnected || _isConnecting) return;

    final token = explicitToken ?? await StorageService.getToken();
    if (token == null || token.isEmpty) {
      if (kDebugMode) print('[WS] No JWT token available. Skipping connection.');
      return;
    }

    _isConnecting = true;
    _reconnectTimer?.cancel();

    final wsUri = Uri.parse('${ApiConfig.wsBase}?token=${Uri.encodeComponent(token)}');

    if (kDebugMode) print('[WS] Connecting to $wsUri');

    try {
      _channel = WebSocketChannel.connect(wsUri);

      _subscription = _channel!.stream.listen(
        (data) {
          _isConnecting = false;
          _isConnected = true;
          _handleMessage(data);
        },
        onError: (error) {
          if (kDebugMode) print('[WS] Error: $error');
          _handleDisconnect();
        },
        onDone: () {
          if (kDebugMode) print('[WS] Closed');
          _handleDisconnect();
        },
        cancelOnError: true,
      );

      // Start ping heartbeat
      _pingTimer?.cancel();
      _pingTimer = Timer.periodic(const Duration(seconds: 25), (_) {
        if (_isConnected && _channel != null) {
          _channel!.sink.add('PING');
        }
      });
    } catch (e) {
      if (kDebugMode) print('[WS] Connection exception: $e');
      _handleDisconnect();
    }
  }

  void _handleMessage(dynamic raw) {
    try {
      final decoded = jsonDecode(raw.toString());
      if (decoded is Map<String, dynamic>) {
        final event = (decoded['event'] ?? decoded['type']) as String?;
        if (event == 'CONNECTED') {
          _isConnected = true;
          if (kDebugMode) print('[WS] Connection ACK confirmed by server.');
        } else if (event == 'PONG') {
          // Heartbeat received
        } else {
          if (kDebugMode) print('[WS Live Event] $event -> ${decoded['data']}');
          _eventController.add(decoded);
        }
      }
    } catch (e) {
      if (kDebugMode) print('[WS] Error decoding message: $e');
    }
  }

  void _handleDisconnect() {
    _isConnected = false;
    _isConnecting = false;
    _pingTimer?.cancel();
    _subscription?.cancel();
    _channel = null;

    // Schedule auto-reconnect if token is still stored
    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(const Duration(seconds: 4), () async {
      final token = await StorageService.getToken();
      if (token != null && token.isNotEmpty) {
        if (kDebugMode) print('[WS] Attempting auto-reconnect...');
        connect(token);
      }
    });
  }

  /// Cleanly close the socket (e.g. on user logout).
  void disconnect() {
    _reconnectTimer?.cancel();
    _pingTimer?.cancel();
    _subscription?.cancel();
    try {
      _channel?.sink.close();
    } catch (_) {}
    _channel = null;
    _isConnected = false;
    _isConnecting = false;
    if (kDebugMode) print('[WS] Disconnected cleanly.');
  }
}
