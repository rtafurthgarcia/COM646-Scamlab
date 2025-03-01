import 'package:flutter/material.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Scamlab')),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 600),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            spacing: 16,
            children: [
              Card(
                child: Column(
                  children: [
                    ListTile(
                      titleTextStyle:
                          Theme.of(context).textTheme.headlineSmall!,
                      subtitleTextStyle: Theme.of(context).textTheme.labelMedium
                          ?.copyWith(fontStyle: FontStyle.italic),
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
              Card(
                child: Column(
                  children: [
                    ListTile(
                      titleTextStyle:
                          Theme.of(context).textTheme.headlineSmall!,
                      visualDensity: VisualDensity.comfortable,
                      leading: const Icon(Icons.menu_book),
                      title: Text("2. How to play"),
                    ),
                    Container(
                      padding: EdgeInsets.only(left: 16, right: 16, bottom: 16),
                      alignment: Alignment.centerLeft,
                      child: Text(
                        "Join the game, chat/vote to unmask the AI bot hidden among players using scripted scenarios. Earn candy by voting correctly, or lose if the bot fools you—rate your confidence post-game. Stay anonymous: new username each round.",
                        textAlign: TextAlign.justify,
                      ),
                    ),
                  ],
                ),
              ),
              Card(
                child: Column(
                  children: [
                    ListTile(
                      titleTextStyle:
                          Theme.of(context).textTheme.headlineSmall!,
                      subtitleTextStyle: Theme.of(context).textTheme.labelMedium
                          ?.copyWith(fontStyle: FontStyle.italic),
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
                          Text('1. Share only fictional/scenario details: Never personal data, links, or real identities.'),
                          Text('2. No IRL Coordination: Interact in-game only! No external chats to reveal identities.'),
                          Text('3. No harassment, hate speech, or explicit content.'),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                //crossAxisAlignment: CrossAxisAlignment.stretch,
                spacing: 32.0,
                children: [
                  ElevatedButton.icon(
                    onPressed: null,
                    icon: Icon(Icons.videogame_asset),
                    label: Text('New game'),
                  ),
                  ElevatedButton.icon(
                    onPressed: null,
                    icon: Icon(Icons.dashboard),
                    label: Text('Dashboard (admin-only)'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
