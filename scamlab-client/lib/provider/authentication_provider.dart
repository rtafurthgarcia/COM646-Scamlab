import 'package:flutter/material.dart';
import 'package:scamlab/model/player.dart';
import 'package:scamlab/service/authentication_service.dart';

class AuthenticationProvider with ChangeNotifier {
  final AuthenticationService _authenticationService;
  bool _isLoading = false;

  Exception? _exception;
  Exception? get exception => _exception;

  Player? _player;
  Player? get player => _player;

  AuthenticationProvider({required AuthenticationService authenticationService}) : _authenticationService = authenticationService {
    if (_player == null) {
      registerNewPlayer();
    }
  }

  bool get isLoading => _isLoading;

  Future<void> registerNewPlayer() async {
    _exception = null;
    _isLoading = true;
    notifyListeners();

    try {
      _player = await _authenticationService.registerNewPlayer();
    } on Exception catch (e)  {
      //debugPrint("Error registering player: $error");
      _exception = e;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> unregisterPlayer() async {
    _exception = null;
    _isLoading = true;
    notifyListeners();

    try {
      await _authenticationService.unregisterPlayer(player!.secondaryId, player!.jwtToken);
      _player = null;
    } on Exception catch (e)  {
      //debugPrint("Error registering player: $error");
      _exception = e;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> refreshPlayersIdentity() async {
    if (_player != null) {
      await unregisterPlayer();
      registerNewPlayer();
    } else {
      registerNewPlayer();
    }
  }

  void clearException() {
    _exception = null;
    notifyListeners();
  }
}
