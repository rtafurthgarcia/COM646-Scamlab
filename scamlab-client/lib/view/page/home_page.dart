import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/startmenu_ws_provider.dart';
import 'package:scamlab/view/widget/rules_card_widget.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  List<Widget> buildButtons(BuildContext context) {
    return [
      Consumer<AuthenticationProvider>(
        builder: (context, authenticationProvider, child) {
          return ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.secondary,
              foregroundColor: Theme.of(context).colorScheme.onSecondary,
              iconColor: Theme.of(context).colorScheme.onSecondary,
            ),
            onPressed: () => authenticationProvider.player != null ? Navigator.pushNamed(context, '/lobby') : null,
            icon: Icon(Icons.videogame_asset),
            label: Text('New game'),
          );
        },
      ),
      Consumer<AuthenticationProvider>(
        builder: (context, authenticationProvider, child) {
          if (authenticationProvider.player?.systemRole == "USER") {
            return ElevatedButton.icon(
              onPressed: null,
              style: ElevatedButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.secondary,
                foregroundColor: Theme.of(context).colorScheme.onSecondary,
                iconColor: Theme.of(context).colorScheme.onSecondary,
              ),
              icon: Icon(Icons.dashboard),
              label: Text('Dashboard (admin-only)'),
            );
          } else {
            return ElevatedButton.icon(
              onPressed: () => authenticationProvider.player != null ? Navigator.pushNamed(context, '/lobby') : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.secondary,
                foregroundColor: Theme.of(context).colorScheme.onSecondary,
                iconColor: Theme.of(context).colorScheme.onSecondary,
              ),
              icon: Icon(Icons.dashboard),
              label: Text('Dashboard'),
            );
          }
        },
      ),
    ];
  }

  @override
  Widget build(BuildContext context) {
    bool isScreenSmall = MediaQuery.of(context).size.width < 600;

    return Scaffold(
      appBar: AppBar(
        title: Consumer<AuthenticationProvider>(
          builder: (context, authenticationProvider, child) {
            if (!authenticationProvider.isLoading) {
              String titleSuffix =
                  authenticationProvider.player != null
                      ? authenticationProvider.player!.secondaryId
                      : "-";
              // Shorten the ID if on mobile and if it's long enough.
              String displayedId = titleSuffix;
              if (isScreenSmall && titleSuffix.length > 10) {
                displayedId =
                    "${titleSuffix.substring(0, 4)}...${titleSuffix.substring(titleSuffix.length - 2)}";
              }

              return Row(
                children: [
                  const Text("Scamlab - Player's ID: "),
                  // If on mobile, show a tappable text that copies the full ID to clipboard.
                  isScreenSmall
                      ? GestureDetector(
                        onTap: () {
                          Clipboard.setData(ClipboardData(text: titleSuffix));
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text("Player ID copied to clipboard!"),
                            ),
                          );
                        },
                        child: Text(
                          displayedId,
                          style: const TextStyle(
                            decoration: TextDecoration.underline,
                          ),
                        ),
                      )
                      : SelectableText(titleSuffix),
                  IconButton(
                    onPressed:
                        authenticationProvider.isLoading
                            ? null
                            : () async {
                              final bool? confirm = await showDialog<bool>(
                                context: context,
                                builder: (BuildContext context) {
                                  return AlertDialog(
                                    title: const Text('Confirm New Identity'),
                                    content: const Text(
                                      'Are you sure you want to generate a new identity?',
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
                                authenticationProvider.tryAgain();
                              }
                            },
                    icon: const Icon(Icons.refresh_sharp),
                  ),
                ],
              );
            } else {
              return const LinearProgressIndicator();
            }
          },
        ),
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 600),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            spacing: 16,
            children: [
              Row(
                children: [
                  Card(
                    color: Theme.of(context).colorScheme.onPrimary,
                    child: SizedBox(
                      width: 150,
                      child: Center(
                        child: Consumer<StartMenuWSProvider>(
                          builder: (context, conversationWSProvider, child) {
                            return Text(
                              "Players online: ${conversationWSProvider.chatMessage == null ? "-" : conversationWSProvider.chatMessage?.numberOfPlayersConnected}",
                            );
                          },
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              Card(
                child: Column(
                  children: [
                    ListTile(
                      titleTextStyle: Theme.of(
                        context,
                      ).textTheme.headlineSmall!.copyWith(
                        color:
                            Theme.of(
                              context,
                            ).colorScheme.primary, // Use primary color
                      ),
                      subtitleTextStyle: Theme.of(
                        context,
                      ).textTheme.labelMedium?.copyWith(
                        fontStyle: FontStyle.italic,
                        color: Theme.of(context).colorScheme.secondary,
                      ),
                      visualDensity: VisualDensity.comfortable,
                      leading: const Icon(Icons.question_mark),
                      title: Text("1. What's the game about"),
                      subtitle: const Text(
                        textAlign: TextAlign.justify,
                        "(Think \"Among Us\" meets phishing scams, with AI chaos!)",
                      ),
                    ),
                    Container(
                      padding: EdgeInsets.only(left: 16, right: 16, bottom: 16),
                      alignment: Alignment.centerLeft,
                      child: Text(
                        "Dive into a 5-minute chat showdown where you unmask an AI impostor, or become one. Use scripted scams (fake job offers, tech traps) to bait clues or bluff your way to victory. Correct votes earn candy; wrong ones let the bot reign! 🕵️🍬",
                        textAlign: TextAlign.justify,
                      ),
                    ),
                  ],
                ),
              ),
              InstructionsCardWidget(title: "2. How to play"),
              Card(
                child: Column(
                  children: [
                    ListTile(
                      titleTextStyle: Theme.of(
                        context,
                      ).textTheme.headlineSmall!.copyWith(
                        color:
                            Theme.of(
                              context,
                            ).colorScheme.primary, // Use primary color
                      ),
                      subtitleTextStyle: Theme.of(
                        context,
                      ).textTheme.labelMedium?.copyWith(
                        fontStyle: FontStyle.italic,
                        color: Theme.of(context).colorScheme.secondary,
                      ),
                      visualDensity: VisualDensity.comfortable,
                      leading: const Icon(Icons.not_interested),
                      title: Text("3. What's off limits"),
                      subtitle: const Text(
                        textAlign: TextAlign.justify,
                        "(Violations invalidate your game and rewards!)",
                      ),
                    ),
                    Container(
                      padding: EdgeInsets.only(left: 16, right: 16, bottom: 16),
                      alignment: Alignment.centerLeft,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            '1. Stick to fictional/scenario details only: Never personal data, links, or real identities.',
                          ),
                          Text(
                            '2. No IRL Coordination: Interact in-game only! No external chats to reveal identities.',
                          ),
                          Text(
                            '3. No harassment, hate speech, or explicit content.',
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              Flex(
                direction: isScreenSmall ? Axis.vertical : Axis.horizontal,
                spacing: 32.0,
                mainAxisAlignment: MainAxisAlignment.center,
                children: buildButtons(context),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
