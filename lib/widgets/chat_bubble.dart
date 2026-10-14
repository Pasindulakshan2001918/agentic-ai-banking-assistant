import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../models/chat_message_model.dart';
import '../theme/app_theme.dart';

/// Chat message bubble for AI chat screen.
class ChatBubble extends StatelessWidget {
  final ChatMessageModel message;

  const ChatBubble({super.key, required this.message});

  bool get _isUser => message.sender == MessageSender.user;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: _isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.78,
        ),
        margin: EdgeInsets.only(
          left: _isUser ? 48 : 0,
          right: _isUser ? 0 : 48,
          bottom: 8,
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            if (!_isUser) ...[
              Container(
                width: 32,
                height: 32,
                margin: const EdgeInsets.only(right: 8),
                decoration: BoxDecoration(
                  gradient: AppTheme.accentGradient,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(Icons.smart_toy_rounded, size: 18, color: Colors.white),
              ),
            ],
            Flexible(
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: _isUser ? AppTheme.chatUser : AppTheme.chatBot,
                  borderRadius: BorderRadius.only(
                    topLeft: const Radius.circular(18),
                    topRight: const Radius.circular(18),
                    bottomLeft: Radius.circular(_isUser ? 18 : 4),
                    bottomRight: Radius.circular(_isUser ? 4 : 18),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.1),
                      blurRadius: 4,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: Text(
                  message.text,
                  style: GoogleFonts.poppins(
                    fontSize: 14,
                    color: Colors.white,
                    height: 1.5,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}