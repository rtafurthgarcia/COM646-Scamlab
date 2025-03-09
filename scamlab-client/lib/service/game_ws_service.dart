import 'dart:async';
import 'dart:convert';
import 'package:scamlab/model/ws_message.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

class GameWSService {
  final String wsUrl;
  String? jwtToken;
  WebSocketChannel? _channel;
  StreamSubscription<WsMessage>? _subscription;

  GameWSService({required this.wsUrl, this.jwtToken});

  bool isListening() {
    return _channel != null && _channel!.protocol != null;
  }

  /// Starts listening to the WebSocket and calls [onData] for new messages,
  /// [onError] for errors (without canceling the subscription), and [onDone]
  /// when the connection is closed.
  void startListening({
    required void Function(WsMessage message) onData,
    required void Function(Object error, StackTrace stackTrace) onError,
    void Function()? onDone,
  }) {
    if (jwtToken == null) {
      throw Exception("Missing JWT token for WebSocket!");
    }

    // Encode the token in the Quarkus-specific subprotocol format.
    final quarkusHeaderProtocol = Uri.encodeComponent(
      "quarkus-http-upgrade#Authorization#Bearer $jwtToken",
    );

    // Define the subprotocols list (order matters!)
    final protocols = [
      "bearer-token-carrier",
      quarkusHeaderProtocol,
    ];

    _channel = WebSocketChannel.connect(Uri.parse(wsUrl), protocols: protocols);

    // Map incoming data to WsMessage objects.
    final Stream<WsMessage> messageStream = _channel!.stream
        .map<WsMessage>((data) {
          try {
            final Map<String, dynamic> decodedData = json.decode(data);
            return mapMessage(decodedData);
          } catch (e) {
            // Bubble the error up as an error event.
            rethrow;
          }
        })
        .asBroadcastStream();

    // Listen with provided callbacks. cancelOnError: false ensures that errors
    // are delivered via the onError callback but do not cancel the subscription.
    _subscription = messageStream.listen(
      onData,
      onError: onError,
      onDone: onDone,
      cancelOnError: false,
    );
  }

  /// Disconnects the WebSocket.
  void disconnect() {
    _subscription?.cancel();
  
    if (isListening()) {
      _channel?.sink.close();
    }
  }
}
