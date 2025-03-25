import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

class OutChatBubble extends StatelessWidget {
  final String message;
  final DateTime time;
  final bool fromSamePersonAsPreviousOne;

  const OutChatBubble({super.key, required this.message, required this.time, required this.fromSamePersonAsPreviousOne});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(right: 8.0, bottom: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        crossAxisAlignment: CrossAxisAlignment.end,
        mainAxisSize: MainAxisSize.min,
        children: [
          Flexible(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 400),
              child: Container(
                padding: const EdgeInsets.all(15),
                decoration: BoxDecoration(
                  color: Colors.indigo.shade600,
                  borderRadius: BorderRadius.only(
                    topLeft: Radius.circular(19),
                    bottomLeft: Radius.circular(19),
                    bottomRight: Radius.circular(19),
                    topRight: fromSamePersonAsPreviousOne ? Radius.circular(19) : Radius.zero
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      message,
                      style: const TextStyle(color: Colors.white, fontSize: 15),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      DateFormat.Hm().format(time),
                      textAlign: TextAlign.left,
                      style: const TextStyle(color: Colors.white70, fontSize: 10),
                    ),
                  ],
                ),
              ),
            ),
          )
        ],
      ),
    );
  }
}

class InChatBubble extends StatelessWidget {
  final String message;
  final String from;
  final DateTime time;
  final bool fromSamePersonAsPreviousOne;
  const InChatBubble({
    super.key,
    required this.message,
    required this.from,
    required this.time,
    required this.fromSamePersonAsPreviousOne
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 2.0, bottom: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.end,
        mainAxisSize: MainAxisSize.min,
        children: [
          Flexible(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 400),
              child: Container(
                padding: const EdgeInsets.all(15),
                decoration: BoxDecoration(
                  color: Colors.grey.shade300,
                  borderRadius: BorderRadius.only(
                    topRight: Radius.circular(19),
                    bottomLeft: Radius.circular(19),
                    bottomRight: Radius.circular(19),
                    topLeft: fromSamePersonAsPreviousOne ? Radius.circular(19) : Radius.zero
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      from,
                      style: const TextStyle(color: Colors.black87, fontSize: 13, fontWeight: FontWeight.bold),
                    ),
                    Text(
                      textAlign: TextAlign.justify,
                      message,
                      style: const TextStyle(color: Colors.black, fontSize: 15),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      textAlign: TextAlign.right,
                      DateFormat.Hm().format(time),
                      style: const TextStyle(color: Colors.black54, fontSize: 10)
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
