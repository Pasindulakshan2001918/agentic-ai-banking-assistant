enum MessageSender { user, bot }

class ChatMessageModel {
  final String text;
  final MessageSender sender;
  final DateTime timestamp;

  ChatMessageModel({
    required this.text,
    required this.sender,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();
}