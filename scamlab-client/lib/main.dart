import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/lobby_ws_provider.dart';
import 'package:scamlab/provider/startmenu_ws_provider.dart';
import 'package:scamlab/provider/state_machine_provider.dart';
import 'package:scamlab/service/lobby_ws_service.dart';
import 'package:scamlab/service/authentication_service.dart';
import 'package:scamlab/service/game_service.dart';
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
        ChangeNotifierProvider(
          create:
              (_) => StateMachineProvider(),
        ),
        ChangeNotifierProxyProvider<AuthenticationProvider, StartMenuWSProvider>(
          update: (context, authenticationProvider, startMenuWSProvider) {
            if (authenticationProvider.player != null) {
              startMenuWSProvider!.wsService.jwtToken = authenticationProvider.player!.jwtToken;
              startMenuWSProvider.startListening();
            } else {
              startMenuWSProvider!.wsService.jwtToken = null;
              startMenuWSProvider.stopListening();
            }
            return startMenuWSProvider;
          }, 
          create: (BuildContext context) => StartMenuWSProvider(
            wsService: LobbyWSService(
              wsUrl: "$wsURL/ws/start-menu"
            )
          )
        ),
        ChangeNotifierProxyProvider<AuthenticationProvider, LobbyWSProvider>(
          update: (context, authenticationProvider, lobbyWSProvider) {
            if (authenticationProvider.player != null) {
              lobbyWSProvider!.gameService.jwtToken = authenticationProvider.player!.jwtToken;
              lobbyWSProvider.wsService.jwtToken = authenticationProvider.player!.jwtToken;
            } else {
              lobbyWSProvider!.gameService.jwtToken = null;
              lobbyWSProvider.wsService.jwtToken = null;
              lobbyWSProvider.stopListening();
            }
            return lobbyWSProvider;
          }, 
          create: (BuildContext context) => LobbyWSProvider(
            wsService: LobbyWSService(
              wsUrl: "$wsURL/ws/games"
            ),
            gameService: GameService(
              baseUrl: '$apiURL/api'
            )
          )
        ),
        ChangeNotifierProxyProvider<AuthenticationProvider, StateMachineProvider>(
          update: (context, authenticationProvider, stateMachineProvider) => StateMachineProvider(),
          create: (BuildContext context) => StateMachineProvider()
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
          message: "Couldn't get a new identity.",
          child: const HomePage(),
        ),
        '/lobby': (BuildContext context) => ClearableExceptionListener<LobbyWSProvider>(
          message: "Couldn't join a new game.",
          child: const WaitingLobbyPage()
        )
      },
    );
  }
}
