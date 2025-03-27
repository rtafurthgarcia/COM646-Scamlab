import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/service/ws_service.dart';

class ChatWSService extends WSService {
  ChatWSService({required super.wsUrl});

  void castVote(String playerOnBallotSecondaryId) {
    sendMessage(GameCastVoteMessage(
      sequence: -1,
      playerSecondaryId: playerOnBallotSecondaryId) 
    );
  }
}