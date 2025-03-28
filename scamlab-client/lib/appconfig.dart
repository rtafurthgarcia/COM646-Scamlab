import 'dart:convert';

import 'package:flutter/services.dart';

class AppConfig {
  final String apiURL;
  final String wsURL;

  AppConfig({required this.apiURL, required this.wsURL});

  factory AppConfig.fromJson(Map<String, dynamic> json) {
    return AppConfig(
      apiURL: json['apiURL'],
      wsURL: json['wsURL'],
    );
  }
}

Future<AppConfig> loadConfig() async {
  final configString = await rootBundle.loadString('assets/config.json');
  final jsonMap = json.decode(configString);
  return AppConfig.fromJson(jsonMap);
}