import 'dart:async';

import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/chat_ws_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'dart:developer' as developer;

import 'package:state_machine/state_machine.dart';

class ChatWSProvider extends RetryableProvider {
  final ChatWSService _wsService;
  final GameService _gameService;
  Game get game => _gameService.game;

  ChatWSProvider({required GameService gameService, required ChatWSService wsService}) : _wsService = wsService, _gameService = gameService;

  late Stream<List<GamePlayersMessage>> messagesStream;

  late StreamSubscription _subscription;

  bool get isListening => _wsService.isListening;
  void stopListening() {
    _subscription.cancel();
    _wsService.disconnect();
    notifyListeners();
  }

  void clearMessages() {
    notifyListeners();
  }

  void sendNewMessage(String message) {
    if (game.currentState == game.isRunning && game.isGameAssigned) {
      _wsService.sendMessage(
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
    game.onStateChange.listen((event) {
      developer.log("Game ${game.conversationSecondaryId} went from ${event.from.name} to ${event.to.name}");
      notifyListeners();
    });
    _wsService.connect();
    _subscription = _wsService.stream.listen((message) => _onMessageReceived(message), onError: _onErrorReceived);
    messagesStream = _wsService.stream
      .where((element) => element is GamePlayersMessage)
      .cast<GamePlayersMessage>()
      .map((element) => element..isSender = element.senderSecondaryId == game.playerSecondaryId)
      .toList()
      .asStream();
    notifyListeners();
  }

  Future<void> _onMessageReceived(WsMessage message) async {
    try {
      _processMessage(message);
    } on IllegalStateTransition {
      await _gameService.reconcileStateIfNecessary(game.conversationSecondaryId!);
    }
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

    if (! _wsService.isListening) {
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
    _gameService.game = Game();
    super.dispose();
  }
  
  @override
  Future<void> tryAgain() async {
    stopListening();
    await startListening();
  }
}