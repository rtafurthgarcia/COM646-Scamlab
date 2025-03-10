import 'package:http/http.dart' as http;

class GameService {
  final String baseUrl;
  String? jwtToken;

  GameService({required this.baseUrl, this.jwtToken});

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
      throw Exception(
        'Failed to join game. Status: ${response.statusCode}\n'
        'Response: ${response.body}'
      );
    }
  }
}