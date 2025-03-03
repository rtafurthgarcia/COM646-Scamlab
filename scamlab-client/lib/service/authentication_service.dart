import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:scamlab/model/player.dart';

class AuthenticationService {
  final String baseUrl;

  AuthenticationService({required this.baseUrl});

  Future<Player> registerNewPlayer() async {
    final response = await http.get(
      Uri.parse('$baseUrl/players/new'),
      headers: {'Content-Type': 'application/json'},
    );

    if (response.statusCode == 201) {
      final Map<String, dynamic> data = json.decode(response.body);
      return Player.fromJson(data);
    } else {
      throw Exception(
        'Failed to register player. Status: ${response.statusCode}\n'
        'Response: ${response.body}'
      );
    }
  }

  Future<void> unregisterPlayer(String secondaryId, String authToken) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/players/$secondaryId'),
      headers: {
        'Authorization': 'Bearer $authToken',
      },
    );

    if (response.statusCode != 205) {
      throw Exception('Failed to unregister player');
    }
  }
}
