import 'package:flutter/material.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/service/startmenu_ws_service.dart';

class StartMenuWSProvider extends ChangeNotifier {
  final StartmenuWsService wsService;
  int? _playersCount;

  bool isReady() {
    return wsService.jwtToken != null;
  }

  int? get playersCount => _playersCount;

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
      notifyListeners();
    }
  }

  void _onMessageReceived(WsMessage message) {
    if (message is StartMenuStatisticsMessage) {
      _playersCount = message.playersConnectedCount;
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
