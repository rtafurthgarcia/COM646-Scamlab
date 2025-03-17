import 'package:scamlab/model/ws_message.dart';
import 'package:scamlab/service/ws_service.dart';

class LobbyWsService extends WSService {
  LobbyWsService({required super.wsUrl});

  void voteToStart(String id) {
    sendMessage(WaitingLobbyVoteToStartMessage(
      sequence: -1,
      conversationSecondaryId: id) 
    );
  }

  void gameStarting() {
    sendMessage(WaitingLobbyGameStartingMessage(sequence: -1));
  }
}