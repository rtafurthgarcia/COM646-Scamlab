import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/game.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/home_provider.dart';
import 'package:scamlab/service/chat_ws_service.dart';
import 'package:scamlab/service/lobby_ws_service.dart';
import 'package:scamlab/service/settings_service.dart';
import 'package:scamlab/service/startmenu_ws_service.dart';
import 'package:scamlab/service/authentication_service.dart';
import 'package:scamlab/service/game_service.dart';
import 'package:scamlab/theme.dart';
import 'package:scamlab/view/page/chat_page.dart';
import 'package:scamlab/view/page/home_page.dart';
import 'package:scamlab/view/page/vote_page.dart';
import 'package:scamlab/view/page/waiting_lobby_page.dart';
import 'package:scamlab/view/widget/clearable_exception_listener.dart';

import 'appconfig.dart';

Future<AppConfig> loadConfig() async {
  final configString = await rootBundle.loadString('assets/config.json');
  final jsonMap = json.decode(configString);
  return AppConfig.fromJson(jsonMap);
}

Future<void> main() async {
  // Define a ZoneSpecification that overrides print.
  final zoneSpecification = ZoneSpecification(
    print: (self, parent, zone, line) {
      parent.print(zone, line);
      final logFile = File('logs.txt');
      logFile.writeAsStringSync('$line\n', mode: FileMode.append, flush: true);
    },
  );

  // Execute all initialization inside the zone.
  runZonedGuarded(() async {
    // Initialize Flutter bindings.
    WidgetsFlutterBinding.ensureInitialized();

    // Now load the configuration asset.
    final config = await loadConfig();

    runApp(
      MultiProvider(
        providers: [
          Provider<AppConfig>.value(value: config),
          Provider(
              create: (context) => AuthenticationService(
                  baseUrl: '${config.apiURL}/api')),
          Provider(
              create: (context) =>
                  GameService(baseUrl: '${config.apiURL}/api', game: Game())),
          Provider(
              create: (context) =>
                  StartmenuWsService(wsUrl: "${config.wsURL}/ws/start-menu")),
          Provider(
              create: (context) =>
                  LobbyWsService(wsUrl: "${config.wsURL}/ws/lobby")),
          Provider(create: (context) => ChatWSService(wsUrl: "")),
          Provider(
              create: (context) => SettingsService(appConfig: config)),
          Provider(create: (context) => RouteObserver<PageRoute>()),
          ChangeNotifierProvider(
            create: (context) => AuthenticationProvider(
              authenticationService: context.read<AuthenticationService>(),
              settingsService: context.read<SettingsService>(),
            ),
          ),
          ChangeNotifierProxyProvider<AuthenticationProvider, HomeProvider>(
            create: (BuildContext context) => HomeProvider(
              wsService: context.read(), 
              gameService: context.read(), 
              setingsService: context.read(),
            ),
            update: (context, authenticationProvider, homeProvider) {
              homeProvider ??= HomeProvider(
                wsService: context.read(), 
                gameService: context.read(), 
                setingsService: context.read(),
              );
      
              if (homeProvider.isListening) {
                homeProvider.stopListening();
              }
      
              if (homeProvider.jwtToken != authenticationProvider.player?.jwtToken) {
                homeProvider.jwtToken = authenticationProvider.player?.jwtToken;
                if (homeProvider.jwtToken != null) {
                  homeProvider.startListening();
                }
              }
              return homeProvider;
            },
          ),
        ],
        child: const MainApp(),
      ),
    );
  }, (error, stack) {
    print('Uncaught error: $error');
    print('Stack trace: $stack');
  }, zoneSpecification: zoneSpecification);
}

class MainApp extends StatelessWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context) {
    // Use the default Material 3 TextTheme (or define your own)
    final textTheme = Typography.material2021().white; // Light theme text
    final materialTheme = MaterialTheme(textTheme);

    return Builder(
      builder: (innerContext) {
        return MaterialApp(
          title: "Scamlab - Game experiment",
          theme: materialTheme.light(),
          darkTheme: materialTheme.dark(),
          themeMode: ThemeMode.system,
          navigatorObservers: [innerContext.read<RouteObserver<PageRoute>>()],
          initialRoute: '/',
          routes: <String, WidgetBuilder>{
            '/': (BuildContext context) => ClearableExceptionListener<AuthenticationProvider>(
                  message: "Couldn't get a new identity.",
                  child: const HomePage(),
                ),
            '/lobby': (BuildContext context) => const WaitingLobbyPage(),
            '/games': (BuildContext context) => const ChatPage(),
            '/votes': (BuildContext context) => const VotePage(),
          },
        );
      },
    );
  }
}
