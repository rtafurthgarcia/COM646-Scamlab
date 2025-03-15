import 'package:flutter/material.dart';
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
  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProxyProvider<AuthenticationProvider, LobbyWSProvider>(
      update: (context, authenticationProvider, lobbyWSProvider) {
        lobbyWSProvider!.stopListening();
        lobbyWSProvider.gameService.jwtToken = authenticationProvider.player?.jwtToken;
        lobbyWSProvider.wsService.jwtToken = authenticationProvider.player?.jwtToken;
        lobbyWSProvider.startListening();
        return lobbyWSProvider;
      }, 
      create: (BuildContext context) => LobbyWSProvider(
        wsService: context.read(),
        gameService: context.read(),
        game: context.read()
      ),
      child: Scaffold(
          appBar: AppBar(
            leading: Consumer<LobbyWSProvider>(
              builder: (context, provider, child) {
                return IconButton(
                  icon: Icon(Icons.arrow_back),
                  onPressed: () async {
                    if (provider.game.stateMachine.current == provider.game.isWaiting) {
                      Navigator.pop(context);
                    } else {
                      final bool? confirm = await showDialog<bool>(
                        context: context,
                        builder: (BuildContext context) {
                          return AlertDialog(
                            title: const Text('Leave despite game ready?'),
                            content: const Text(
                              'A new game is about to start. Are you sure you want to quit now?',
                            ),
                            actions: <Widget>[
                              TextButton(
                                onPressed:
                                    () => Navigator.pop(context, false),
                                child: const Text('No'),
                              ),
                              TextButton(
                                onPressed:
                                    () => Navigator.pop(context, true),
                                child: const Text('Yes'),
                              ),
                            ],
                          );
                        },
                      );

                      if (confirm == true) {
                        Navigator.pop(context);
                      }
                    }
                  },
                );
              }
            ),
            title: buildTitle(),
          ),
          body: buildBody(),
        )
    );
  }

  Consumer<LobbyWSProvider> buildTitle() {
    return Consumer<LobbyWSProvider>(
        builder: (context, provider, child) {
          return Row(
            children: [
              Text("Scamlab - Player's username: "),
              SelectableText(
                provider
                  .getLastMessageOfType<
                    WaitingLobbyAssignedStrategyMessage
                  >()
                  ?.username ??
              "-",
              ),
            ],
          );
        },
      );
  }

  Center buildBody() {
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 600),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize:
              MainAxisSize.min, // Constrain the height to the content
          spacing: 16,
          children: [
            Consumer<LobbyWSProvider>(
              builder: (context, provider, child) {
                var children = List<Widget>.empty(growable: true);
                var lastMessage = provider.getLastMessage();

                if (lastMessage == null) {
                  children.add(
                    const Text(
                      "Loading the new gameplay's strategy and role",
                    ),
                  );
                }

                if (provider.game.stateMachine.current == provider.game.isWaiting) {
                  children.add(CircularProgressIndicator());
                  var reasonsForWaiting = provider.getLastMessageOfType<WaitingLobbyReasonForWaitingMessage>();

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
                var assignedStrategy = provider.getLastMessageOfType<WaitingLobbyAssignedStrategyMessage>();
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
                          provider.game.stateMachine.current == provider.game.isReady
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
                    if (provider.game.stateMachine.current == provider.game.isReady) {
                      var timeout = provider.getLastMessageOfType<WaitingLobbyReadyToStartMessage>()?.voteTimeout;
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
