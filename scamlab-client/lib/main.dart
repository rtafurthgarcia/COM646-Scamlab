import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/provider/player_provider.dart';
import 'package:scamlab/provider/websocket_provider.dart';
import 'package:scamlab/service/conversation_ws_service.dart';
import 'package:scamlab/service/player_service.dart';
import 'package:scamlab/theme.dart';
import 'package:scamlab/view/page/home_page.dart';

void main() {

  const apiURL = String.fromEnvironment('API_URL', defaultValue: 'http://127.0.0.1:8080');
  const wsURL = String.fromEnvironment('WS_URL', defaultValue: 'ws://127.0.0.1:8080');

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create:
              (_) => PlayerProvider(
                playerService: PlayerService(
                  baseUrl: '$apiURL/api',
                ),
              ),
        ),
        ChangeNotifierProvider(
          create:
              (_) => ConversationWSProvider(
                wsService: ConversationWSService(
                  wsUrl: '$wsURL/ws/conversation/start',
                ),
              ),
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
        '/': (BuildContext context) => const HomePage(),
        //'/signup': (BuildContext context) => const SignUpPage(),
      },
    );
  }
}
