import 'dart:async';
import 'dart:collection';

import 'package:collection/collection.dart';
import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/lobby_ws_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'package:scamlab/service/settings_service.dart';
import 'dart:developer' as developer;

import 'package:state_machine/state_machine.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

class LobbyWSProvider extends RetryableProvider {
  final LobbyWsService _wsService;
  final GameService _gameService;
  final SettingsService _settingsService;
  Game get game => _gameService.game;
  bool get dontWaitNextTime => _settingsService.dontWaitNextTime;
  set dontWaitNextTime(bool newValue) => {
    _settingsService.dontWaitNextTime = newValue,
    notifyListeners()
  };

  final SplayTreeSet<WsMessage> _messages = SplayTreeSet(
    (m1, m2) => m1.sequence.compareTo(m2.sequence),
  );

  Timer? _timer;
  StreamSubscription? _subscription;

  LobbyWSProvider({
    required GameService gameService,
    required LobbyWsService wsService,
    required SettingsService settingsService,
  }) : _settingsService = settingsService,
       _wsService = wsService,
       _gameService = gameService;

  bool get isListening => _wsService.isListening;
  void stopListening() {
    _subscription?.cancel();
    _wsService.disconnect();
    _messages.clear();
    notifyListeners();
  }

  void clearMessages() {
    _messages.clear();
    notifyListeners();
  }

  T? getLastMessageOfType<T extends WsMessage>() {
    // As _bufferedMessages is keyed by sequence, iterate its values.
    return _messages.whereType<T>().lastOrNull;
  }

  WsMessage? getLastMessage() {
    return _messages.isNotEmpty ? _messages.last : null;
  }

  void voteToStart() {
    if (game.currentState == game.isReady && game.isGameAssigned) {
      _timer?.cancel();
      _wsService.voteToStart(game.conversationSecondaryId!);
    }
  }

  Future<void> startListening() async {
    try {
      _wsService.connect();
      await _gameService.joinNewGame();
      game.startFrom(game.isWaiting);
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
    } catch (e) {
      _onErrorReceived(e);
    }
    notifyListeners();
  }

  Future<void> _onMessageReceived(WsMessage message) async {
    // Insert the incoming message keyed by its sequence.
    _messages.add(message);
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
    if (message is WaitingLobbyGameAssignmentMessage) {
      game.gameAssignment = message;
    }

    if (message is WaitingLobbyReasonForWaitingMessage &&
        game.conditionsNotMetAnymore.canCall()) {
      game.conditionsNotMetAnymore();
    }

    if (message is WaitingLobbyReadyToStartMessage &&
        game.conditionsMetForStart.canCall()) {
      _timer = Timer(
        Duration(seconds: message.voteTimeout),
        () => triggerTimeout(),
      );

      game.conditionsMetForStart();

      if (_settingsService.dontWaitNextTime) {
        voteToStart();
      }
    }

    if (message is WaitingLobbyVoteAcknowledgedMessage) {
      game.playerStarted();
    }

    if (message is GameStartingOrContinuingMessage) {
      game.allPlayersStarted();
      _wsService.gameStarting();
    }
  }

  void _onErrorReceived(dynamic error) {
    exception = error;

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
    _timer?.cancel();
    super.dispose();
  }

  @override
  Future<void> tryAgain() async {
    stopListening();
    await startListening();
  }
}
