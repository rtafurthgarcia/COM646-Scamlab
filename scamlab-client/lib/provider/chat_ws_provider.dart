import 'dart:async';

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

  ChatWSProvider({required this.gameService, required this.wsService, required this.game});

  late Stream<List<GamePlayersMessage>> messagesStream;

  bool isReady() {
    return wsService.jwtToken != null;
  }

  bool isListening() => wsService.isListening();
  void stopListening() {
    wsService.disconnect();
    notifyListeners();
  }

  void clearMessages() {
    notifyListeners();
  }

  void sendNewMessage(String message) {
    if (game.stateMachine.current == game.isRunning && game.isGameAssigned) {
      wsService.sendMessage(
        GamePlayersMessage(
          senderSecondaryId: game.playerSecondaryId!,
          senderUsername: game.username!,
          isSender: true,
          text: message,
          imagePath: "", 
          sequence: -1
        )
      );
    }
  }

  Future<void> startListening() async {
    if (isReady()) {
      game.stateMachine.onStateChange.listen((event) {
        developer.log("${game.stateMachine.name} went from ${event.from.name} to ${event.to.name}");
        notifyListeners();
      });
      wsService.connect();
      wsService.stream.listen((message) => _onMessageReceived(message), onError: _onErrorReceived);
      messagesStream = wsService.stream
        .where((element) => element is GamePlayersMessage)
        .cast<GamePlayersMessage>()
        .map((element) => element..isSender = element.senderSecondaryId == game.playerSecondaryId)
        .toList()
        .asStream();
    }
  }

  void _onMessageReceived(WsMessage message) {
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