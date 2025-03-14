import 'package:flutter/foundation.dart';
import 'package:state_machine/state_machine.dart';
import 'dart:developer' as developer;

class StateMachineProvider with ChangeNotifier {
  late final StateMachine _gameStateMachine;
  String? _conversationId;
  set conversationId(String newId) {
    _conversationId = newId;
    _gameStateMachine.name = "Game $_conversationId";
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

  State get currentState => _gameStateMachine.current;

  StateMachineProvider({String? conversationId}): _conversationId = conversationId {
    _gameStateMachine = StateMachine("Game ${ conversationId ?? "(non initialised)" }");

    isWaiting = _gameStateMachine.newState("isWaiting");
    isReady = _gameStateMachine.newState("isReady");
    isRunning = _gameStateMachine.newState("isRunning");
    isVoting = _gameStateMachine.newState("isVoting");
    isFinished = _gameStateMachine.newState("isFinished");
    isCancelled = _gameStateMachine.newState("isCancelled");

    conditionsMetForStart = _gameStateMachine.newStateTransition('conditionsMetForStart', [isWaiting], isReady);
    conditionsNotMetAnymore = _gameStateMachine.newStateTransition('conditionsNotMetAnymore', [isReady], isWaiting);
    startTimedOut = _gameStateMachine.newStateTransition('startTimedOut', [isReady], isWaiting);
    playersClickedStart = _gameStateMachine.newStateTransition('playersClickedStart', [isReady], isRunning);
    gameGotInterrupted = _gameStateMachine.newStateTransition('gameGotInterrupted', [isRunning], isCancelled);
    voteCalled = _gameStateMachine.newStateTransition('voteCalled', [isRunning], isVoting);
    voted = _gameStateMachine.newStateTransition('voted', [isVoting], isRunning);
    voteTimedOut = _gameStateMachine.newStateTransition('voteTimedOut', [isVoting], isRunning);
    keepOnPlaying = _gameStateMachine.newStateTransition('keepOnPlaying', [isVoting], isRunning);
    reachedEndGame = _gameStateMachine.newStateTransition('reachedEndGame', [isVoting], isFinished);

    _gameStateMachine.onStateChange.listen((event) {
      developer.log("Game ${_gameStateMachine.name} transitioned from ${event.from} to ${event.to}", name: 'scamlab.gamestate');
      notifyListeners();
    });
  }

  void startWith(State startingState) {
    _gameStateMachine.start(startingState);
    notifyListeners();
  }
}