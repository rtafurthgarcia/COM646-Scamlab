import 'package:flutter/material.dart';
import 'package:scamlab/view/page/HomePage.dart';

void main() {
  runApp(const MainApp());
}

class MainApp extends StatelessWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      initialRoute: '/',
      routes: <String, WidgetBuilder>{
        '/': (BuildContext context) => const HomePage(),
        //'/signup': (BuildContext context) => const SignUpPage(),
      }
    );
  }
}
