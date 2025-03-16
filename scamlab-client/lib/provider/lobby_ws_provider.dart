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

class LobbyWSProvider extends RetryableProvider {
  final LobbyWsService wsService;
  final GameService gameService;
  final Game game;

  final SplayTreeSet<WsMessage> _messages = SplayTreeSet((m1, m2) => m1.sequence.compareTo(m2.sequence));
  bool _dontWaitNextTime = false;
  bool get dontWaitNextTime => _dontWaitNextTime;
  late final SharedPreferences _settings;

  set dontWaitNextTime(bool newValue) {
    _dontWaitNextTime = newValue;
    _settings.setBool('dontwaitnexttime', newValue);
    notifyListeners();
  }

  LobbyWSProvider({required this.gameService, required this.wsService, required this.game}) {
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
    final assignedMsg = getLastMessageOfType<WaitingLobbyAssignedStrategyMessage>();
    if (game.stateMachine.current == game.isReady && assignedMsg != null) {
      wsService.sendMessage(WaitingLobbyVoteToStartMessage(
        sequence: -1,
        conversationSecondaryId: assignedMsg.conversationSecondaryId) 
      );
    }
  }

  Future<void> startListening() async {
    if (isReady()) {
      await gameService.joinNewGame();
      game.stateMachine.start(game.isWaiting);
      game.stateMachine.onStateChange.listen((event) {
        developer.log("${game.stateMachine.name} went from ${event.from.name} to ${event.to.name}");
        notifyListeners();
      });
      wsService.connect(_onMessageReceived, _onErrorReceived);
    }
  }

  void _onMessageReceived(WsMessage message) {
    // Insert the incoming message keyed by its sequence.
    _messages.add(message);
    _processMessage(message);
    notifyListeners();
  }

  void _processMessage(WsMessage message) {
    if (message is WaitingLobbyAssignedStrategyMessage) {
      game.conversationId = message.conversationSecondaryId;
    }

    if (message is WaitingLobbyReasonForWaitingMessage && game.conditionsNotMetAnymore.canCall()) {
      game.conditionsNotMetAnymore();
    }

    if (message is WaitingLobbyReadyToStartMessage) {
      Timer(
        Duration(seconds: message.voteTimeout),
        () => triggerTimeout(),
      );

      game.conditionsMetForStart();
    }

    if (message is WaitingLobbyVoteAcknowledgedMessage) {
      game.playersClickedStart();
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
    game.reset();
    super.dispose();
  }
  
  @override
  Future<void> tryAgain() async {
    stopListening();
    await startListening();
  }
}
