import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/model/game.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/home_provider.dart';
import 'package:scamlab/view/widget/rules_card_widget.dart';

class HomePage extends StatefulWidget  {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with RouteAware {
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Subscribe to route changes.
    context.read<RouteObserver<PageRoute>>().subscribe(this, ModalRoute.of(context) as PageRoute);
  }

  @override
  void dispose() {
    context.read<RouteObserver<PageRoute>>().unsubscribe(this);
    super.dispose();
  }

  @override
  void didPushNext() {
    context.read<HomeProvider>().stopListening();
  }

  @override
  void didPopNext() {
    var provider = context.read<HomeProvider>()..startListening();

    onBackOnHomePage(provider.game);

    provider.game = Game();
  }

  void onBackOnHomePage(Game game) {
    if (game.error is Error || game.error is Exception) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: SelectableText(
            "Your gameplay got unexpectedly interrupted: ${game.error}",
            style: Theme.of(context).textTheme.labelMedium?.copyWith(color: Theme.of(context).colorScheme.onErrorContainer),
          ),
          backgroundColor: Theme.of(context).colorScheme.errorContainer,
          showCloseIcon: true,
          closeIconColor: Theme.of(context).colorScheme.onErrorContainer,
          duration: Duration(seconds: 60)
        ),
      );
    }

    if (game.reasonForCancellation != null) {
      var message = "Your game got interrupted due to the following reason: ${game.reasonForCancellation?.message}";

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: SelectableText(
            message,
            //style: Theme.of(context).textTheme.labelMedium?.copyWith(color: Theme.of(context).colorScheme.onSurface)
            style: Theme.of(context).textTheme.labelMedium?.copyWith(color: Theme.of(context).colorScheme.onErrorContainer),
          ),
          backgroundColor: Theme.of(context).colorScheme.errorContainer,
          showCloseIcon: true,
          closeIconColor: Theme.of(context).colorScheme.onErrorContainer,
          duration: Duration(seconds: 60)
        ),
      );
    }

    if (game.isFinished == game.currentState) {
      var message = "Thanks for playing! Don't forget to participate in the post-game survey ❤️";

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: SelectableText(
            message,
          ),
          showCloseIcon: true,
          duration: Duration(seconds: 15)
        ),
      );
    }
  }

  List<Widget> buildButtons(BuildContext context) {
    return [
      Consumer<AuthenticationProvider>(
        builder: (context, provider, child) {
          return ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.secondary,
              foregroundColor: Theme.of(context).colorScheme.onSecondary,
              iconColor: Theme.of(context).colorScheme.onSecondary,
            ),
            onPressed:
                () =>
                    provider.player != null
                      ? Navigator.pushNamed(context, '/lobby')
                      : null,
            icon: Icon(Icons.videogame_asset),
            label: Text('New game'),
          );
        },
      ),
      Consumer<AuthenticationProvider>(
        builder: (context, provider, child) {
          if (provider.player?.systemRole == "USER") {
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
              onPressed:
                  () =>
                      provider.player != null
                          ? Navigator.pushNamed(context, '/lobby')
                          : null,
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
      appBar: AppBar(title: buildTitle(isScreenSmall)),
      body: buildBody(context, isScreenSmall),
    );
  }

  Center buildBody(BuildContext context, bool isScreenSmall) {
    return Center(
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
                      child: Consumer<HomeProvider>(
                        builder: (context, provider, child) {
                          return Text(
                            "Players online: ${provider.playersCount ?? "-"}",
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
            InstructionsCard(
              title: "2. How to play",
              icon: const Icon(Icons.menu_book),
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
    );
  }

  Consumer<AuthenticationProvider> buildTitle(bool isScreenSmall) {
    return Consumer<AuthenticationProvider>(
      builder: (context, provider, child) {
        if (!provider.isLoading) {
          String titleSuffix =
              provider.player != null ? provider.player!.secondaryId : "-";
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
                    provider.isLoading
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
                                  ElevatedButton(
                                    onPressed:
                                        () => Navigator.pop(context, false),
                                    child: const Text('No'),
                                  ),
                                  ElevatedButton(
                                    onPressed:
                                        () => Navigator.pop(context, true),
                                    child: const Text('Yes'),
                                  ),
                                ],
                              );
                            },
                          );
                          if (confirm == true) {
                            provider.dontWaitNextTime = false;
                            provider.tryAgain();
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
    );
  }
}
