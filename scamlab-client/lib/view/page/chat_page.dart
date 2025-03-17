import 'dart:math';

import 'package:chat_bubbles/bubbles/bubble_special_one.dart';
import 'package:chat_bubbles/message_bars/message_bar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_window_close/flutter_window_close.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/ws_message.dart';
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

  Color colorFromUsername(String username) {
    // Create a seed by combining the char codes.
    final seed = username.codeUnits.fold(0, (prev, curr) => prev + curr);
    final random = Random(seed);
    
    return Color.fromARGB(
      255,
      random.nextInt(256),
      random.nextInt(256),
      random.nextInt(256),
    );
  } 

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProxyProvider<AuthenticationProvider, ChatWSProvider>(
      update: (context, authenticationProvider, chatWSProvider) {
        chatWSProvider!.stopListening();
        chatWSProvider.wsService.jwtToken =
            authenticationProvider.player?.jwtToken;
        chatWSProvider.startListening();
        return chatWSProvider;
      },
      create: (context) {
        const wsURL = String.fromEnvironment(
          'WS_URL',
          defaultValue: 'ws://127.0.0.1:8080',
        );

        Game game = context.read();

        return ChatWSProvider(
          wsService: ChatWSService(
            wsUrl: "$wsURL/ws/games/${game.conversationId}",
          ),
          gameService: context.read(),
          game: game,
        );
      },
      child: Scaffold(
        appBar: AppBar(title: buildTitle()),
        body: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 960),
            child: buildStack(context),
          ),
        ),
      ),
    );
  }

  Widget buildStack(BuildContext context) {
    return Container(
      margin: EdgeInsets.only(top: 8, bottom: 16.0),
      decoration: BoxDecoration(
        color: Colors.white,
        //borderRadius: BorderRadius.all(Radius.circular(16.0)),
        boxShadow: [
          BoxShadow(
            color: Theme.of(
              context,
            ).colorScheme.onTertiary.withValues(alpha: 0.5),
            spreadRadius: 5,
            blurRadius: 7,
            //offset: Offset(0, 3), // changes position of shadow
          ),
        ],
      ),
      child: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: EdgeInsets.all(16.0),
              child: Column(
                children: <Widget>[
                  StreamProvider<List<GamePlayersMessage>>(
                    create:
                        (context) =>
                            Provider.of<ChatWSProvider>(
                              context,
                              listen: false,
                            ).messagesStream,
                    builder: (context, snapshot) {
                      return Consumer<List<GamePlayersMessage>>(
                        builder: (context, messages, child) => ListView.builder(
                          itemCount: messages.length,
                          itemBuilder: (context, index) {
                            GamePlayersMessage message = messages[index];

                            return Row(
                              children: [
                                CircleAvatar(
                                  backgroundColor: colorFromUsername(message.senderUsername),
                                  child: Text(message.senderUsername.substring(0, 1)),
                                ),
                                BubbleSpecialOne(
                                  text: message.text,
                                  isSender: message.isSender,
                                  color: Theme.of(context).colorScheme.onPrimary,
                                  textStyle: Theme.of(context).textTheme.bodyMedium!,
                                ),
                              ],
                            );
                          },
                        ),
                      );
                    },
                    initialData: [],
                  ),
                ],
              ),
            ),
          ),
          const Divider(height: 1, color: Color(0xffe6e6e6)),
          MessageBar(
            onSend: (text) {
              Provider.of<ChatWSProvider>(context,listen: false).sendNewMessage(text);
            },
            actions: [
              InkWell(
                child: Icon(
                  Icons.add,
                  color: Theme.of(context).primaryColor,
                  size: 24,
                ),
                onTap: () {},
              ),
              Padding(
                padding: EdgeInsets.only(left: 8, right: 8),
                child: InkWell(
                  child: Icon(
                    Icons.camera_alt,
                    color: Theme.of(context).primaryColor,
                    size: 24,
                  ),
                  onTap: () {},
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Consumer<Game> buildTitle() {
    return Consumer<Game>(
      builder: (context, game, child) {
        return Row(
          children: [
            Text("Scamlab - Conversation ID:"),
            SelectableText(game.conversationId),
          ],
        );
      },
    );
  }
}
