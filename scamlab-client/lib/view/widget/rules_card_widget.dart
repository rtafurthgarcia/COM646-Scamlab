import 'package:flutter/material.dart';

class RulesCardWidget extends StatelessWidget {
  const RulesCardWidget({super.key, required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        children: [
          ListTile(
            titleTextStyle: Theme.of(context).textTheme.headlineSmall!.copyWith(
              color: Theme.of(context).colorScheme.primary, // Use primary color
            ),
            visualDensity: VisualDensity.comfortable,
            leading: const Icon(Icons.menu_book),
            title: Text(title),
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
    );
  }
}
