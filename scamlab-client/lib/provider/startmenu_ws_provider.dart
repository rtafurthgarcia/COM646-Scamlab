import 'package:flutter/material.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/service/basic_ws_service.dart';

class StartMenuWSProvider extends ChangeNotifier {
  final LobbyWSService wsService;
  StartMenuStatisticsMessage? _chatMessage;

  bool isReady() {
    return wsService.jwtToken != null;
  }

  StartMenuStatisticsMessage? get chatMessage => _chatMessage;

  StartMenuWSProvider({required this.wsService}) {
    connect();
  }

  void connect() {
    // Start the connection when this provider is instantiated.
    if (isReady()) {
      wsService.connect(_onMessageReceived);
    }
  }

  void _onMessageReceived(WsMessage message) {
    if (message is StartMenuStatisticsMessage) {
      _chatMessage = message;
      notifyListeners();
    }
  }

  @override
  void dispose() {
    wsService.disconnect();
    super.dispose();
  }
}
