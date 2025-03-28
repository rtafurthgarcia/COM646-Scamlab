import 'dart:async';

import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/chat_ws_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'dart:developer' as developer;

import 'package:state_machine/state_machine.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

class VoteProvider extends RetryableProvider {
  final ChatWSService _wsService;
  final GameService _gameService;
  Game get game => _gameService.game;
  Timer? _timer;
  StreamSubscription? _subscription;

  VoteProvider({
    required GameService gameService,
    required ChatWSService wsService
  }) : _wsService = wsService,
       _gameService = gameService;

  bool get isListening => _wsService.isListening;
  void stopListening() {
    _subscription?.cancel();
    _wsService.disconnect();
    notifyListeners();
  }

  void castVote(String playerOnBallotSecondaryId) {
    if (game.currentState == game.isVoting && game.isGameAssigned) {
      _timer?.cancel();
      _wsService.castVote(
        game.conversationSecondaryId!,
        game.playerSecondaryId!, 
        playerOnBallotSecondaryId
      );
    }
  }

  Future<void> startListening() async {
    try {
      _wsService.connect();
      game.onStateChange.listen((event) {
        developer.log(
          "Game ${game.conversationSecondaryId} went from ${event.from.name} to ${event.to.name}",
          name: "lobby_ws_provider", 
          time: DateTime.now()
        );
        notifyListeners();
      });
      _subscription = _wsService.stream.listen(
        (message) => _onMessageReceived(message),
        onDone: () {
          if (_wsService.errorCode != null) {
            _onErrorReceived(WebSocketChannelException("Connection closed on error code: ${_wsService.errorCode}"));
          }
        }
      );

      _timer = Timer(
        Duration(seconds: game.voteTimeout!),
        () => triggerTimeout(),
      );
    } catch (e) {
      _onErrorReceived(e);
    }
    notifyListeners();
  }

  Future<void> _onMessageReceived(WsMessage message) async {
    // Insert the incoming message keyed by its sequence.
    try {
      _processMessage(message);
    } on IllegalStateTransition catch (e) {
      _subscription?.pause();
      developer.log(
        "Transition ${e.transition} impossible from ${e.from} to ${e.to}",
        name: "lobby_ws_provider",
        time: DateTime.now()
      );
      await _gameService.reconcileStateIfNecessary(
        game.conversationSecondaryId!,
      );
      _subscription?.resume();
    }
    notifyListeners();
  }

  void _processMessage(WsMessage message) {
    if (message is GameVoteAcknowledgedMessage) {
      game.playerVoted();
    }

    if (message is GameStartingOrContinuingMessage) {
      if (! game.keepOnPlaying.canCall()) {
        game.voteTimedOut();
      }

      game.keepOnPlaying();
    }

    if (message is GameCancelledMessage) {
      if (! game.keepOnPlaying.canCall()) {
        game.voteTimedOut();
      }

      game.gameGotInterrupted();
    }

    if (message is GameFinishedMessage) {
      if (! game.keepOnPlaying.canCall()) {
        game.voteTimedOut();
      }

      game.reachedEndGame();
    }
  }

  void _onErrorReceived(dynamic error) {
    exception = error;

    notifyListeners();
  }

  void triggerTimeout() {
    if (game.voteTimedOut.canCall()) {
      game.voteTimedOut();
      notifyListeners();
    }
  }

  @override
  void dispose() {
    stopListening();
    _timer?.cancel();
    super.dispose();
  }

  @override
  Future<void> tryAgain() async {
    stopListening();
    await startListening();
  }
}
