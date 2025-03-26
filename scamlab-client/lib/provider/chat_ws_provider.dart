import 'dart:async';

import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/chat_ws_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'dart:developer' as developer;

class ChatWSProvider extends RetryableProvider {
  final ChatWSService _wsService;
  final GameService _gameService;
  Game get game => _gameService.game;

  // Internal list to hold messages
  final List<GamePlayersMessage> _messages = [];
  
  // A stream controller to broadcast updated lists
  final StreamController<List<GamePlayersMessage>> _messagesController = StreamController.broadcast();

  Stream<List<GamePlayersMessage>> get messagesStream => _messagesController.stream;

  ChatWSProvider({
    required GameService gameService,
    required ChatWSService wsService,
  })  : _wsService = wsService,
        _gameService = gameService;

  late StreamSubscription _subscription;

  bool get isListening => _wsService.isListening;
  
  void stopListening() {
    _subscription.cancel();
    _wsService.disconnect();
    notifyListeners();
  }

  void clearMessages() {
    _messages.clear();
    _messagesController.add(List.unmodifiable(_messages));
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
          sequence: -1,
        ),
      );
    }
  }

  Future<void> startListening() async {
    // Listen for state changes if needed.
    game.onStateChange.listen((event) {
      developer.log(
        "Game ${game.conversationSecondaryId} went from ${event.from.name} to ${event.to.name}",
        name: "chat_ws_provider",
        time: DateTime.now(),
      );
      notifyListeners();
    });
    
    _wsService.connect();

    // Listen to incoming WebSocket messages.
    _subscription = _wsService.stream.listen(
      (message) => _onMessageReceived(message),
      onError: _onErrorReceived,
    );

    notifyListeners();
  }

  Future<void> _onMessageReceived(WsMessage message) async {
    if (message is GamePlayersMessage) {
      // Process the message and mark sender.
      message.isSender = message.senderSecondaryId == game.playerSecondaryId;
      _messages.add(message);
      // Emit the updated list.
      _messagesController.add(List.unmodifiable(_messages));
    } else {
      // Handle other types of messages.
      _processMessage(message);
    }
    notifyListeners();
  }

  void _processMessage(WsMessage message) {
    if (message is GameCallToVoteMessage) {
      game.voteCalled();
      Timer(Duration(seconds: message.voteTimeout), () => triggerTimeout());
    }
    if (message is GameFinishedMessage) {
      game.reachedEndGame();
    }
    if (message is GameCancelledMessage) {
      game.reasonForCancellation = message.reason;
      game.gameGotInterrupted();
    }
  }

  T? getLastMessageOfType<T extends WsMessage>() {
    // As _bufferedMessages is keyed by sequence, iterate its values.
    return _messages.whereType<T>().lastOrNull;
  }

  void _onErrorReceived(dynamic error) {
    if (error is Exception) {
      exception = error;
      if (!_wsService.isListening) {
        game.gameGotInterrupted();
      }
    } else {
      developer.log("Error of type {$error}", name: "chat_ws_provider", time: DateTime.now(), error: error);
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
    _messagesController.close();
    super.dispose();
  }

  @override
  Future<void> tryAgain() async {
    stopListening();
    await startListening();
  }
}
