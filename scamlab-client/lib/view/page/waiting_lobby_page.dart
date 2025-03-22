import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_window_close/flutter_window_close.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/lobby_ws_provider.dart';
import 'package:scamlab/view/widget/rules_card_widget.dart';
import 'package:scamlab/view/widget/timout_timer_widget.dart';

class WaitingLobbyPage extends StatefulWidget {
  const WaitingLobbyPage({super.key});

  @override
  State<WaitingLobbyPage> createState() => _WaitingLobbyPageState();
}

class _WaitingLobbyPageState extends State<WaitingLobbyPage> {
  var _alertShowing = false;
  bool _hasNavigated = false; // Flag to ensure navigation only happens once

  Future askBeforeQuitting() {
    return showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Leave despite game ready?'),
          content: const Text(
            'A new game is about to start. Are you sure you want to quit now?',
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

    if (! kDebugMode) {
      FlutterWindowClose.setWindowShouldCloseHandler(() async {
        if (_alertShowing) return false;
        _alertShowing = true;

        return await askBeforeQuitting();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create:
          (BuildContext context) => LobbyWSProvider(
            gameService:
                context.read()
                  ..jwtToken =
                      context.read<AuthenticationProvider>().player?.jwtToken,
            wsService:
                context.read()
                  ..jwtToken =
                      context.read<AuthenticationProvider>().player!.jwtToken,
            settingsService: context.read()
          )..startListening(),
      builder:
          (context, child) => Scaffold(
            appBar: AppBar(
              leading: Consumer<LobbyWSProvider>(
                builder: (context, provider, child) {
                  return IconButton(
                    icon: Icon(Icons.arrow_back),
                    onPressed: () async {
                      if (provider.game.currentState ==
                          provider.game.isWaiting) {
                        Navigator.pop(context);
                      } else {
                        await askBeforeQuitting();
                      }
                    },
                  );
                },
              ),
              title: buildTitle(),
            ),
            // Use a Stack so we can overlay a Consumer that checks for state changes
            body: Stack(
              children: [
                buildBody(),
                // Consumer that listens for game state change to IsRunning
                Consumer<LobbyWSProvider>(
                  builder: (context, provider, child) {
                    if (!_hasNavigated &&
                        provider.game.currentState == provider.game.isRunning) {
                      _hasNavigated = true;
                      // Schedule the navigation after the current frame
                      WidgetsBinding.instance.addPostFrameCallback((_) {
                        Navigator.pushNamed(
                          context,
                          '/games',
                          arguments: {
                            'id': provider.game.conversationSecondaryId,
                          },
                        );
                      });
                    }
                    return const SizedBox.shrink();
                  },
                ),
              ],
            ),
          ),
    );
  }

  Consumer<LobbyWSProvider> buildTitle() {
    return Consumer<LobbyWSProvider>(
      builder: (context, provider, child) {
        List<Widget> children = List.empty(growable: true);

        if (provider.game.currentState ==
            provider.game.isWaitingForStartOfGame) {
          children.add(LinearProgressIndicator());
        }

        children.add(Text("Scamlab - Player's username: "));
        children.add(
          SelectableText(
            provider.game.isGameAssigned ? provider.game.username! : "-",
          ),
        );

        return Row(children: children);
      },
    );
  }

  Center buildBody() {
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 600),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min, // Constrain the height to the content
          spacing: 16,
          children: [
            Consumer<LobbyWSProvider>(
              builder: (context, provider, child) {
                var children = List<Widget>.empty(growable: true);
                var lastMessage = provider.getLastMessage();

                if (lastMessage == null) {
                  children.add(
                    const Text("Loading the new gameplay's strategy and role"),
                  );
                }

                if (provider.game.currentState == provider.game.isWaiting) {
                  children.add(CircularProgressIndicator());
                  var reasonsForWaiting =
                      provider
                          .getLastMessageOfType<
                            WaitingLobbyReasonForWaitingMessage
                          >();

                  if (reasonsForWaiting != null) {
                    for (var reason in reasonsForWaiting.reasons) {
                      children.add(Text(reason));
                    }
                  }
                }

                return Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  spacing: 16,
                  children: children,
                );
              },
            ),
            Consumer<LobbyWSProvider>(
              builder: (context, provider, child) {
                var assignedStrategy =
                    provider
                        .getLastMessageOfType<
                          WaitingLobbyGameAssignmentMessage
                        >();
                if (assignedStrategy != null) {
                  return Column(
                    children: [
                      InstructionsCardWidget(
                        title: "1. This game's scenario:",
                        text: assignedStrategy.script,
                        icon: const Icon(Icons.menu_book),
                      ),
                      InstructionsCardWidget(
                        title: "2. Your role as a player:",
                        text: assignedStrategy.role.replaceFirst(
                          assignedStrategy.role[0],
                          assignedStrategy.role[0].toUpperCase(),
                        ),
                        icon: const Icon(Icons.person),
                      ),
                      InstructionsCardWidget(
                        title: "3. Example of what you can say:",
                        text: "\"${assignedStrategy.example}\"",
                        icon: const Icon(Icons.message),
                      ),
                    ],
                  );
                } else {
                  return Column(
                    children: [
                      InstructionsCardWidget(
                        title: "Rules reminder:",
                        icon: const Icon(Icons.menu_book),
                      ),
                    ],
                  );
                }
              },
            ),
            Row(
              children: [
                Consumer<LobbyWSProvider>(
                  builder: (context, provider, child) {
                    return ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor:
                            Theme.of(context).colorScheme.secondary,
                        foregroundColor:
                            Theme.of(context).colorScheme.onSecondary,
                        iconColor: Theme.of(context).colorScheme.onSecondary,
                      ),
                      onPressed:
                          provider.game.currentState == provider.game.isReady
                              ? provider.voteToStart
                              : null,
                      icon: Icon(Icons.videogame_asset),
                      label: Text('Start'),
                    );
                  },
                ),
                SizedBox.square(dimension: 8.0),
                Consumer<LobbyWSProvider>(
                  builder: (context, provider, child) {
                    if (provider.game.currentState == provider.game.isReady) {
                      var timeout =
                          provider
                              .getLastMessageOfType<
                                WaitingLobbyReadyToStartMessage
                              >()
                              ?.voteTimeout;
                      return TimoutTimerWidget(
                        duration: Duration(seconds: timeout ?? 0),
                      );
                    }

                    return SizedBox.shrink();
                  },
                ),
                Spacer(),
                Consumer<LobbyWSProvider>(
                  builder: (context, lobbyWSProvider, child) {
                    return Flexible(
                      child: CheckboxListTile(
                        key: Key("checkboxDontWaitNextTime"),
                        tristate: false,
                        value: lobbyWSProvider.dontWaitNextTime,
                        onChanged: (value) {
                          lobbyWSProvider.dontWaitNextTime = value ?? false;
                        },
                        title: const Text("Don't wait next time"),
                      ),
                    );
                  },
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
