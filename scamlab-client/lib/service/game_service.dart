import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:scamlab/model/game.dart';
import 'package:scamlab/model/message.dart';
import 'dart:developer' as developer;


class GameService {
  final String baseUrl;
  String? jwtToken;
  Game game;

  GameService({required this.baseUrl, this.jwtToken, required this.game});

  Future<void> joinNewGame() async {
    if (jwtToken == null) {
      throw Exception("Missing JWT token for WebSocket!");
    }
    
    final response = await http.get(
      Uri.parse('$baseUrl/games/join'),
      headers: {
        'Authorization': 'Bearer $jwtToken',
      },
    );

    if (response.statusCode != 200) {
      developer.log(
        "Error when joining a game: ${response.statusCode}", 
        name: "game_service", 
        time: DateTime.now());
      throw Exception(
        'Failed to join game. Status: ${response.statusCode}\n'
        'Response: ${response.body}'
      );
    }
  }

  Future<void> reconcileStateIfNecessary(String conversationSecondaryId) async {
    if (jwtToken == null) {
      throw Exception("Missing JWT token for WebSocket!");
    }

    developer.log(
      "Reconciliating game $conversationSecondaryId", 
      name: "game_service", 
      time: DateTime.now());
    
    final response = await http.get(
      Uri.parse('$baseUrl/games/$conversationSecondaryId/state'),
      headers: {
        'Authorization': 'Bearer $jwtToken',
      },
    );

    if (response.statusCode != 200) {
      developer.log(
        "Error when reconciliating a game: ${response.statusCode}", 
        name: "game_service", 
        time: DateTime.now());
      throw Exception(
        "Failed to obtain the game's current state. Status: ${response.statusCode}\n"
        'Response: ${response.body}'
      );
    }

    var state = GameReconcileState.fromJson(json: jsonDecode(response.body) as Map<String, dynamic>).state;

    game = Game()..startFrom(game.reconciliateBasedOnConversationStateId(state));
  }
}