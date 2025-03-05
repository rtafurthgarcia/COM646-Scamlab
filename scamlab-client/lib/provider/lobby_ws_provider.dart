import 'package:scamlab/provider/clearable_provider.dart';

class LobbyWSProvider extends ClearableProvider {
  bool _dontWaitNextTime = false;
  bool get dontWaitNextTime => _dontWaitNextTime;

  set dontWaitNextTime(bool value) {
    _dontWaitNextTime = value;
    notifyListeners();
  }

  @override
  void tryAgain() {
    // TODO: implement tryAgain
  }
}