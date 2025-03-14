import 'dart:async';
import 'dart:collection';

import 'package:collection/collection.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/lobby_ws_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LobbyWSProvider extends RetryableProvider {
  final LobbyWSService wsService;
  final GameService gameService;
  final SplayTreeSet<WsMessage> _lastMessages = SplayTreeSet((m0, m1) => m0.receivedOn.compareTo(m1.receivedOn));
  bool _dontWaitNextTime = false;
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
    _lastMessages.clear();
    _mayStillStart = false;
    notifyListeners();
  }

  void clearMessages() {
    _lastMessages.clear();
    notifyListeners();
  }

  T? getLastMessageOfType<T extends WsMessage>() {
    return _lastMessages
      .whereType<T>()
      .lastOrNull;
  }

  WsMessage? getLastMessage() {
    return _lastMessages.lastOrNull;
  }


  void voteToStart() {
    if (_mayStillStart) {
      wsService.voteToStart(getLastMessageOfType<WaitingLobbyAssignedStrategyMessage>()!.conversationSecondaryId);
    }
  }

  Future<void> startListening() async {
    // Start the connection when this provider is instantiated.
    if (isReady()) {
      await gameService.joinNewGame();
      wsService.connect(_onMessageReceived, _onErrorReceived);
    }
  }

  void _onMessageReceived(WsMessage message) {
    _lastMessages.add(message);
  
    if (message is WaitingLobbyReadyToStartMessage) {
      _mayStillStart = true;
      _timeout = message.voteTimeout;
      Timer(
        Duration(seconds: message.voteTimeout),
        () => triggerTimeout(),
      );
    }

    notifyListeners();
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
