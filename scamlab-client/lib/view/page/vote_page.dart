import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_window_close/flutter_window_close.dart';
import 'package:provider/provider.dart';
import 'package:scamlab/provider/authentication_provider.dart';
import 'package:scamlab/provider/chat_provider.dart';
import 'package:scamlab/provider/vote_provider.dart';
import 'package:scamlab/view/widget/timout_timer_widget.dart';
import 'package:url_launcher/url_launcher.dart';

class VotePage extends StatefulWidget {
  const VotePage({super.key});

  @override
  State<VotePage> createState() => _VotePageState();
}

class _VotePageState extends State<VotePage> {
  var _alertShowing = false;
  late String _id;
  bool _hasNavigated = false; // Flag to ensure navigation only happens once

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

  @override
  Widget build(BuildContext context) {
    final arguments =
        ModalRoute.of(context)?.settings.arguments as Map<dynamic, dynamic>?;
    _id = arguments?['id'];

    return ChangeNotifierProvider(
      create: (context) {
        const wsURL = String.fromEnvironment(
          'WS_URL',
          defaultValue: 'ws://127.0.0.1:8080',
        );

        return VoteProvider(
          wsService:
              context.read()
                ..wsUrl = "$wsURL/ws/games/$_id"
                ..jwtToken =
                    context.read<AuthenticationProvider>().player?.jwtToken,
          gameService: context.read(),
        )..startListening();
      },
      builder:
          (context, child) => Scaffold(
            appBar: AppBar(
              leading: Consumer<ChatProvider>(
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
              title: const Text("Scamlab: Time to vote!"),
            ),
            body: buildVotingBooth(context),
          ),
    );
  }

  Consumer<ChatProvider> buildEventsListener() {
    // Consumer that listens for game state change
    return Consumer<ChatProvider>(
      builder: (context, provider, child) {
        if (!_hasNavigated) {
          if (provider.game.currentState == provider.game.isCancelled) {
            // Schedule the navigation after the current frame
            _hasNavigated = true;
            WidgetsBinding.instance.addPostFrameCallback((_) {
              Navigator.popUntil(context, ModalRoute.withName('/'));
            });
          }

          if (provider.game.currentState == provider.game.isRunning) {
            // Schedule the navigation after the current frame
            _hasNavigated = true;
            WidgetsBinding.instance.addPostFrameCallback((_) {
              Navigator.pop(context);
            });
          }

          if (provider.game.currentState == provider.game.isFinished) {
            _hasNavigated = true;
            launchUrl(
              Uri.parse(
                "https://forms.office.com/Pages/ResponsePage.aspx?id=2Hi6C01P2U2bWu4SGxFu_upC6ffSPuZKg73H54wZ47NURTVIMENKUFRQVU42UDkzUjdMTUtPN1k2WC4u&r95b92478978b4877a86068c04f5f621e=${provider.game.conversationSecondaryId}",
              ),
            );

            WidgetsBinding.instance.addPostFrameCallback((_) {
              Navigator.popUntil(context, ModalRoute.withName('/'));
            });
          }
        }
        return const SizedBox.shrink();
      },
    );
  }

  Column buildVotingBooth(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Row(
          children: [
            const Text("Time to vote for the player you think to be the bot."),
            TimoutTimer(
              duration: Duration(
                seconds: context.read<VoteProvider>().game.voteTimeout!,
              ),
            ),
          ],
        ),
        buildEventsListener(),
        Consumer<VoteProvider>(
          builder: (context, provider, child) {
            return Row(
              children: [
                ElevatedButton(
                  onPressed: () {
                    var id =
                        provider.game.otherPlayers!.entries.first.key;
                    provider.castVote(id);
                  },
                  child: Text(
                    "Vote out ${provider.game.otherPlayers!.entries.first.value}",
                  ),
                ),
                ElevatedButton(
                  onPressed: () {
                    var id =
                        provider.game.otherPlayers!.entries.last.key;
                    provider.castVote(id);
                  },
                  child: Text(
                    "Vote out ${provider.game.otherPlayers!.entries.last.value}",
                  ),
                ),
                ElevatedButton(
                  onPressed: () {
                    var id =
                        provider.game.otherPlayers!.entries.last.key;
                    provider.castVote(id);
                  },
                  child: Text(
                    "Blank vote"
                  ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }
}
