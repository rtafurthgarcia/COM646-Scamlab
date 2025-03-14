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
          //fillColor: Colors.transparent,
          //progressBackgroundColor: Theme.of(context).colorScheme.secondary,
          prgressThickness: 8,
          progressMaskFilter:
              const MaskFilter.blur(BlurStyle.inner, 11.5),
          //progressColors: Theme.of(context).colorScheme.onPrimary,
          // progressShadow: ProgressShadow(
          //     color: Colors.red, opacity: 0.5, blur: 9.8, spreadRadius: 18),
        ),
        builder: (BuildContext context, value, progress) {
          return Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Text(
                value ?? '',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w300,
                ),
              ),
              const Text(
                "s",
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w300,
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}