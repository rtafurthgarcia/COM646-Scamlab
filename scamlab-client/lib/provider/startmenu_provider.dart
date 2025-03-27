import 'dart:async';

import 'package:flutter/material.dart';
import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/service/game_service.dart';
import 'package:scamlab/service/startmenu_ws_service.dart';

class StartMenuProvider extends ChangeNotifier {
  final StartmenuWsService _wsService;
  final GameService _gameService;
  Game get game => _gameService.game;
  set game(Game newGame) => _gameService.game = newGame;
  
  int? _playersCount;

  set jwtToken(String? newJwtToken) {
    _wsService.jwtToken = newJwtToken;
    notifyListeners();
  }
  String? get jwtToken => _wsService.jwtToken;
  int? get playersCount => _playersCount;

  late StreamSubscription _subscription;

  StartMenuProvider({required StartmenuWsService wsService, required GameService gameService}) : 
    _wsService = wsService, _gameService = gameService;

  bool get isListening => _wsService.isListening;
  void stopListening() {
    _subscription.cancel();
    _wsService.disconnect();
    notifyListeners();
  }

  void startListening() {
    // Start the connection when this provider is instantiated.
    _wsService.connect();
    _subscription = _wsService.stream.listen((message) => _onMessageReceived(message), onError: _onErrorReceived);
    notifyListeners();
  }

  void _onMessageReceived(WsMessage message) {
    if (message is StartMenuStatisticsMessage) {
      _playersCount = message.playersConnectedCount;
      notifyListeners();
    }
  }

  void _onErrorReceived(dynamic exception) {
    notifyListeners();
  }

  @override
  void dispose() {
    stopListening();
    super.dispose();
  }
}
