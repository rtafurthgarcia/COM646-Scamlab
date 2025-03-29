import 'dart:math';
import 'dart:ui';

import 'package:flutter/material.dart';

Color getColorFromUsername(String username) {
  // Create a seed by combining the char codes.
  final seed = username.codeUnits.fold(0, (prev, curr) => prev + curr);
  final random = Random(seed);

  return Color.fromARGB(
    255,
    random.nextInt(256),
    random.nextInt(256),
    random.nextInt(256),
  );
}

Color getContrastingColor(Color color) {
  // Compute luminance returns a value between 0.0 and 1.0
  final double luminance = color.computeLuminance();
  // Return black if the color is light, white if dark.
  return luminance > 0.5 ? Colors.black : Colors.white;
}
