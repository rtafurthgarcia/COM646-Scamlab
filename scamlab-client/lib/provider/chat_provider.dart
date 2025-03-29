import 'dart:async';

import 'package:rxdart/rxdart.dart';
import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/chat_ws_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'dart:developer' as developer;

class ChatProvider extends RetryableProvider {
  final ChatWSService _wsService;
  final GameService _gameService;
  Game get game => _gameService.game;

  final Stopwatch _stopWatch = Stopwatch()..start();
  int get timeLeft {
    var timeLeft = game.timeBeforeVote! - _stopWatch.elapsed.inSeconds;

    if (timeLeft >= 0) {
      return timeLeft;
    } else {
      return 0;
    }
  }

  // Internal list to hold messages
  final List<GamePlayersMessage> _messages = [];

  // A stream controller to broadcast updated lists
  final BehaviorSubject<List<GamePlayersMessage>> _messagesSubject = BehaviorSubject();

  Stream<List<GamePlayersMessage>> get messagesStream => _messagesSubject.stream;

  ChatProvider({
    required GameService gameService,
    required ChatWSService wsService,
  }) : _wsService = wsService,
       _gameService = gameService;

  late StreamSubscription _wsSubscription;
  late StreamSubscription _stateSubscription;

  bool get isListening => _wsService.isListening;

  void stopListening() {
    _wsSubscription.cancel();
    _stateSubscription.cancel();
    _wsService.disconnect();
    notifyListeners();
  }

  void pauseListening() { 
    _wsSubscription.pause();
    _stateSubscription.pause();
    _stopWatch.stop();
  }

  void resumeListening() { 
    _wsSubscription.resume(); 
    _stateSubscription.resume();
    _stopWatch.reset();
  }

  void clearMessages() {
    _messages.clear();
    _messagesSubject.add(List.unmodifiable(_messages));
    notifyListeners();
  }

  Future sendNewMessage(String message) {
    return _wsService.sendMessage(
        GamePlayersMessage(
          senderSecondaryId: game.playerSecondaryId!,
          senderUsername: game.username!,
          text: message,
          imagePath: "",
          sequence: -1,
        ),
      );
  }

  Future<void> startListening() async {
    // Listen for state changes if needed.
    _stateSubscription = game.onStateChange.listen((event) {
      developer.log(
        "Game ${game.conversationSecondaryId} went from ${event.from.name} to ${event.to.name}",
        name: "chat_provider",
        time: DateTime.now(),
      );
      notifyListeners();
    });

    _wsService.connect();

    // Listen to incoming WebSocket messages.
    _wsSubscription = _wsService.stream.listen(
      (message) => _onMessageReceived(message),
      onError: _onErrorReceived,
    );

    if (_messages.isEmpty) {
      _messages.addAll([
        GamePlayersMessage(
          sequence: 0,
          senderSecondaryId: "",
          senderUsername: "",
          text: "${game.otherPlayers?.entries.first.value} joined...",
          origin: MessageOrigin.system,
        ),
        GamePlayersMessage(
          sequence: 0,
          senderSecondaryId: "",
          senderUsername: "",
          text: "${game.otherPlayers?.entries.last.value} joined...",
          origin: MessageOrigin.system,
        ),
      ]);
      _messagesSubject.add(List.unmodifiable(_messages));
    }

    notifyListeners();
  }

  Future<void> _onMessageReceived(WsMessage message) async {
    if (message is GamePlayersMessage) {
      // Process the message and mark sender.
      message.origin =
          message.senderSecondaryId == game.playerSecondaryId
              ? MessageOrigin.me
              : MessageOrigin.other;
      _messages.add(message);
      // Emit the updated list.
      _messagesSubject.add(List.unmodifiable(_messages));
    } else {
      // Handle other types of messages.
      _processMessage(message);
    }
    notifyListeners();
  }

  void _processMessage(WsMessage message) {
    if (message is GameCallToVoteMessage) {
      game.callToVote = message;
      game.voteCalled();
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
      developer.log(
        "Error of type {$error}",
        name: "chat_provider",
        time: DateTime.now(),
        error: error,
      );
    }

    notifyListeners();
  }

  @override
  void dispose() {
    stopListening();
    _messagesSubject.close();
    super.dispose();
  }

  @override
  Future<void> tryAgain() async {
    stopListening();
    await startListening();
  }
}
