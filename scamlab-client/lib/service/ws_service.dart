import 'dart:convert';
import 'dart:io';
import 'package:scamlab/model/ws_message.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'dart:developer' as developer;

abstract class WSService {
  final String wsUrl;
  String? jwtToken;
  WebSocketChannel? _channel;
  int _sequence = 0;

  WSService({required this.wsUrl, this.jwtToken});

  bool isListening() {
    return _channel != null && _channel!.protocol != null;
  }

  void connect(void Function(WsMessage) onMessage, void Function(dynamic) onError) {
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

    _channel?.stream.listen((data) {
      // Assume the incoming data is in JSON format.
      final Map<String, dynamic> decodedData = json.decode(data);
      final chatMessage = mapMessage(json: decodedData, sequence: _sequence);
      _sequence++;

      onMessage(chatMessage);
      developer.log(chatMessage.toString(), name: 'scamlab.wsservice');
    }, onError: (error) {
      onError(error);
      developer.log(error.toString(), name: 'scamlab.wsservice');
    }, onDone: () {
      // per WebSocket https://datatracker.ietf.org/doc/html/rfc6455#section-7.1.5
      if (_channel?.closeCode != null && _channel?.closeCode != 1000 ) {
        onError(WebSocketException("Connection not closed properly", _channel?.closeCode));
      }
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
      _channel!.sink.add(message);
    }
  }
  
}
