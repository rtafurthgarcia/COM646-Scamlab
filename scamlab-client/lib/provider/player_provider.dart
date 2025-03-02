import 'package:flutter/material.dart';
import 'package:scamlab/model/player.dart';
import 'package:scamlab/service/player_service.dart';

class PlayerProvider with ChangeNotifier {
  final PlayerService _playerService;
  bool _isLoading = false;

  Player? _player;
  Player? get player => _player;

  PlayerProvider({required PlayerService playerService}) : _playerService = playerService {
    if (_player == null) {
      registerNewPlayer();
    }
  }

  bool get isLoading => _isLoading;

  Future<void> registerNewPlayer() async {
    _isLoading = true;
    notifyListeners();

    try {
      _player = await _playerService.registerNewPlayer();
    } catch (error) {
      debugPrint("Error registering player: $error");
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> unregisterPlayer(String secondaryId, String authToken) async {
    _isLoading = true;
    notifyListeners();

    try {
      await _playerService.unregisterPlayer(secondaryId, authToken);
      _player = null;
    } catch (error) {
      debugPrint('Error unregistering player: $error');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
