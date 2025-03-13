import 'dart:async';
import 'dart:collection';

import 'package:collection/collection.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/service/basic_ws_service.dart';
import 'package:scamlab/service/game_service.dart';

class LobbyWSProvider extends ChangeNotifier {
  final BasicWSService wsService;
  final GameService gameService;
  final SplayTreeSet<WsMessage> _lastMessages = SplayTreeSet((m0, m1) => m0.receivedOn.compareTo(m1.receivedOn));
  bool dontWaitNextTime = false;
  bool _mayStillStart = false;
  late Timer _timer;

  bool get mayStillStart => _mayStillStart;

  bool isReady() {
    return wsService.jwtToken != null;
  }

  bool isListening() => wsService.isListening();

  void stopListening() {
    wsService.disconnect();
    _lastMessages.clear();
    _mayStillStart = false;
    notifyListeners();
  }

  void clearMessages() {
    _lastMessages.clear();
    notifyListeners();
  }

  T? getLastMessageOfType<T extends WsMessage>() {
    return _lastMessages
      .whereType<T>()
      .lastOrNull;
  }

  WsMessage? getLastMessage() {
    return _lastMessages.lastOrNull;
  }

  LobbyWSProvider({required this.gameService, required this.wsService}) {
    //connect();
  }

  void voteToStart() {
    if (_mayStillStart) {
      wsService.voteToStart(getLastMessageOfType<WaitingLobbyAssignedStrategyMessage>()!.conversationSecondaryId);
    }
  }

  Future<void> startListening() async {
    // Start the connection when this provider is instantiated.
    if (isReady()) {
      await gameService.joinNewGame();
      wsService.connect(_onMessageReceived);
    }
  }

  void _onMessageReceived(WsMessage message) {
    _lastMessages.add(message);
  
    if (message is WaitingLobbyReadyToStartMessage) {
      _mayStillStart = true;
      _timer = Timer(
        Duration(seconds: message.voteTimeout),
        () => triggerTimeout(),
      );
    }

    notifyListeners();
  }

  void triggerTimeout() {
    _mayStillStart = false;
    notifyListeners();
  }

  @override
  void dispose() {
    wsService.disconnect();
    super.dispose();
  }
}
