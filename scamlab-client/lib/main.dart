import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/lobby_ws_provider.dart';
import 'package:scamlab/provider/startmenu_ws_provider.dart';
import 'package:scamlab/service/game_ws_service.dart';
import 'package:scamlab/service/startmenu_ws_service.dart';
import 'package:scamlab/service/authentication_service.dart';
import 'package:scamlab/theme.dart';
import 'package:scamlab/view/page/home_page.dart';
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
        ChangeNotifierProvider(
          create:
              (_) => AuthenticationProvider(
                authenticationService: AuthenticationService(baseUrl: '$apiURL/api'),
              ),
        ),
        ChangeNotifierProxyProvider<AuthenticationProvider, StartMenuWSProvider>(
          update: (context, authenticationProvider, conversationWSProvider) =>
          StartMenuWSProvider(
            wsService: StartMenuWSService(
              wsUrl: "$wsURL/ws/start-menu",
              jwtToken: authenticationProvider.player?.jwtToken
            )
          ), 
          create: (BuildContext context) => StartMenuWSProvider(
            wsService: StartMenuWSService(
              wsUrl: "$wsURL/ws/start-menu",
              jwtToken: null
            )
          )
        ),
        ChangeNotifierProxyProvider<AuthenticationProvider, LobbyWSProvider>(
          update: (context, authenticationProvider, lobbyWSProvider) => LobbyWSProvider(
            wsService: GameWSService(
              wsUrl: "$wsURL/ws/games",
              jwtToken: authenticationProvider.player?.jwtToken
            )
          ), 
          create: (BuildContext context) => LobbyWSProvider(
            wsService: GameWSService(
              wsUrl: "$wsURL/ws/games",
              jwtToken: null
            )
          )
        ),
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

    return MaterialApp(
      title: "Scamlab - Game experiment",
      theme: materialTheme.light(),
      darkTheme: materialTheme.dark(),
      themeMode: ThemeMode.system,
      initialRoute: '/',
      routes: <String, WidgetBuilder>{
        '/': (BuildContext context) => ClearableExceptionListener<AuthenticationProvider>(
          message: "Couldn't get a new identity",
          child: const HomePage(),
        ),
        '/lobby': (BuildContext context) => const WaitingLobbyPage(),
      },
    );
  }
}
