import 'package:scamlab/appconfig.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsService {
  late final SharedPreferences _settings;
  late bool _dontWaitNextTime;
  bool get dontWaitNextTime => _dontWaitNextTime;
  final AppConfig _appConfig;
  String get apiURL => _appConfig.apiURL;
  String get wsURL => _appConfig.wsURL;

  SettingsService({ required AppConfig appConfig}) : _appConfig = appConfig {
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