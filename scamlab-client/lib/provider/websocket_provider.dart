import 'package:flutter/material.dart';
import 'package:scamlab/model/conversation_start_message.dart';
import 'package:scamlab/service/conversation_ws_service.dart';

class ConversationWSProvider extends ChangeNotifier {
  final ConversationWSService wsService;
  ConversationStartMessage? _chatMessage;

  ConversationStartMessage? get chatMessage => _chatMessage;

  ConversationWSProvider({required this.wsService}) {
    // Start the connection when this provider is instantiated.
    wsService.connect(_onMessageReceived);
  }

  void _onMessageReceived(ConversationStartMessage message) {
    _chatMessage = message;
    notifyListeners();
  }

  @override
  void dispose() {
    wsService.disconnect();
    super.dispose();
  }
}
