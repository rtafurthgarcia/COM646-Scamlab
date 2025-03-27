import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/game.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/startmenu_provider.dart';
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

void main() {
  const apiURL = String.fromEnvironment(
    'API_URL',
    defaultValue: 'http://127.0.0.1:8080',
  );
  const wsURL = String.fromEnvironment(
    'WS_URL',
    defaultValue: 'ws://127.0.0.1:8080',
  );

  runApp(
    MultiProvider(
      providers: [
        Provider(create: (context) => AuthenticationService(baseUrl: '$apiURL/api')),
        Provider(create: (context) => GameService(baseUrl: '$apiURL/api', game: Game())),
        Provider(create: (context) => StartmenuWsService(wsUrl: "$wsURL/ws/start-menu")),
        Provider(create: (context) => LobbyWsService(wsUrl: "$wsURL/ws/lobby")),
        Provider(create: (context) => ChatWSService(wsUrl: "")),
        Provider(create: (context) => SettingsService()),
        Provider(create: (context) => RouteObserver<PageRoute>()),
        ChangeNotifierProvider(create: (context) => AuthenticationProvider(
          authenticationService: context.read(),
          settingsService: context.read()
        )),
        ChangeNotifierProxyProvider<AuthenticationProvider, StartMenuProvider>(
          create: (BuildContext context) => StartMenuProvider(wsService: context.read(), gameService: context.read()),
          update: (context, authenticationProvider, startMenuWSProvider) {
            startMenuWSProvider ??= StartMenuProvider(wsService: context.read(), gameService: context.read());
  
            if (startMenuWSProvider.isListening) {
              startMenuWSProvider.stopListening();
            }

            if (startMenuWSProvider.jwtToken != authenticationProvider.player?.jwtToken) {
              startMenuWSProvider.jwtToken = authenticationProvider.player?.jwtToken;
              if (startMenuWSProvider.jwtToken != null) {
                startMenuWSProvider.startListening();
              }
            }
            return startMenuWSProvider;
          }
        )
      ],
      child: MainApp(),
    ),
  );
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
      }
    );
  }
}
