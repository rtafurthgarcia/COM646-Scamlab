import 'package:state_machine/state_machine.dart';

class Game {
  late StateMachine stateMachine;

  String _conversationId;
  set conversationId(String newId) {
    _conversationId = newId;
    stateMachine.name = "Game $_conversationId";
  }
  String get conversationId => _conversationId;

  late State isWaiting;
  late State isReady;
  late State isRunning;
  late State isVoting;
  late State isFinished;
  late State isCancelled;

  late StateTransition conditionsMetForStart;
  late StateTransition conditionsNotMetAnymore;
  late StateTransition startTimedOut;
  late StateTransition playersClickedStart;
  late StateTransition gameGotInterrupted;
  late StateTransition voteCalled;
  late StateTransition voted;
  late StateTransition voteTimedOut;
  late StateTransition keepOnPlaying;
  late StateTransition reachedEndGame;

  Game({String? conversationId}): _conversationId = conversationId ?? "" {
    stateMachine = StateMachine("Game ${ conversationId ?? "(non initialised)" }");
    _prepare();
  }

  void reset() {
    stateMachine = StateMachine("Game $conversationId");
    _prepare();  
  }

  void _prepare() {
    isWaiting = stateMachine.newState("isWaiting");
    isReady = stateMachine.newState("isReady");
    isRunning = stateMachine.newState("isRunning");
    isVoting = stateMachine.newState("isVoting");
    isFinished = stateMachine.newState("isFinished");
    isCancelled = stateMachine.newState("isCancelled");

    conditionsMetForStart = stateMachine.newStateTransition('conditionsMetForStart', [isWaiting], isReady);
    conditionsNotMetAnymore = stateMachine.newStateTransition('conditionsNotMetAnymore', [isReady], isWaiting);
    startTimedOut = stateMachine.newStateTransition('startTimedOut', [isReady], isWaiting);
    playersClickedStart = stateMachine.newStateTransition('playersClickedStart', [isReady], isRunning);
    gameGotInterrupted = stateMachine.newStateTransition('gameGotInterrupted', [isRunning], isCancelled);
    voteCalled = stateMachine.newStateTransition('voteCalled', [isRunning], isVoting);
    voted = stateMachine.newStateTransition('voted', [isVoting], isRunning);
    voteTimedOut = stateMachine.newStateTransition('voteTimedOut', [isVoting], isRunning);
    keepOnPlaying = stateMachine.newStateTransition('keepOnPlaying', [isVoting], isRunning);
    reachedEndGame = stateMachine.newStateTransition('reachedEndGame', [isVoting], isFinished);
  }
}