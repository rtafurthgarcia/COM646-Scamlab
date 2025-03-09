import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:scamlab/model/ws_message.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

class StartMenuWSService {
  final String wsUrl;
  String? jwtToken;
  WebSocketChannel? _channel;

  StartMenuWSService({required this.wsUrl, this.jwtToken});

  bool isListening() {
    return _channel != null && _channel!.protocol != null;
  }

  void connect(void Function(StartMenuStatisticsMessage) onMessage) {
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
      final chatMessage = StartMenuStatisticsMessage.fromJson(decodedData);
      onMessage(chatMessage);
    }, onError: (error) {
      debugPrint('WebSocket error: $error');
    }, onDone: () {
      debugPrint('WebSocket connection closed');
    });
  }

  /// Disconnects the WebSocket.
  void disconnect() {
    if (isListening()) {
      _channel?.sink.close();
    }
  }
}
