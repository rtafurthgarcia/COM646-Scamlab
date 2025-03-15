import 'package:state_machine/state_machine.dart';

class GameStateMachine {
  late final StateMachine gameStateMachine;

  String? _conversationId;
  set conversationId(String newId) {
    _conversationId = newId;
    gameStateMachine.name = "Game $_conversationId";
  }

  late final State isWaiting;
  late final State isReady;
  late final State isRunning;
  late final State isVoting;
  late final State isFinished;
  late final State isCancelled;

  late final StateTransition conditionsMetForStart;
  late final StateTransition conditionsNotMetAnymore;
  late final StateTransition startTimedOut;
  late final StateTransition playersClickedStart;
  late final StateTransition gameGotInterrupted;
  late final StateTransition voteCalled;
  late final StateTransition voted;
  late final StateTransition voteTimedOut;
  late final StateTransition keepOnPlaying;
  late final StateTransition reachedEndGame;

  GameStateMachine({String? conversationId}): _conversationId = conversationId {
    gameStateMachine = StateMachine("Game ${ conversationId ?? "(non initialised)" }");

    isWaiting = gameStateMachine.newState("isWaiting");
    isReady = gameStateMachine.newState("isReady");
    isRunning = gameStateMachine.newState("isRunning");
    isVoting = gameStateMachine.newState("isVoting");
    isFinished = gameStateMachine.newState("isFinished");
    isCancelled = gameStateMachine.newState("isCancelled");

    conditionsMetForStart = gameStateMachine.newStateTransition('conditionsMetForStart', [isWaiting], isReady);
    conditionsNotMetAnymore = gameStateMachine.newStateTransition('conditionsNotMetAnymore', [isReady], isWaiting);
    startTimedOut = gameStateMachine.newStateTransition('startTimedOut', [isReady], isWaiting);
    playersClickedStart = gameStateMachine.newStateTransition('playersClickedStart', [isReady], isRunning);
    gameGotInterrupted = gameStateMachine.newStateTransition('gameGotInterrupted', [isRunning], isCancelled);
    voteCalled = gameStateMachine.newStateTransition('voteCalled', [isRunning], isVoting);
    voted = gameStateMachine.newStateTransition('voted', [isVoting], isRunning);
    voteTimedOut = gameStateMachine.newStateTransition('voteTimedOut', [isVoting], isRunning);
    keepOnPlaying = gameStateMachine.newStateTransition('keepOnPlaying', [isVoting], isRunning);
    reachedEndGame = gameStateMachine.newStateTransition('reachedEndGame', [isVoting], isFinished);
  }
}