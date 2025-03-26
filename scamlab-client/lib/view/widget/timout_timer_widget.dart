import 'package:flutter/material.dart';
import 'package:timer_flutter/timer_flutter.dart';

class TimoutTimer extends StatelessWidget {
  final Duration _duration;

  TimoutTimer({super.key, required Duration duration}) : _duration = duration;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: FlutterCircularProgressTimer(
        isPieShape: false,
        timerControllerValues: TimerControllerValues(
          listeningDelay: const Duration(milliseconds: 100),
          timeFormate: "MM:SS",
          isCountdown: true,
          duration: _duration, 
        ),
        radius: 24,
        decoraton: CircularTimerDecoraton(
          prgressThickness: 8,
          progressColors: List.empty(growable: true)..add(Theme.of(context).colorScheme.secondary),
        ),
        builder: (BuildContext context, value, progress) {
          return Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [Text(value ?? '')],
          );
        },
      ),
    );
  }
}
