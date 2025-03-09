enum WsMessageType {
  notifyStartMenuStatistics(value: 1),
  notifyWaitingLobbyStatistics(value: 2),
  voteToStart(value: 3),
  voteAcknowledged(value: 4),
  gameStarting(value: 5),
  gameCancelled(value: 6),
  callToVote(value: 7),
  castVote(value: 8),
  gameFinished(value: 9);

  const WsMessageType({required this.value});

  final int value;
}

class WsMessage {
  final WsMessageType type;

  WsMessage({required this.type});

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is WsMessage && runtimeType == other.runtimeType && type == other.type);

  @override
  int get hashCode => type.hashCode;
}

WsMessage mapMessage(Map<String, dynamic> json) {
  switch (json['type'] as WsMessageType) {
    case WsMessageType.notifyStartMenuStatistics:
      return StartMenuStatisticsMessage.fromJson(json);
    case WsMessageType.notifyWaitingLobbyStatistics:
      return WaitingLobbyStatisticsMessage.fromJson(json);
    case WsMessageType.voteToStart:
      return WaitingLobbyVoteToStartMessage.fromJson(json);
    case WsMessageType.voteAcknowledged:
      return WaitingLobbyVoteAcknowledgedMessage.fromJson(json);
    case WsMessageType.gameStarting:
      return WaitingLobbyGameStartingMessage.fromJson(json);
    case WsMessageType.callToVote:
      return GameCallToVoteMessage.fromJson(json);
    case WsMessageType.castVote:
      return GameCastVoteMessage.fromJson(json);
    case WsMessageType.gameFinished:
      return GameFinishedMessage.fromJson(json);
    case WsMessageType.gameCancelled:
      return GameFinishedMessage.fromJson(json);
  }
}

class StartMenuStatisticsMessage extends WsMessage {
  final int numberOfPlayersConnected;

  StartMenuStatisticsMessage({required this.numberOfPlayersConnected})
      : super(type: WsMessageType.notifyStartMenuStatistics);

  factory StartMenuStatisticsMessage.fromJson(Map<String, dynamic> json) {
    return StartMenuStatisticsMessage(
      numberOfPlayersConnected: json['numberOfPlayersConnected'] as int,
    );
  }
}

class WaitingLobbyStatisticsMessage extends WsMessage {
  final int waitingPlayerCount; 
  final int ongoingGamesCount; 
  final int maxOngoingGamesCount;

  WaitingLobbyStatisticsMessage({required this.waitingPlayerCount, required this.ongoingGamesCount, required this.maxOngoingGamesCount})
    : super(type: WsMessageType.notifyWaitingLobbyStatistics);

  factory WaitingLobbyStatisticsMessage.fromJson(Map<String, dynamic> json) {
    return WaitingLobbyStatisticsMessage(
      waitingPlayerCount: json['waitingPlayerCount'] as int,
      ongoingGamesCount: json['ongoingGamesCount'] as int,
      maxOngoingGamesCount: json['maxOngoingGamesCount'] as int,
    );
  }
}

class WaitingLobbyVoteToStartMessage extends WsMessage {
  WaitingLobbyVoteToStartMessage() : super(type: WsMessageType.voteToStart);

  factory WaitingLobbyVoteToStartMessage.fromJson(Map<String, dynamic> json) {
    return WaitingLobbyVoteToStartMessage();
  }
}

class WaitingLobbyVoteAcknowledgedMessage extends WsMessage {
  WaitingLobbyVoteAcknowledgedMessage() : super(type: WsMessageType.voteAcknowledged);

  factory WaitingLobbyVoteAcknowledgedMessage.fromJson(Map<String, dynamic> json) {
    return WaitingLobbyVoteAcknowledgedMessage();
  }
}

class WaitingLobbyGameStartingMessage extends WsMessage {
  WaitingLobbyGameStartingMessage() : super(type: WsMessageType.gameStarting);

  factory WaitingLobbyGameStartingMessage.fromJson(Map<String, dynamic> json) {
    return WaitingLobbyGameStartingMessage();
  }
}

class GameCancelledMessage extends WsMessage {
  GameCancelledMessage() : super(type: WsMessageType.gameCancelled);

  factory GameCancelledMessage.fromJson(Map<String, dynamic> json) {
    return GameCancelledMessage();
  }
}

class GameCallToVoteMessage extends WsMessage {
  final int voteTimeout;

  GameCallToVoteMessage({required this.voteTimeout})
      : super(type: WsMessageType.callToVote);

  factory GameCallToVoteMessage.fromJson(Map<String, dynamic> json) {
    return GameCallToVoteMessage(
      voteTimeout: json['voteTimeout'] as int,
    );
  }
}

class GameCastVoteMessage extends WsMessage {
  final String playerSecondaryId;

  GameCastVoteMessage({required this.playerSecondaryId})
      : super(type: WsMessageType.castVote);

  factory GameCastVoteMessage.fromJson(Map<String, dynamic> json) {
    return GameCastVoteMessage(
      playerSecondaryId: json['playerSecondaryId'] as String,
    );
  }
}

class GameFinishedMessage extends WsMessage {
  GameFinishedMessage() : super(type: WsMessageType.gameFinished);

  factory GameFinishedMessage.fromJson(Map<String, dynamic> json) {
    return GameFinishedMessage();
  }
}
