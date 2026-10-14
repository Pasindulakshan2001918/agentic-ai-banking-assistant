import 'package:flutter/material.dart';
import '../models/chat_message_model.dart';
import '../services/api_client.dart';
import '../services/chat_service.dart';

/// Manages AI chat state and history.
class ChatProvider extends ChangeNotifier {
  final ChatService _chatService;

  List<ChatMessageModel> _messages = [];
  bool _isTyping = false;
  String? _error;

  ChatProvider(ApiClient api) : _chatService = ChatService(api);

  List<ChatMessageModel> get messages => _messages;
  bool get isTyping => _isTyping;
  String? get error => _error;

  /// Send a message and get the AI response.
  Future<void> sendMessage(String text) async {
    if (text.trim().isEmpty) return;

    // Add user message
    _messages.add(ChatMessageModel(
      text: text,
      sender: MessageSender.user,
    ));
    _isTyping = true;
    _error = null;
    notifyListeners();

    try {
      final reply = await _chatService.sendMessage(text);
      _messages.add(ChatMessageModel(
        text: reply,
        sender: MessageSender.bot,
      ));
    } catch (e) {
      _error = e.toString();
      _messages.add(ChatMessageModel(
        text: 'Sorry, I encountered an error. Please try again.',
        sender: MessageSender.bot,
      ));
    }

    _isTyping = false;
    notifyListeners();
  }

  /// Clear chat history.
  void clearChat() {
    _messages = [];
    _error = null;
    notifyListeners();
  }
}