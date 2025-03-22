class GameReconcileState {
  final String conversationSecondaryId;
  final int state;

  GameReconcileState({
    required this.conversationSecondaryId,
    required this.state,
  });

  factory GameReconcileState.fromJson({required Map<String, dynamic> json}) {
    return GameReconcileState(
      conversationSecondaryId: json['conversationSecondaryId'] as String,
      state: json['state'] as int
    );
  }
}