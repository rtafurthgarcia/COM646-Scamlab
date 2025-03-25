import 'package:scamlab/model/ws_message.dart';
import 'package:state_machine/state_machine.dart';

class Game {
  late StateMachine _stateMachine;
  State? get currentState => _stateMachine.current;
  Stream<StateChange> get onStateChange => _stateMachine.onStateChange;

  WaitingLobbyGameAssignmentMessage? _gameAssignment;
  set gameAssignment(WaitingLobbyGameAssignmentMessage assignment) {
    _gameAssignment = assignment;
    _stateMachine.name = "Game ID: ${_gameAssignment!.conversationSecondaryId}";
  }

  String? get username => _gameAssignment?.username;
  String? get conversationSecondaryId => _gameAssignment?.conversationSecondaryId;
  String? get playerSecondaryId => _gameAssignment?.playerSecondaryId;
  String? get role => _gameAssignment?.role;
  String? get strategy => _gameAssignment?.strategy;
  String? get script => _gameAssignment?.script;
  String? get example => _gameAssignment?.example;
  bool get isGameAssigned => _gameAssignment != null;

  late State isWaiting;
  late State isReady;
  late State isWaitingForStartOfGame;
  late State isRunning;
  late State isVoting;
  late State isWaitingForEndOfVote;
  late State isFinished;
  late State isCancelled;

  late StateTransition conditionsMetForStart;
  late StateTransition conditionsNotMetAnymore;
  late StateTransition startTimedOut;
  late StateTransition playerStarted;
  late StateTransition allPlayersStarted;
  late StateTransition gameGotInterrupted;
  late StateTransition voteCalled;
  late StateTransition playerVoted;
  late StateTransition voteTimedOut;
  late StateTransition keepOnPlaying;
  late StateTransition reachedEndGame;

  Game() {
    _stateMachine = StateMachine("Game (non-initialised)");
    _prepare();
  }

  State reconciliateBasedOnConversationStateId(int statusId) {
    switch (statusId) {
      case 1: return isWaiting;
      case 2: return isReady;
      case 3: return isRunning;
      case 4: return isVoting; 
      case 5: return isFinished;
      case 6: return isCancelled;
      default: throw IllegalStateMachineMutation("Reconciliation failed due to erronous DB state ID");
    }
  }

  void startFrom(State state) => _stateMachine.start(state);

  void _prepare() {
    isWaiting = _stateMachine.newState("isWaiting");
    isReady = _stateMachine.newState("isReady");
    isWaitingForStartOfGame = _stateMachine.newState("isWaitingForStartOfGame");
    isRunning = _stateMachine.newState("isRunning");
    isVoting = _stateMachine.newState("isVoting");
    isWaitingForEndOfVote = _stateMachine.newState("isWaitingForEndOfVote");
    isFinished = _stateMachine.newState("isFinished");
    isCancelled = _stateMachine.newState("isCancelled");

    conditionsMetForStart = _stateMachine.newStateTransition('conditionsMetForStart', [isWaiting], isReady);
    conditionsNotMetAnymore = _stateMachine.newStateTransition('conditionsNotMetAnymore', [isReady, isWaitingForStartOfGame], isWaiting);
    startTimedOut = _stateMachine.newStateTransition('startTimedOut', [isReady, isWaitingForStartOfGame], isWaiting);
    playerStarted = _stateMachine.newStateTransition('playerStarted', [isReady], isWaitingForStartOfGame);
    allPlayersStarted = _stateMachine.newStateTransition('allPlayersStarted', [isWaitingForStartOfGame], isRunning);
    gameGotInterrupted = _stateMachine.newStateTransition('gameGotInterrupted', [isRunning], isCancelled);
    voteCalled = _stateMachine.newStateTransition('voteCalled', [isRunning], isVoting);
    playerVoted = _stateMachine.newStateTransition('playerVoted', [isVoting], isWaitingForEndOfVote);
    voteTimedOut = _stateMachine.newStateTransition('voteTimedOut', [isVoting], isWaitingForEndOfVote);
    keepOnPlaying = _stateMachine.newStateTransition('keepOnPlaying', [isWaitingForEndOfVote], isRunning);
    reachedEndGame = _stateMachine.newStateTransition('reachedEndGame', [isWaitingForEndOfVote], isFinished);

    conditionsMetForStart.cancelIf((stateChange) => !isGameAssigned);
  }
}