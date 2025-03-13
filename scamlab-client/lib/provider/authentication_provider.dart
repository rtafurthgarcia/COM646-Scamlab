import 'package:scamlab/model/player.dart';
import 'package:scamlab/provider/retryable_provider.dart';
import 'package:scamlab/service/authentication_service.dart';

class AuthenticationProvider extends RetryableProvider {
  final AuthenticationService _authenticationService;

  Player? _player;
  Player? get player => _player;

  AuthenticationProvider({required AuthenticationService authenticationService}) : _authenticationService = authenticationService {
    if (_player == null) {
      registerNewPlayer();
    }
  }

  Future<void> registerNewPlayer() async {
    exception = null;
    isLoading = true;
    notifyListeners();

    try {
      _player = await _authenticationService.registerNewPlayer();
    } on TokenAlreadyAttributedException catch (e) {
      unregisterPlayer();

      exception = e;
    } on Exception catch (e)  {
      //debugPrint("Error registering player: $error");
      exception = e;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<void> unregisterPlayer() async {
    exception = null;
    isLoading = true;
    notifyListeners();

    try {
      await _authenticationService.unregisterPlayer(player!.secondaryId, player!.jwtToken);
      _player = null;
    } on Exception catch (e)  {
      //debugPrint("Error registering player: $error");
      exception = e;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  @override
  Future<void> tryAgain() async {
    if (_player != null) {
      await unregisterPlayer();
      registerNewPlayer();
    } else {
      registerNewPlayer();
    }
  }
}
