import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_window_close/flutter_window_close.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/chat_provider.dart';
import 'package:scamlab/view/widget/chat_bubble_widget.dart';
import 'package:scamlab/view/widget/rules_card_widget.dart';
import 'package:scamlab/view/widget/timout_timer_widget.dart';
import 'package:state_machine/state_machine.dart' as sm;

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> with RouteAware {
  var _alertShowing = false;
  late String _id;
  bool _hasNavigated = false; // Flag to ensure navigation only happens once
  late RouteObserver<PageRoute> _observer;

  final TextEditingController _textEditingController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final FocusNode _focusNode = FocusNode();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Subscribe to route changes.
    _observer = context.read<RouteObserver<PageRoute>>();
    _observer.subscribe(this, ModalRoute.of(context) as PageRoute);
  }

  @override
  void dispose() {
    _observer.unsubscribe(this);
    super.dispose();
  }

  @override
  void didPushNext() {
    context.read<ChatProvider>().stopListening();
  }

  @override
  void didPopNext() {
    context.read<ChatProvider>().startListening();
  }

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
        ModalRoute.of(context)?.settings.arguments as Map<dynamic, dynamic>?;
    _id = arguments?['id'];

    return MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (context) {
            const wsURL = String.fromEnvironment(
              'WS_URL',
              defaultValue: 'ws://127.0.0.1:8080',
            );
            return ChatProvider(
              wsService:
                  context.read()
                    ..wsUrl = "$wsURL/ws/games/$_id"
                    ..jwtToken =
                        context.read<AuthenticationProvider>().player?.jwtToken,
              gameService: context.read(),
            )..startListening();
          },
        ),
        StreamProvider<List<GamePlayersMessage>>(
          create: (context) => context.read<ChatProvider>().messagesStream,
          initialData: <GamePlayersMessage>[],
        ),
      ],
      builder:
          (context, child) => Scaffold(
            appBar: AppBar(title: buildTitle(), leading: buildLeading(context)),
            drawer: buildDrawer(),
            body: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 960),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    buildChatView(context),
                    buildChatBox(context),
                    buildEventListener(),
                  ],
                ),
              ),
            ),
          ),
    );
  }

  Widget buildEventListener() {
    // Consumer that listens for game state change
    return Consumer<ChatProvider>(
      builder: (context, provider, child) {
        if (!_hasNavigated) {
          if (provider.game.currentState == provider.game.isCancelled) {
            // Schedule the navigation after the current frame
            _hasNavigated = true;
            WidgetsBinding.instance.addPostFrameCallback((_) {
              Navigator.of(context).pop();
            });
          }

          if (provider.game.currentState == provider.game.isVoting) {
            // Schedule the navigation after the current frame
            _hasNavigated = true;
            WidgetsBinding.instance.addPostFrameCallback((_) {
              Navigator.pushNamed(
                context,
                '/votes',
                arguments: {'id': provider.game.conversationSecondaryId},
              );
            });
          }
        }
        return const SizedBox.shrink();
      },
    );
  }

  Drawer buildDrawer() {
    return Drawer(
      width: 440,
      child: Selector<ChatProvider, sm.State?>(
        selector: (context, provider) => provider.game.currentState,
        builder: (context, state, child) {
          var game = context.read<ChatProvider>().game;
          var timeBeforeVote = context.read<ChatProvider>().timeLeft;
          return Padding(
            padding: const EdgeInsets.all(8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text("Time before next vote:"),
                TimoutTimer(duration: Duration(seconds: timeBeforeVote)),
                Divider(),
                InstructionsCard(
                  title: "1. This game's scenario:",
                  text: game.script!,
                  icon: const Icon(Icons.menu_book),
                  withoutCard: true,
                ),
                InstructionsCard(
                  title: "2. Your role as a player:",
                  text: game.role!.replaceFirst(
                    game.role![0],
                    game.role![0].toUpperCase(),
                  ),
                  icon: const Icon(Icons.person),
                  withoutCard: true,
                ),
                InstructionsCard(
                  title: "3. Example of what you can say:",
                  text: "\"${game.example}\"",
                  icon: const Icon(Icons.message),
                  withoutCard: true,
                ),
                Divider(),
                Spacer(),
                ElevatedButton.icon(
                  icon: Icon(Icons.exit_to_app),
                  label: Text('Leave game'),
                  onPressed: () async {
                    if (game.currentState == game.isWaiting) {
                      Navigator.pop(context);
                    } else {
                      await askBeforeQuitting();
                    }
                  },
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget buildLeading(BuildContext context) {
    return Builder(
      builder: (context) {
        return IconButton(
          icon: Icon(Icons.help_center),
          onPressed: () => Scaffold.of(context).openDrawer(),
        );
      },
    );
  }

  Widget buildChatBox(BuildContext context) {
    return SizedBox(
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
              focusNode: _focusNode,
              controller: _textEditingController,
              textInputAction:
                  TextInputAction.send, // shows "send" on the keyboard
              onSubmitted: (value) {
                // Send the message when Enter is pressed.
                context.read<ChatProvider>().sendNewMessage(value);
                _textEditingController.clear();
                _focusNode.requestFocus();
              },
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
                    onPressed: () {
                      context.read<ChatProvider>().sendNewMessage(
                        _textEditingController.text,
                      );
                      _textEditingController.clear();
                      _focusNode.requestFocus();
                    },
                    icon: Icon(Icons.send),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget buildChatView(BuildContext context) {
    return Expanded(
      child: Consumer<List<GamePlayersMessage>>(
        builder: (context, messages, child) {
          // After the frame is rendered, scroll to the bottom.
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (_scrollController.hasClients) {
              _scrollController.jumpTo(
                _scrollController.position.maxScrollExtent,
              );
            }
          });

          return ListView.builder(
            controller: _scrollController,
            scrollDirection: Axis.vertical,
            itemCount: messages.length,
            itemBuilder: (context, index) {
              GamePlayersMessage message = messages[index];
              bool isPreviousMessageFromSamePlayer = false;

              isPreviousMessageFromSamePlayer =
                  (messages.length > 1 && index > 0) &&
                  (messages[index - 1].senderSecondaryId ==
                      message.senderSecondaryId);

              List<Widget> children = List.empty(growable: true);

              if (message.origin == MessageOrigin.other) {
                children.add(
                  isPreviousMessageFromSamePlayer
                      ? SizedBox(width: 48, height: 48)
                      : CircleAvatar(
                        radius: 24,
                        backgroundColor: colorFromUsername(
                          message.senderUsername,
                        ),
                        child: Text(message.senderUsername.substring(0, 1)),
                      ),
                );
              }

              var alignment = MainAxisAlignment.center;
              switch (message.origin) {
                case MessageOrigin.me:
                  children.add(OutChatBubble(
                      message: message.text,
                      time: message.time,
                      fromSamePersonAsPreviousOne:
                          isPreviousMessageFromSamePlayer,
                    ));
                  alignment = MainAxisAlignment.end;
                  break;
                case MessageOrigin.other:
                  children.add(
                    InChatBubble(
                      message: message.text,
                      from: message.senderUsername,
                      time: message.time,
                      fromSamePersonAsPreviousOne:
                          isPreviousMessageFromSamePlayer,
                    )
                  );
                  alignment = MainAxisAlignment.start;
                case MessageOrigin.system:
                  children.add(
                    InnerChatBubble(
                      message: message.text,
                      time: message.time,
                    )
                  );
                  alignment = MainAxisAlignment.center;
              }

              return Row(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: alignment,
                spacing: 16.0,
                children: children,
              );
            },
          );
        },
      ),
    );
  }

  Widget buildTitle() {
    return Consumer<ChatProvider>(
      builder: (context, provider, child) {
        String titleSuffix = provider.game.conversationSecondaryId!;
        String displayedId = titleSuffix;
        if (titleSuffix.length > 10) {
          displayedId =
              "${titleSuffix.substring(0, 4)}...${titleSuffix.substring(titleSuffix.length - 2)}";
        }

        return Row(
          children: [
            Text("Scamlab - Conversation ID:"),
            GestureDetector(
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
            ),
            Text(", you are playing as: "),
            SelectableText(provider.game.username!)
          ],
        );
      }
    );
  }
}
