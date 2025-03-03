class ConversationStartMessage {
  final int numberOfPlayersConnected;

  ConversationStartMessage({required this.numberOfPlayersConnected});

  factory ConversationStartMessage.fromJson(Map<String, dynamic> json) {
    return ConversationStartMessage(
      numberOfPlayersConnected: json['numberOfPlayersConnected'] as int,
    );
  }
}