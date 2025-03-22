// Create a custom triangle
import 'package:flutter/widgets.dart';

class ChatBubbleFeet extends CustomPainter {
  final Color backgroundColor;
  ChatBubbleFeet({ required this.backgroundColor });

  @override
  void paint(Canvas canvas, Size size) {
    var paint = Paint()..color = backgroundColor;

    var path = Path();
    path.lineTo(-5, 0);
    path.lineTo(0, 10);
    path.lineTo(5, 0);
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) {
    return false;
  }
}