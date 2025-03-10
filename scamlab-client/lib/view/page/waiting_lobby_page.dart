import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/provider/lobby_ws_provider.dart';
import 'package:scamlab/view/widget/rules_card_widget.dart';

class WaitingLobbyPage extends StatelessWidget {
  const WaitingLobbyPage({super.key});

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
          builder: (context, lobbyWSProvider, child) {
            return Row(
              children: [
                Text("Scamlab - Player's username: "),
                SelectableText("TEST")
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
            spacing: 16,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                spacing: 16,
                children: [
                  CircularProgressIndicator(),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text("Waiting on other players to join..."),
                      Text("Waiting on a free lobby..."),
                    ],
                  ),
                ],
              ),
              RulesCardWidget(title: "Rules reminder:",),
              Row(
                children: [
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.secondary,
                    foregroundColor:
                        Theme.of(context).colorScheme.onSecondary,
                    iconColor: Theme.of(context).colorScheme.onSecondary,
                  ),
                  onPressed: null,
                  icon: Icon(Icons.videogame_asset),
                  label: Text('Start'),
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
              ],)
            ],
          ),
        ),
      ),
    );
  }

}