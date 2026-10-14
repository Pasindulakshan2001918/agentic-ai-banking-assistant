import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../../providers/chat_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/chat_bubble.dart';
import '../../widgets/typing_indicator.dart';

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final _messageController = TextEditingController();
  final _scrollController = ScrollController();

  final List<String> _quickPrompts = [
    'Check my balance',
    'Show my transactions',
    'What cards do I have?',
    'Pay my electricity bill',
    'Transfer LKR 5000 to John',
    'Show my active loans',
  ];

  @override
  void dispose() {
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _sendMessage([String? textOverride]) {
    final text = textOverride ?? _messageController.text;
    if (text.trim().isEmpty) return;

    context.read<ChatProvider>().sendMessage(text.trim());
    if (textOverride == null) {
      _messageController.clear();
    }
    _scrollToBottom();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: AppTheme.surface,
        elevation: 0,
        title: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF7C4DFF), Color(0xFF536DFE)],
                ),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.smart_toy_rounded,
                color: Colors.white,
                size: 20,
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'AI Banking Assistant',
                  style: GoogleFonts.poppins(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
                Row(
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      decoration: const BoxDecoration(
                        color: AppTheme.success,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      'Online � EN / SI / TA',
                      style: GoogleFonts.poppins(
                        fontSize: 11,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Clear Chat',
            icon: const Icon(Icons.delete_outline_rounded, color: AppTheme.textSecondary),
            onPressed: () => _confirmClearChat(context),
          ),
        ],
      ),
      body: Consumer<ChatProvider>(
        builder: (context, chat, _) {
          WidgetsBinding.instance.addPostFrameCallback((_) => _scrollToBottom());

          return Column(
            children: [
              // -- Quick Prompts Horizontal Bar --
              Container(
                height: 48,
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: _quickPrompts.length,
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemBuilder: (context, index) {
                    final prompt = _quickPrompts[index];
                    return ActionChip(
                      backgroundColor: AppTheme.surface,
                      side: const BorderSide(color: Color(0xFF33334D)),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                      label: Text(
                        prompt,
                        style: GoogleFonts.poppins(
                          fontSize: 12,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                      onPressed: () => _sendMessage(prompt),
                    );
                  },
                ),
              ),

              const Divider(color: Color(0xFF222238), height: 1),

              // -- Messages List --
              Expanded(
                child: chat.messages.isEmpty
                    ? _buildEmptyState()
                    : ListView.builder(
                        controller: _scrollController,
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                        itemCount: chat.messages.length + (chat.isTyping ? 1 : 0),
                        itemBuilder: (context, index) {
                          if (index == chat.messages.length && chat.isTyping) {
                            return const Padding(
                              padding: EdgeInsets.symmetric(vertical: 4),
                              child: Align(
                                alignment: Alignment.centerLeft,
                                child: TypingIndicator(),
                              ),
                            );
                          }
                          return ChatBubble(message: chat.messages[index]);
                        },
                      ),
              ),

              // -- Input Field Bar --
              Container(
                padding: EdgeInsets.only(
                  left: 16,
                  right: 16,
                  top: 10,
                  bottom: MediaQuery.of(context).padding.bottom + 10,
                ),
                decoration: const BoxDecoration(
                  color: AppTheme.surface,
                  border: Border(
                    top: BorderSide(color: Color(0xFF222238), width: 1),
                  ),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14),
                        decoration: BoxDecoration(
                          color: AppTheme.background,
                          borderRadius: BorderRadius.circular(24),
                          border: Border.all(color: const BorderSide(color: Color(0xFF33334D)).color),
                        ),
                        child: TextField(
                          controller: _messageController,
                          style: GoogleFonts.poppins(color: Colors.white, fontSize: 14),
                          maxLines: null,
                          textInputAction: TextInputAction.send,
                          onSubmitted: (_) => _sendMessage(),
                          decoration: InputDecoration(
                            hintText: 'Ask in English, Sinhala or Tamil...',
                            hintStyle: GoogleFonts.poppins(
                              color: AppTheme.textSecondary.withOpacity(0.6),
                              fontSize: 13,
                            ),
                            border: InputBorder.none,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      decoration: const BoxDecoration(
                        gradient: AppTheme.primaryGradient,
                        shape: BoxShape.circle,
                      ),
                      child: IconButton(
                        icon: const Icon(Icons.send_rounded, color: Colors.white, size: 20),
                        onPressed: chat.isTyping ? null : () => _sendMessage(),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF7C4DFF), Color(0xFF536DFE)],
                ),
                borderRadius: BorderRadius.circular(24),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF7C4DFF).withOpacity(0.3),
                    blurRadius: 20,
                    offset: const Offset(0, 10),
                  ),
                ],
              ),
              child: const Icon(
                Icons.smart_toy_rounded,
                color: Colors.white,
                size: 42,
              ),
            ),
            const SizedBox(height: 20),
            Text(
              'How can I help you today?',
              style: GoogleFonts.poppins(
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'You can ask me to check balances, transfer funds,\npay utility bills, inspect cards, or show loan status.\nI speak English, Sinhala, and Tamil.',
              textAlign: TextAlign.center,
              style: GoogleFonts.poppins(
                fontSize: 13,
                color: AppTheme.textSecondary,
                height: 1.5,
              ),
            ),
            const SizedBox(height: 24),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              alignment: WrapAlignment.center,
              children: _quickPrompts.take(4).map((prompt) {
                return ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.surface,
                    foregroundColor: Colors.white,
                    side: const BorderSide(color: Color(0xFF33334D)),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(20),
                    ),
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  ),
                  onPressed: () => _sendMessage(prompt),
                  child: Text(prompt, style: GoogleFonts.poppins(fontSize: 12)),
                );
              }).toList(),
            ),
          ],
        ),
      ),
    );
  }

  void _confirmClearChat(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text('Clear Chat', style: GoogleFonts.poppins(color: Colors.white)),
        content: Text(
          'Are you sure you want to clear your chat history?',
          style: GoogleFonts.poppins(color: AppTheme.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text('Cancel', style: GoogleFonts.poppins(color: AppTheme.textSecondary)),
          ),
          TextButton(
            onPressed: () {
              context.read<ChatProvider>().clearChat();
              Navigator.pop(ctx);
            },
            child: Text('Clear', style: GoogleFonts.poppins(color: AppTheme.error)),
          ),
        ],
      ),
    );
  }
}
