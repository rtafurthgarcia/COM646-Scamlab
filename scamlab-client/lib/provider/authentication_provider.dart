import 'package:flutter/material.dart';
import 'package:scamlab/model/player.dart';
import 'package:scamlab/service/authentication_service.dart';

class AuthenticationProvider with ChangeNotifier {
  final AuthenticationService _authenticationService;
  bool _isLoading = false;

  Player? _player;
  Player? get player => _player;

  AuthenticationProvider({required AuthenticationService authenticationService}) : _authenticationService = authenticationService {
    if (_player == null) {
      registerNewPlayer();
    }
  }

  bool get isLoading => _isLoading;

  Future<void> registerNewPlayer() async {
    _isLoading = true;
    notifyListeners();

    try {
      _player = await _authenticationService.registerNewPlayer();
    } catch (error) {
      debugPrint("Error registering player: $error");
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> unregisterPlayer() async {
    _isLoading = true;
    notifyListeners();

    try {
      await _authenticationService.unregisterPlayer(player!.secondaryId, player!.jwtToken);
      _player = null;
    } catch (error) {
      debugPrint('Error unregistering player: $error');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
