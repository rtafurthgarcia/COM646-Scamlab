import 'package:flutter/material.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/service/lobby_ws_service.dart';

class StartMenuWSProvider extends ChangeNotifier {
  final LobbyWsService wsService;
  StartMenuStatisticsMessage? _chatMessage;

  bool isReady() {
    return wsService.jwtToken != null;
  }

  StartMenuStatisticsMessage? get chatMessage => _chatMessage;

  StartMenuWSProvider({required this.wsService});

  bool isListening() => wsService.isListening();
  void stopListening() {
    wsService.disconnect();
    notifyListeners();
  }

  void startListening() {
    // Start the connection when this provider is instantiated.
    if (isReady()) {
      wsService.connect(_onMessageReceived, _onErrorReceived);
    }
  }

  void _onMessageReceived(WsMessage message) {
    if (message is StartMenuStatisticsMessage) {
      _chatMessage = message;
      notifyListeners();
    }
  }

  void _onErrorReceived(dynamic exception) {
    notifyListeners();
  }

  @override
  void dispose() {
    wsService.disconnect();
    super.dispose();
  }
}
