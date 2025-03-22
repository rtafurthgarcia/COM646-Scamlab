import 'dart:math';

import 'package:chat_bubbles/bubbles/bubble_special_one.dart';
import 'package:chat_bubbles/message_bars/message_bar.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_window_close/flutter_window_close.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/chat_ws_provider.dart';

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  var _alertShowing = false;
  late String id;

  Future askBeforeQuitting() {
    return showDialog(
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
                Navigator.of(context).popUntil(ModalRoute.withName('/'));
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
  }

  @override
  void initState() {
    super.initState();

    if (!kDebugMode) {
      FlutterWindowClose.setWindowShouldCloseHandler(() async {
        if (_alertShowing) return false;
        _alertShowing = true;

        return await askBeforeQuitting();
      });
    }
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
    final arguments =
        ModalRoute.of(context)?.settings.arguments as Map<String, dynamic>?;
    id = arguments?['id'];

    return MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (context) {
            const wsURL = String.fromEnvironment(
              'WS_URL',
              defaultValue: 'ws://127.0.0.1:8080',
            );

            return ChatWSProvider(
              wsService:
                  context.read()
                    ..wsUrl = "$wsURL/ws/games/$id"
                    ..jwtToken =
                        context.read<AuthenticationProvider>().player?.jwtToken,
              gameService: context.read(),
            )..startListening();
          },
        ),
        StreamProvider<List<GamePlayersMessage>>(
          create: (context) => context.read<ChatWSProvider>().messagesStream,
          initialData: <GamePlayersMessage>[],
        ),
      ],
      child: Scaffold(
        appBar: AppBar(title: buildTitle(), leading: buildLeading(context)),
        body: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 960),
            child: buildStack(context),
          ),
        ),
      ),
    );
  }

  Widget buildLeading(BuildContext context) {
    return Consumer<ChatWSProvider>(
      builder: (context, provider, child) {
        return IconButton(
          icon: Icon(Icons.arrow_back),
          onPressed: () async {
            if (provider.game.currentState == provider.game.isWaiting) {
              Navigator.pop(context);
            } else {
              await askBeforeQuitting();
            }
          },
        );
      },
    );
  }

  Widget buildStack(BuildContext context) {
    return Container(
      margin: EdgeInsets.only(top: 8, bottom: 16.0),
      decoration: BoxDecoration(
        color: Colors.white,
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
                  Consumer<List<GamePlayersMessage>>(
                    builder:
                        (context, messages, child) => ListView.builder(
                          shrinkWrap: true,
                          itemCount: messages.length,
                          itemBuilder: (context, index) {
                            GamePlayersMessage message = messages[index];

                            return Row(
                              children: [
                                CircleAvatar(
                                  backgroundColor: colorFromUsername(
                                    message.senderUsername,
                                  ),
                                  child: Text(
                                    message.senderUsername.substring(0, 1),
                                  ),
                                ),
                                BubbleSpecialOne(
                                  text: message.text,
                                  isSender: message.isSender,
                                  color:
                                      Theme.of(context).colorScheme.onPrimary,
                                  textStyle:
                                      Theme.of(context).textTheme.bodyMedium!,
                                ),
                              ],
                            );
                          },
                        ),
                  ),
                ],
              ),
            ),
          ),
          const Divider(height: 1, color: Color(0xffe6e6e6)),
          MessageBar(
            onSend: (text) {
              context.read<ChatWSProvider>().sendNewMessage(text);
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

  Widget buildTitle() {
    bool isScreenSmall = MediaQuery.of(context).size.width < 600;
    String titleSuffix = id;
    // Shorten the ID if on mobile and if it's long enough.
    String displayedId = titleSuffix;
    if (isScreenSmall && titleSuffix.length > 10) {
      displayedId =
          "${titleSuffix.substring(0, 4)}...${titleSuffix.substring(titleSuffix.length - 2)}";
    }

    return Row(
      children: [
        Text("Scamlab - Conversation ID:"),
        isScreenSmall
            ? GestureDetector(
              onTap: () {
                Clipboard.setData(ClipboardData(text: titleSuffix));
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text("Conversation ID copied to clipboard!"),
                  ),
                );
              },
              child: Text(
                displayedId,
                style: const TextStyle(decoration: TextDecoration.underline),
              ),
            )
            : SelectableText(titleSuffix),
      ],
    );
  }
}
