import 'dart:async';
import 'dart:collection';

import 'package:collection/collection.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/lobby_ws_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LobbyWSProvider extends RetryableProvider {
  final LobbyWsService wsService;
  final GameService gameService;

  final SplayTreeMap<int, WsMessage> _bufferedMessages = SplayTreeMap();  bool _dontWaitNextTime = false;
  int _lastProcessedSequence = 0; // Track the last processed sequence number.
  bool get dontWaitNextTime => _dontWaitNextTime;
  late final SharedPreferences _settings;

  set dontWaitNextTime(bool newValue) {
    _dontWaitNextTime = newValue;
    _settings.setBool('dontwaitnexttime', newValue);
    notifyListeners();
  }

  bool _mayStillStart = false;
  bool get mayStillStart => _mayStillStart;

  int? _timeout;
  int? get timeout => _timeout;

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
    _bufferedMessages.clear();
    _mayStillStart = false;
    notifyListeners();
  }

  void clearMessages() {
    _bufferedMessages.clear();
    notifyListeners();
  }

  T? getLastMessageOfType<T extends WsMessage>() {
    // As _bufferedMessages is keyed by sequence, iterate its values.
    return _bufferedMessages.values.whereType<T>().lastOrNull;
  }

  WsMessage? getLastMessage() {
    return _bufferedMessages.isNotEmpty ? _bufferedMessages.values.last : null;
  }

  void voteToStart() {
    final assignedMsg = getLastMessageOfType<WaitingLobbyAssignedStrategyMessage>();
    if (_mayStillStart && assignedMsg != null) {
      wsService.sendMessage(WaitingLobbyVoteToStartMessage(
        conversationSecondaryId: assignedMsg.conversationSecondaryId, 
        sequence: _lastProcessedSequence) 
      );
    }
  }

  Future<void> startListening() async {
    if (isReady()) {
      await gameService.joinNewGame();
      wsService.connect(_onMessageReceived, _onErrorReceived);
    }
  }

  void _onMessageReceived(WsMessage message) {
    // Insert the incoming message keyed by its sequence.
    _bufferedMessages[message.sequence] = message;
    _processBufferedMessages();
    notifyListeners();
  }

  void _processBufferedMessages() {
    // Process messages in order starting from lastProcessedSequence + 1.
    while (_bufferedMessages.containsKey(_lastProcessedSequence + 1)) {
      final nextMessage = _bufferedMessages.remove(_lastProcessedSequence + 1)!;
      _applyMessage(nextMessage);
      _lastProcessedSequence++;
    }
  }

  void _applyMessage(WsMessage message) {
    // Handle the message according to its type.
    if (message is WaitingLobbyReadyToStartMessage) {
      _mayStillStart = true;
      _timeout = message.voteTimeout;
      Timer(
        Duration(seconds: message.voteTimeout),
        () => triggerTimeout(),
      );
    }
  }

   void _onErrorReceived(dynamic error) {
    exception = error;

    notifyListeners();
  }

  void triggerTimeout() {
    _mayStillStart = false;
    _timeout = null;
    notifyListeners();
  }

  @override
  void dispose() {
    stopListening();
    super.dispose();
  }
  
  @override
  Future<void> tryAgain() async {
    stopListening();
    await startListening();
  }
}
