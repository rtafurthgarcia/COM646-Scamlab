import 'package:shared_preferences/shared_preferences.dart';

class SettingsService {
  late final SharedPreferences _settings;
  late bool _dontWaitNextTime;
  bool get dontWaitNextTime => _dontWaitNextTime;

  SettingsService() {
    loadSettings();
  }

  Future<void> loadSettings() async {
    _settings = await SharedPreferences.getInstance();
    _dontWaitNextTime = _settings.getBool('dontwaitnexttime') ?? false;
  }

  set dontWaitNextTime(bool newValue) {
    _dontWaitNextTime = newValue;
    _settings.setBool('dontwaitnexttime', newValue);
  }
} 