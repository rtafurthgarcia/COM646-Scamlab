import 'package:flutter/material.dart';
import 'package:timer_flutter/timer_flutter.dart';

class TimoutTimerWidget extends StatelessWidget {
  final Duration duration;

  const TimoutTimerWidget({super.key, required this.duration});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: FlutterCircularProgressTimer(
        isPieShape: false,
        timerControllerValues: TimerControllerValues(
          listeningDelay: const Duration(milliseconds: 100),
          timeFormate: "SS",
          isCountdown: true,
          duration: duration,
        ),
        radius: 16,
        decoraton: CircularTimerDecoraton(
          prgressThickness: 8,
          progressColors: List.empty(growable: true)..add(Theme.of(context).colorScheme.secondary),
        ),
        builder: (BuildContext context, value, progress) {
          return Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [Text(value ?? ''), const Text("s")],
          );
        },
      ),
    );
  }
}
