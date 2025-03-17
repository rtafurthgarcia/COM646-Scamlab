import 'package:state_machine/state_machine.dart';

class Game {
  late StateMachine stateMachine;

  String _conversationId;
  set conversationId(String newId) {
    _conversationId = newId;
    stateMachine.name = "Game $_conversationId";
  }
  String get conversationId => _conversationId;

  String username = "";
  String playerId = "";

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

  Game({String? conversationId}): _conversationId = conversationId ?? "" {
    stateMachine = StateMachine("Game ${ conversationId ?? "(non initialised)" }");
    _prepare();
  }

  void reset() {
    stateMachine = StateMachine("Game $conversationId");
    username = "";
    playerId = "";
    _prepare();  
  }

  void _prepare() {
    isWaiting = stateMachine.newState("isWaiting");
    isReady = stateMachine.newState("isReady");
    isWaitingForStartOfGame = stateMachine.newState("isWaitingForStartOfGame");
    isRunning = stateMachine.newState("isRunning");
    isVoting = stateMachine.newState("isVoting");
    isWaitingForEndOfVote = stateMachine.newState("isWaitingForEndOfVote");
    isFinished = stateMachine.newState("isFinished");
    isCancelled = stateMachine.newState("isCancelled");

    conditionsMetForStart = stateMachine.newStateTransition('conditionsMetForStart', [isWaiting], isReady);
    conditionsNotMetAnymore = stateMachine.newStateTransition('conditionsNotMetAnymore', [isReady, isWaitingForStartOfGame], isWaiting);
    startTimedOut = stateMachine.newStateTransition('startTimedOut', [isReady, isWaitingForStartOfGame], isWaiting);
    playerStarted = stateMachine.newStateTransition('playerStarted', [isReady], isWaitingForStartOfGame);
    allPlayersStarted = stateMachine.newStateTransition('allPlayersStarted', [isWaitingForStartOfGame], isRunning);
    gameGotInterrupted = stateMachine.newStateTransition('gameGotInterrupted', [isRunning], isCancelled);
    voteCalled = stateMachine.newStateTransition('voteCalled', [isRunning], isVoting);
    playerVoted = stateMachine.newStateTransition('playerVoted', [isVoting], isWaitingForEndOfVote);
    voteTimedOut = stateMachine.newStateTransition('voteTimedOut', [isVoting], isWaitingForEndOfVote);
    keepOnPlaying = stateMachine.newStateTransition('keepOnPlaying', [isWaitingForEndOfVote], isRunning);
    reachedEndGame = stateMachine.newStateTransition('reachedEndGame', [isWaitingForEndOfVote], isFinished);
  }
}