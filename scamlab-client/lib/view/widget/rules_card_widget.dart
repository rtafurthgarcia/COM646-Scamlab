import 'package:flutter/material.dart';

class InstructionsCard extends StatelessWidget {
  const InstructionsCard({
    super.key,
    required this.title,
    required this.icon,
    this.text =
        "Join the game, chat/vote to unmask the AI bot hidden among players using scripted scenarios. Earn candy by voting correctly, or lose if the bot fools you—rate your confidence post-game. Stay anonymous: new username each round.",
    this.withoutCard = false,
  });

  final String title;
  final String text;
  final Icon icon;
  final bool withoutCard;

  @override
  Widget build(BuildContext context) {
    return withoutCard
        ? buildColumn(context)
        : Card(child: buildColumn(context));
  }

  Column buildColumn(BuildContext context) {
    return Column(
      children: [
        ListTile(
          titleTextStyle: Theme.of(context).textTheme.headlineSmall!.copyWith(
            color: Theme.of(context).colorScheme.primary, // Use primary color
          ),
          visualDensity: withoutCard ? VisualDensity.compact : VisualDensity.comfortable,
          leading: icon,
          title: Text(title),
        ),
        Container(
          padding: EdgeInsets.only(left: 16, right: 16, bottom: 16),
          alignment: Alignment.centerLeft,
          child: Text(text, textAlign: TextAlign.justify),
        ),
      ],
    );
  }
}
