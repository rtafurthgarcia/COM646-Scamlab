import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:scamlab/model/conversation_start_message.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

class ConversationWSService {
  final String wsUrl;
  String? jwtToken;
  late final WebSocketChannel? _channel;

  ConversationWSService({required this.wsUrl, this.jwtToken});

  bool isListening() {
    return _channel != null && _channel.protocol != null;
  }

  /// Connects to the WebSocket and listens for messages.
  void connect(void Function(ConversationStartMessage) onMessage) {
    if (jwtToken == null) {
      throw Exception("Missing JWT token for WebSocket!");
    }

    _channel = WebSocketChannel.connect(Uri.parse("$wsUrl?authorization=$jwtToken"));

    _channel?.stream.listen((data) {
      // Assume the incoming data is in JSON format.
      final Map<String, dynamic> decodedData = json.decode(data);
      final chatMessage = ConversationStartMessage.fromJson(decodedData);
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
