import 'dart:convert';
import 'package:scamlab/model/ws_message.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

abstract class WSService {
  final String wsUrl;
  String? jwtToken;
  WebSocketChannel? _channel;
  int _sequence = 0;
  late Stream<WsMessage> stream;

  WSService({required this.wsUrl, this.jwtToken});

  bool isListening() {
    return _channel != null && _channel!.protocol != null;
  }

  void connect() {
    if (jwtToken == null) {
      throw Exception("Missing JWT token for WebSocket!");
    }

    // Encode the token in the Quarkus-specific subprotocol format,
    // per https://quarkus.io/guides/websockets-next-reference#bearer-token-authentication
    final quarkusHeaderProtocol = Uri.encodeComponent(
      "quarkus-http-upgrade#Authorization#Bearer $jwtToken"
    );

    // Define the subprotocols list (order matters!)
    final protocols = [
      "bearer-token-carrier", // Your custom subprotocol
      quarkusHeaderProtocol,  // Quarkus header subprotocol
    ];

    _channel = WebSocketChannel.connect(Uri.parse(wsUrl), protocols: protocols);

    stream = _channel!.stream
      .map((data) => json.decode(data))
      .map((json) {
        var message = deserialiseMessage(json: json, sequence: _sequence);
        _sequence++;
        return message;
      });
  }

  /// Disconnects the WebSocket.
  void disconnect() {
    if (isListening()) {
      _channel!.sink.close();
    }
  }

  Future<void> sendMessage(WsMessage message) async {
    if (isListening()) {
      _channel!.sink.add(serialiseMessage(message: message));
    }
  }
}
