import 'dart:async';
import 'dart:collection';

import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/chat_ws_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'dart:developer' as developer;

class ChatWSProvider extends RetryableProvider {
  final ChatWSService wsService;
  final GameService gameService;
  final Game game;

  final SplayTreeSet<WsMessage> _messages = SplayTreeSet((m1, m2) => m1.sequence.compareTo(m2.sequence));

  ChatWSProvider({required this.gameService, required this.wsService, required this.game});

  bool isReady() {
    return wsService.jwtToken != null;
  }

  bool isListening() => wsService.isListening();
  void stopListening() {
    wsService.disconnect();
    _messages.clear();
    notifyListeners();
  }

  void clearMessages() {
    _messages.clear();
    notifyListeners();
  }

  T? getLastMessageOfType<T extends WsMessage>() {
    return _messages.whereType<T>().lastOrNull;
  }

  WsMessage? getLastMessage() {
    return _messages.isNotEmpty ? _messages.last : null;
  }

  void sendNewMessage(GamePlayersMessage message) {
    if (game.stateMachine.current == game.isRunning) {
      wsService.sendMessage(message);
    }
  }

  Future<void> startListening() async {
    if (isReady()) {
      game.stateMachine.onStateChange.listen((event) {
        developer.log("${game.stateMachine.name} went from ${event.from.name} to ${event.to.name}");
        notifyListeners();
      });
      wsService.connect(_onMessageReceived, _onErrorReceived);
    }
  }

  void _onMessageReceived(WsMessage message) {
    _messages.add(message);
    _processMessage(message);
    notifyListeners();
  }

  void _processMessage(WsMessage message) {
    if (message is GameCallToVoteMessage) {
      game.voteCalled();
      Timer(
        Duration(seconds: message.voteTimeout),
        () => triggerTimeout(),
      );

    }

    if (message is GameFinishedMessage) {
      game.reachedEndGame();
    }

    if (message is GameCancelledMessage) {
      game.gameGotInterrupted();
    }
  }

   void _onErrorReceived(dynamic error) {
    exception = error;

    if (! wsService.isListening()) {
      game.gameGotInterrupted();
    }

    notifyListeners();
  }

  void triggerTimeout() {
    if (game.startTimedOut.canCall()) {
      game.startTimedOut();
      notifyListeners();
    }
  }

  @override
  void dispose() {
    stopListening();
    game.reset();
    super.dispose();
  }
  
  @override
  Future<void> tryAgain() async {
    stopListening();
    await startListening();
  }
}