import 'package:flutter/material.dart';
import 'package:flutter_window_close/flutter_window_close.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/game.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/chat_ws_provider.dart';
import 'package:scamlab/service/chat_ws_service.dart';

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
 var _alertShowing = false;

  @override
  void initState() {
    super.initState();

    FlutterWindowClose.setWindowShouldCloseHandler(() async {
      if (_alertShowing) return false;
      _alertShowing = true;

      return await showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: const Text('Leave despite ongoing gameplay?'),
            content: const Text(
              'Quitting mid-game is not a very nice thing to do. Are you sure you want to quit now?',
            ),
            actions: [
              ElevatedButton(
                onPressed: () {
                  Navigator.of(context).pop(true);
                  _alertShowing = false;
                },
                child: const Text('Yes'),
              ),
              ElevatedButton(
                onPressed: () {
                  Navigator.of(context).pop(false);
                  _alertShowing = false;
                },
                child: const Text('No'),
              ),
            ],
          );
        },
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    bool isScreenSmall = MediaQuery.of(context).size.width < 600;

    return ChangeNotifierProxyProvider<
      AuthenticationProvider,
      ChatWSProvider
    >(
      update: (context, authenticationProvider, chatWSProvider) {
        chatWSProvider!.stopListening();
        chatWSProvider.wsService.jwtToken =
            authenticationProvider.player?.jwtToken;
        chatWSProvider.startListening();
        return chatWSProvider;
      },
      create:(context) {
        const wsURL = String.fromEnvironment(
          'WS_URL',
          defaultValue: 'ws://127.0.0.1:8080',
        );

        Game game = context.read();

        return ChatWSProvider(
          wsService: ChatWSService(wsUrl: "$wsURL/ws/games/${game.conversationId}"),
          gameService: context.read(),
          game: game
        );
      },
      child: Scaffold(
        appBar: AppBar(
          title: buildTitle(),
        ),
        //body: buildBody(),
      ),
    );
  }

  Consumer<Game> buildTitle() {
    return Consumer<Game>(
      builder: (context, game, child) {
        return Row(
          children: [
            Text("Scamlab - Conversation ID:"),
            SelectableText(
              game.conversationId
            ),
          ],
        );
      },
    );
  }
}