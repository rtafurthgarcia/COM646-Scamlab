import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_window_close/flutter_window_close.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/chat_ws_provider.dart';
import 'package:scamlab/view/widget/chat_bubble_widget.dart';

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  var _alertShowing = false;
  late String id;

  TextEditingController textEditingController = TextEditingController();

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
          initialData: <GamePlayersMessage>[
            GamePlayersMessage(
              sequence: 0,
              senderSecondaryId: "blabla",
              senderUsername: "blabla",
              text: "Teehee",
            ),
          ],
        ),
      ],
      builder: (context, child) => Scaffold(
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
    return Stack(children: [buildChatView(), buildChatBox(context)]);
  }

  Widget buildChatBox(BuildContext context) {
    return Align(
      alignment: Alignment.bottomCenter,
      child: SizedBox(
        child: Row(
          children: <Widget>[
            ElevatedButton(
              onPressed: null,
              style: ElevatedButton.styleFrom(
                shape: CircleBorder(),
                padding: EdgeInsets.all(24),
                backgroundColor: Theme.of(context).colorScheme.secondary,
                foregroundColor: Theme.of(context).colorScheme.onSecondary,
                iconColor: Theme.of(context).colorScheme.onSecondary,
              ),
              child: Icon(Icons.photo_camera_back),
            ),
            Flexible(
              fit: FlexFit.loose,
              child: TextField(
                controller: textEditingController,
                decoration: InputDecoration(
                  hintText: "Write message...",
                  hintStyle: Theme.of(context).primaryTextTheme.bodyLarge,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(32.0),
                    borderSide: BorderSide(),
                  ),
                  suffixIcon: Container(
                    margin: EdgeInsets.all(8.0),
                    child: IconButton(
                      onPressed: () => Provider.of<ChatWSProvider>(context, listen: false).sendNewMessage(textEditingController.text),
                      icon: Icon(Icons.send),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget buildChatView() {
    return Align(
      alignment: Alignment.center,
      child: Consumer<List<GamePlayersMessage>>(
        builder:
            (context, messages, child) => ListView.builder(
              itemCount: messages.length,
              itemBuilder: (context, index) {
                GamePlayersMessage message = messages[index];
                return Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircleAvatar(
                      backgroundColor: colorFromUsername(
                        message.senderUsername,
                      ),
                      child: Text(message.senderUsername.substring(0, 1)),
                    ),
                    ChatBubble(
                      message: message.text,
                      isSender: message.isSender,
                    ),
                  ],
                );
              },
            ),
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
