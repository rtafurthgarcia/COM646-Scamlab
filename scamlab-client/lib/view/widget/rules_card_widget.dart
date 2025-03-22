import 'package:flutter/material.dart';

class InstructionsCard extends StatelessWidget {
  const InstructionsCard({ 
    super.key, 
    required this.title, 
    required this.icon,
    this.text = "Join the game, chat/vote to unmask the AI bot hidden among players using scripted scenarios. Earn candy by voting correctly, or lose if the bot fools you—rate your confidence post-game. Stay anonymous: new username each round."
  });

  final String title;
  final String text;
  final Icon icon;

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
            leading: icon,
            title: Text(title),
          ),
          Container(
            padding: EdgeInsets.only(left: 16, right: 16, bottom: 16),
            alignment: Alignment.centerLeft,
            child: Text(
              text,
              textAlign: TextAlign.justify,
            ),
          ),
        ],
      ),
    );
  }
}
