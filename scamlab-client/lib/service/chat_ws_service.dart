import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/service/ws_service.dart';

class ChatWSService extends WSService {
  ChatWSService({required super.wsUrl});

  void castVote(
    String conversationSecondaryId,
    String voterSecondaryId,
    String playerOnBallotSecondaryId,
  ) {
    sendMessage(
      GameCastVoteMessage(
        sequence: -1,
        voterSecondaryId: voterSecondaryId,
        playerOnBallotSecondaryId: playerOnBallotSecondaryId,
        conversationSecondaryId: conversationSecondaryId,
      ),
    );
  }
}
