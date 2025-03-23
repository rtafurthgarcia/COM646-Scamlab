import 'dart:async';
import 'dart:convert';
import 'package:scamlab/model/ws_message.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:web_socket_channel/status.dart' as status;
import 'dart:developer' as developer;

abstract class WSService {
  String _wsUrl;
  String get wsUrl => _wsUrl;

  bool _isListening = false;
  bool get isListening => _isListening;

  set wsUrl(String newUrl) {
    if (isListening) {
      throw StateError("Cannot update the WebSocket URI Endpoint whilst coopen.");
    }

    _wsUrl = newUrl;
  }

  String? _jwtToken;
  String? get jwtToken => _jwtToken;
  set jwtToken(String? newToken) {
    if (isListening) {
      throw StateError("Cannot update the WebSocket token whilst connection is open.");
    }

    _jwtToken = newToken;
  }

  WebSocketChannel? _channel;
  int _sequence = 0;

  late StreamController _controller;
  Stream get stream => _controller.stream;

  WSService({required String wsUrl}) : _wsUrl = wsUrl;

  void connect() {
    if (_jwtToken == null) {
      throw ArgumentError("Missing JWT token for WebSocket!");
    }

    if (_wsUrl.isEmpty) {
      throw ArgumentError("Missing URI Endpoint for WebSocket");
    }

    // Encode the token in the Quarkus-specific subprotocol format,
    // per https://quarkus.io/guides/websockets-next-reference#bearer-token-authentication
    final quarkusHeaderProtocol = Uri.encodeComponent(
      "quarkus-http-upgrade#Authorization#Bearer $_jwtToken"
    );

    // Define the subprotocols list (order matters!)
    final protocols = [
      "bearer-token-carrier", // Your custom subprotocol
      quarkusHeaderProtocol,  // Quarkus header subprotocol
    ];

    _channel = WebSocketChannel.connect(Uri.parse(_wsUrl), protocols: protocols);
    _channel!.sink.done.whenComplete(() => _isListening = false);
    _isListening = true;

    _controller = StreamController.broadcast();
    _controller.addStream( _channel!.stream
      .map((data) => json.decode(data))
      .map((json) {
        var message = deserialiseMessage(json: json, sequence: _sequence);
        developer.log(
          "Message received of type ${message.type}", 
          sequenceNumber: message.sequence, 
          name: "ws_service", 
          time: DateTime.now());
        _sequence++;
        return message;
      })
    );
    developer.log(
      "Connection opened on $_wsUrl", 
      name: "ws_service", 
        time: DateTime.now());

  }

  /// Disconnects the WebSocket.
  void disconnect() {
    if (isListening) {
      _channel!.sink.close(status.normalClosure);
      _controller.close();
      developer.log(
        "Connection closed on $_wsUrl", 
        name: "ws_service", 
        time: DateTime.now());
    }
  }

  Future<void> sendMessage(WsMessage message) async {
    if (isListening) {
      developer.log(
        "Message sent of type ${message.type}", 
        sequenceNumber: _sequence, 
        name: "ws_service", 
        time: DateTime.now());
      _sequence++;
      _channel!.sink.add(serialiseMessage(message: message));
    }
  }
}
