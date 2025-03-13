import 'package:flutter/widgets.dart';

abstract class RetryableProvider with ChangeNotifier {
  bool isLoading = false;

  Exception? exception;

  void clearException() {
    exception = null;
    notifyListeners();
  }

  void tryAgain();
}