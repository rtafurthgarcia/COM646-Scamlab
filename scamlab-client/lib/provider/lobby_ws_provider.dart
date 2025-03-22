import 'dart:async';
import 'dart:collection';

import 'package:collection/collection.dart';
import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/lobby_ws_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:developer' as developer;

import 'package:state_machine/state_machine.dart';

class LobbyWSProvider extends RetryableProvider {
  final LobbyWsService wsService;
  final GameService gameService;
  Game get game => gameService.game;

  final SplayTreeSet<WsMessage> _messages = SplayTreeSet((m1, m2) => m1.sequence.compareTo(m2.sequence));
  bool _dontWaitNextTime = false;
  bool get dontWaitNextTime => _dontWaitNextTime;
  late final SharedPreferences _settings;
  bool _pauseProcessingMessages = false;

  set dontWaitNextTime(bool newValue) {
    _dontWaitNextTime = newValue;
    _settings.setBool('dontwaitnexttime', newValue);
    notifyListeners();
  }

  LobbyWSProvider({required this.gameService, required this.wsService}) {
    loadSettings();
  }

  Future<void> loadSettings() async {
    _settings = await SharedPreferences.getInstance();
    _dontWaitNextTime = _settings.getBool('dontwaitnexttime') ?? false;
    notifyListeners();
  }

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
    // As _bufferedMessages is keyed by sequence, iterate its values.
    return _messages.whereType<T>().lastOrNull;
  }

  WsMessage? getLastMessage() {
    return _messages.isNotEmpty ? _messages.last : null;
  }

  void voteToStart() {
    if (game.currentState == game.isReady && game.isGameAssigned) {
      wsService.voteToStart(game.conversationSecondaryId!);
    }
  }

  Future<void> startListening() async {
    if (isReady()) {
      await gameService.joinNewGame();
      game.startFrom(game.isWaiting);
      game.onStateChange.listen((event) {
        developer.log("Game ${game.conversationSecondaryId} went from ${event.from.name} to ${event.to.name}");
        notifyListeners();
      });
      wsService.connect();
      wsService.stream.listen((message) => _onMessageReceived(message), onError: _onErrorReceived);
    }
  }

  Future<void> _onMessageReceived(WsMessage message) async {
    // Insert the incoming message keyed by its sequence.
    _messages.add(message);
     try {
      _processMessage(message);
    } on IllegalStateTransition catch (e) {
      _pauseProcessingMessages = true;
      developer.log("Transition ${e.transition} impossible from ${e.from} to ${e.to}", name: "lobby_ws_provider");
      await gameService.reconcileStateIfNecessary(game.conversationSecondaryId!);
      _pauseProcessingMessages = false;
    }
    notifyListeners();
  }

  void _processMessage(WsMessage message) {
    if (_pauseProcessingMessages) {
      return;
    }

    if (message is WaitingLobbyGameAssignmentMessage) {
      game.gameAssignment = message;
    }

    if (message is WaitingLobbyReasonForWaitingMessage && game.conditionsNotMetAnymore.canCall()) {
      game.conditionsNotMetAnymore();
    }

    if (message is WaitingLobbyReadyToStartMessage && game.conditionsMetForStart.canCall()) {
      Timer(
        Duration(seconds: message.voteTimeout),
        () => triggerTimeout(),
      );

      game.conditionsMetForStart();
    }

    if (message is WaitingLobbyVoteAcknowledgedMessage) {
      game.playerStarted();
    }

    if (message is WaitingLobbyGameStartingMessage) {
      game.allPlayersStarted();
      wsService.gameStarting();
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
    gameService.game = Game();
    super.dispose();
  }
  
  @override
  Future<void> tryAgain() async {
    stopListening();
    await startListening();
  }
}
