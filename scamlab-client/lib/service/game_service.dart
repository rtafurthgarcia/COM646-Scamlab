import 'package:http/http.dart' as http;

class GameService {
  final String baseUrl;

  GameService({required this.baseUrl});

  Future<void> joinNewGame() async {
    final response = await http.get(
      Uri.parse('$baseUrl/games/join'),
      headers: {'Content-Type': 'application/json'},
    );

    if (response.statusCode != 200) {
      throw Exception(
        'Failed to join game. Status: ${response.statusCode}\n'
        'Response: ${response.body}'
      );
    }
  }
}