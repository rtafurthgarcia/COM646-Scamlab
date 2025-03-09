import 'package:flutter/widgets.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/provider/clearable_provider.dart';
import 'package:scamlab/service/game_ws_service.dart';

class LobbyWSProvider extends ClearableProvider {
bool _dontWaitNextTime = false;
  bool get dontWaitNextTime => _dontWaitNextTime;

  set dontWaitNextTime(bool value) {
    _dontWaitNextTime = value;
    notifyListeners();
  }

  // Connection status.
  bool _isConnected = false;
  bool get isConnected => _isConnected;

  // Instance of GameWSService.
  final GameWSService _gameWsService;

  LobbyWSProvider({
    required GameWSService wsService
  }) : _gameWsService = wsService {
    _startConnection();
  }

  /// Starts the WebSocket connection and sets up the subscription.
  void _startConnection() {
    _gameWsService.startListening(
      onData: (WsMessage message) {
        // Process the incoming WsMessage.
        // Update provider state if needed and notify listeners.
        debugPrint("Received WsMessage: $message");
        // You can add logic here to update your state based on message type.
        notifyListeners();
      },
      onError: (error, StackTrace stackTrace) {
        // Handle errors without canceling the stream.
        debugPrint("WebSocket error in provider: $error");
        notifyListeners();
      },
      onDone: () {
        // Update connection status when the connection is closed.
        debugPrint("WebSocket connection closed");
        _isConnected = false;
        notifyListeners();
      },
    );
    _isConnected = true;
    notifyListeners();
  }

  @override
  void tryAgain() {
    // Reconnect by disconnecting and restarting the connection.
    _gameWsService.disconnect();
    _startConnection();
  }

  @override
  void clearException() {
    _gameWsService.disconnect();
    _isConnected = false;
    super.clearException();
  }
}