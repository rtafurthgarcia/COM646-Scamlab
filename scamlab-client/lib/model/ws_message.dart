enum WsMessageType {
  notifyStartMenuStatistics(value: 1),
  notifyReasonForWaiting(value: 2),
  strategyAssigned(value: 3),
  readyToStart(value: 4),
  voteToStart(value: 5),
  voteAcknowledged(value: 6),
  gameStarting(value: 7),
  gameCancelled(value: 8),
  callToVote(value: 9),
  castVote(value: 10),
  gameFinished(value: 11);

  const WsMessageType({required this.value});

  final int value;
}

class WsMessage {
  final WsMessageType type;
  final DateTime receivedOn;

  WsMessage({required this.type}): receivedOn = DateTime.now();

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is WsMessage && runtimeType == other.runtimeType && type == other.type);

  @override
  int get hashCode => type.hashCode;
}

WsMessageType wsMessageTypeFromInt(int value) {
  return WsMessageType.values.firstWhere(
    (e) => e.value == value,
    orElse: () => throw Exception("Invalid WsMessageType value: $value"),
  );
}

WsMessage mapMessage(Map<String, dynamic> json) {
  final type = wsMessageTypeFromInt(json['type'] as int);

  switch (type) {
    case WsMessageType.notifyStartMenuStatistics:
      return StartMenuStatisticsMessage.fromJson(json);
    case WsMessageType.notifyReasonForWaiting:
      return WaitingLobbyReasonForWaitingMessage.fromJson(json);
    case WsMessageType.strategyAssigned:
      return WaitingLobbyAssignedStrategyMessage.fromJson(json);
    case WsMessageType.readyToStart:
      return WaitingLobbyReadyToStartMessage.fromJson(json);
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
      return GameCancelledMessage.fromJson(json);
  }
}

class StartMenuStatisticsMessage extends WsMessage {
  final int playersConnectedCount;

  StartMenuStatisticsMessage({required this.playersConnectedCount})
      : super(type: WsMessageType.notifyStartMenuStatistics);

  factory StartMenuStatisticsMessage.fromJson(Map<String, dynamic> json) {
    return StartMenuStatisticsMessage(
      playersConnectedCount: json['playersConnectedCount'] as int,
    );
  }
}

class WaitingLobbyReasonForWaitingMessage extends WsMessage {
  final String playerSecondaryId;
  final List<String> reasons;

  WaitingLobbyReasonForWaitingMessage({ required this.playerSecondaryId, required this.reasons })
    : super(type: WsMessageType.notifyReasonForWaiting);

  factory WaitingLobbyReasonForWaitingMessage.fromJson(Map<String, dynamic> json) {
    return WaitingLobbyReasonForWaitingMessage(
      playerSecondaryId: json['playerSecondaryId'] as String,
      reasons: List<String>.from(json['reasons'])
    );
  }
}

class WaitingLobbyAssignedStrategyMessage extends WsMessage {
  final String playerSecondaryId;
  final String conversationSecondaryId;
  final String role;
  final String script; 
  final String example; 
  final String strategy; 
  final String username;

  WaitingLobbyAssignedStrategyMessage({
    required this.playerSecondaryId, 
    required this.conversationSecondaryId, 
    required this.role, 
    required this.script, 
    required this.example, 
    required this.strategy, 
    required this.username })
    : super(type: WsMessageType.strategyAssigned);

  factory WaitingLobbyAssignedStrategyMessage.fromJson(Map<String, dynamic> json) {
    return WaitingLobbyAssignedStrategyMessage(
      playerSecondaryId: json['playerSecondaryId'] as String,
      conversationSecondaryId: json['conversationSecondaryId'] as String,
      role: json['role'] as String,
      script: json['script'] as String,
      example: json['example'] as String,
      strategy: json['strategy'] as String,
      username: json['username'] as String
    );
  }
}

class WaitingLobbyReadyToStartMessage extends WsMessage {
  final int voteTimeout; 
  final String playerSecondaryId; 

  WaitingLobbyReadyToStartMessage({required this.voteTimeout, required this.playerSecondaryId})
    : super(type: WsMessageType.readyToStart);

  factory WaitingLobbyReadyToStartMessage.fromJson(Map<String, dynamic> json) {
    return WaitingLobbyReadyToStartMessage(
      voteTimeout: json['voteTimeout'] as int,
      playerSecondaryId: json['playerSecondaryId'] as String
    );
  }
}


class WaitingLobbyVoteToStartMessage extends WsMessage {
  final String conversationSecondaryId;

  WaitingLobbyVoteToStartMessage({ required this.conversationSecondaryId }) : super(type: WsMessageType.voteToStart);

  factory WaitingLobbyVoteToStartMessage.fromJson(Map<String, dynamic> json) {
    return WaitingLobbyVoteToStartMessage(
      conversationSecondaryId: json['conversationSecondaryId'] as String
    );
  }
}

class WaitingLobbyVoteAcknowledgedMessage extends WsMessage {
  final String playerSecondaryId; 

  WaitingLobbyVoteAcknowledgedMessage({ required this.playerSecondaryId }) : super(type: WsMessageType.voteAcknowledged);

  factory WaitingLobbyVoteAcknowledgedMessage.fromJson(Map<String, dynamic> json) {
    return WaitingLobbyVoteAcknowledgedMessage(
      playerSecondaryId: json['playerSecondaryId'] as String
    );
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
