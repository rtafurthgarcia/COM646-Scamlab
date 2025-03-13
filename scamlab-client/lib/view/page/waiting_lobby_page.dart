import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/lobby_ws_provider.dart';
import 'package:scamlab/view/widget/rules_card_widget.dart';

class WaitingLobbyPage extends StatefulWidget {
  const WaitingLobbyPage({super.key});

  @override
  State<WaitingLobbyPage> createState() => _WaitingLobbyPageState();
}

class _WaitingLobbyPageState extends State<WaitingLobbyPage> {
  @override
  void initState() {
    super.initState();
    Provider.of<LobbyWSProvider>(context, listen: false).startListening(); 
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(Icons.arrow_back),
          onPressed: () {
            Navigator.pop(context);
          },
        ),
        title: Consumer<LobbyWSProvider>(
          builder: (context, provider, child) {
            return Row(
              children: [
                Text("Scamlab - Player's username: "),
                SelectableText(provider.getLastMessageOfType<WaitingLobbyAssignedStrategyMessage>()?.username ?? "-")
              ]   
            );
          },
        ),
      ),
      body: Center(
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

                  if (lastMessage is WaitingLobbyReasonForWaitingMessage 
                  || lastMessage is WaitingLobbyAssignedStrategyMessage
                  || lastMessage == null) {
                    children.add(CircularProgressIndicator());
                  }

                  if (lastMessage is WaitingLobbyReasonForWaitingMessage) {
                    for (var reason in lastMessage.reasons) {
                      children.add(Text(reason));
                    }
                  }

                  return Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    spacing: 16,
                    children: children
                  );
                }
              ),
              Consumer<LobbyWSProvider>(
                builder: (context, provider, child) {
                  var assignedStrategy = provider.getLastMessageOfType<WaitingLobbyAssignedStrategyMessage>();
                  if (assignedStrategy != null) {
                    return Column(
                      children: [
                        InstructionsCardWidget(title: "This game's scenario:", text: assignedStrategy.script),
                        InstructionsCardWidget(
                          title: "Your role as a player:", 
                          text: assignedStrategy.role.replaceFirst(assignedStrategy.role[0], assignedStrategy.role[0].toUpperCase())
                        ),
                        InstructionsCardWidget(title: "Example of what you can say:", text: "\"${assignedStrategy.example}\""),
                      ],
                    );
                  } else {
                    return Column(
                      children: [
                        InstructionsCardWidget(
                          title: "Rules reminder:",
                        ),
                      ],
                    );
                  }

                }
              ),
              Row(
                children: [
                  Consumer<LobbyWSProvider>(
                    builder: (context, provider, child) {
                      return ElevatedButton.icon(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Theme.of(context).colorScheme.secondary,
                          foregroundColor:
                              Theme.of(context).colorScheme.onSecondary,
                          iconColor: Theme.of(context).colorScheme.onSecondary,
                        ),
                        onPressed: provider.mayStillStart ? provider.voteToStart : null, 
                        icon: Icon(Icons.videogame_asset),
                        label: Text('Start'),
                      );
                    }
                  ),
                  Spacer(), 
                  Consumer<LobbyWSProvider>(
                    builder: (context, lobbyWSProvider, child) {
                      return Flexible(
                        child: CheckboxListTile(
                          tristate: false,
                          value: lobbyWSProvider.dontWaitNextTime,
                          onChanged: (value) {
                            lobbyWSProvider.dontWaitNextTime = value ?? false;
                          },
                          title: const Text(
                            "Don't wait next time"
                          ),
                        ),
                      );
                    }
                  ),
                ],
              )
            ],
          ),
        ),
      ),
    );
  }
}