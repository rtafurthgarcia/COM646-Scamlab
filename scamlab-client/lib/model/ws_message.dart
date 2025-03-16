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
  final int sequence;

  WsMessage({required this.type, required this.sequence});

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is WsMessage &&
          runtimeType == other.runtimeType &&
          type == other.type &&
          sequence == other.sequence);

  @override
  int get hashCode => type.hashCode ^ sequence.hashCode;

  @override
  String toString() {
    return "$sequence - $type";
  }
}

WsMessageType wsMessageTypeFromInt(int value) {
  return WsMessageType.values.firstWhere(
    (e) => e.value == value,
    orElse: () => throw Exception("Invalid WsMessageType value: $value"),
  );
}

WsMessage mapMessage({required Map<String, dynamic> json, required int sequence}) {
  final type = wsMessageTypeFromInt(json['type'] as int);

  switch (type) {
    case WsMessageType.notifyStartMenuStatistics:
      return StartMenuStatisticsMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.notifyReasonForWaiting:
      return WaitingLobbyReasonForWaitingMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.strategyAssigned:
      return WaitingLobbyAssignedStrategyMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.readyToStart:
      return WaitingLobbyReadyToStartMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.voteToStart:
      return WaitingLobbyVoteToStartMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.voteAcknowledged:
      return WaitingLobbyVoteAcknowledgedMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.gameStarting:
      return WaitingLobbyGameStartingMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.callToVote:
      return GameCallToVoteMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.castVote:
      return GameCastVoteMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.gameFinished:
      return GameFinishedMessage.fromJson(json: json, sequence: sequence);
    case WsMessageType.gameCancelled:
      return GameCancelledMessage.fromJson(json: json, sequence: sequence);
  }
}

class StartMenuStatisticsMessage extends WsMessage {
  final int playersConnectedCount;

  StartMenuStatisticsMessage({
    required this.playersConnectedCount,
    required super.sequence,
  }) : super(type: WsMessageType.notifyStartMenuStatistics);

  factory StartMenuStatisticsMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return StartMenuStatisticsMessage(
      playersConnectedCount: json['playersConnectedCount'] as int,
      sequence: sequence
    );
  }
}

class WaitingLobbyReasonForWaitingMessage extends WsMessage {
  final String playerSecondaryId;
  final List<String> reasons;

  WaitingLobbyReasonForWaitingMessage({
    required this.playerSecondaryId,
    required this.reasons,
    required super.sequence,
  }) : super(type: WsMessageType.notifyReasonForWaiting);

  factory WaitingLobbyReasonForWaitingMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return WaitingLobbyReasonForWaitingMessage(
      playerSecondaryId: json['playerSecondaryId'] as String,
      reasons: List<String>.from(json['reasons']),
      sequence: sequence
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
    required this.username,
    required super.sequence,
  }) : super(type: WsMessageType.strategyAssigned);

  factory WaitingLobbyAssignedStrategyMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return WaitingLobbyAssignedStrategyMessage(
      playerSecondaryId: json['playerSecondaryId'] as String,
      conversationSecondaryId: json['conversationSecondaryId'] as String,
      role: json['role'] as String,
      script: json['script'] as String,
      example: json['example'] as String,
      strategy: json['strategy'] as String,
      username: json['username'] as String,
      sequence: sequence
    );
  }
}

class WaitingLobbyReadyToStartMessage extends WsMessage {
  final int voteTimeout;
  final String playerSecondaryId;

  WaitingLobbyReadyToStartMessage({
    required this.voteTimeout,
    required this.playerSecondaryId,
    required super.sequence,
  }) : super(type: WsMessageType.readyToStart);

  factory WaitingLobbyReadyToStartMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return WaitingLobbyReadyToStartMessage(
      voteTimeout: json['voteTimeout'] as int,
      playerSecondaryId: json['playerSecondaryId'] as String,
      sequence: sequence
    );
  }
}

class WaitingLobbyVoteToStartMessage extends WsMessage {
  final String conversationSecondaryId;

  WaitingLobbyVoteToStartMessage({
    required this.conversationSecondaryId,
    required super.sequence,
  }) : super(type: WsMessageType.voteToStart);

  factory WaitingLobbyVoteToStartMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return WaitingLobbyVoteToStartMessage(
      conversationSecondaryId: json['conversationSecondaryId'] as String,
      sequence: sequence
    );
  }
}

class WaitingLobbyVoteAcknowledgedMessage extends WsMessage {
  final String playerSecondaryId;

  WaitingLobbyVoteAcknowledgedMessage({
    required this.playerSecondaryId,
    required super.sequence,
  }) : super(type: WsMessageType.voteAcknowledged);

  factory WaitingLobbyVoteAcknowledgedMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return WaitingLobbyVoteAcknowledgedMessage(
      playerSecondaryId: json['playerSecondaryId'] as String,
      sequence: sequence
    );
  }
}

class WaitingLobbyGameStartingMessage extends WsMessage {
  WaitingLobbyGameStartingMessage({required super.sequence})
      : super(type: WsMessageType.gameStarting);

  factory WaitingLobbyGameStartingMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return WaitingLobbyGameStartingMessage(
      sequence: sequence
    );
  }
}

class GameCancelledMessage extends WsMessage {
  GameCancelledMessage({required super.sequence})
      : super(type: WsMessageType.gameCancelled);

  factory GameCancelledMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return GameCancelledMessage(
      sequence: sequence
    );
  }
}

class GameCallToVoteMessage extends WsMessage {
  final int voteTimeout;

  GameCallToVoteMessage({
    required this.voteTimeout,
    required super.sequence,
  }) : super(type: WsMessageType.callToVote);

  factory GameCallToVoteMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return GameCallToVoteMessage(
      voteTimeout: json['voteTimeout'] as int,
      sequence: sequence
    );
  }
}

class GameCastVoteMessage extends WsMessage {
  final String playerSecondaryId;

  GameCastVoteMessage({
    required this.playerSecondaryId,
    required super.sequence,
  }) : super(type: WsMessageType.castVote);

  factory GameCastVoteMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return GameCastVoteMessage(
      playerSecondaryId: json['playerSecondaryId'] as String,
      sequence: sequence
    );
  }
}

class GameFinishedMessage extends WsMessage {
  GameFinishedMessage({required super.sequence})
      : super(type: WsMessageType.gameFinished);

  factory GameFinishedMessage.fromJson({required Map<String, dynamic> json, required int sequence}) {
    return GameFinishedMessage(
      sequence: sequence
    );
  }
}
