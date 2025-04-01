package service;

import java.time.LocalTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import helper.DefaultKeyValues;
import helper.DefaultKeyValues.StateValue;
import helper.PlayerConnectionRegistry;
import helper.VoteRegistry;
import io.quarkus.arc.Lock;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduler;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import model.dto.GameDto.GameCallToVoteMessageDTO;
import model.dto.GameDto.GameCancelledMessageDTO;
import model.dto.GameDto.GameCastVoteMessageDTO;
import model.dto.GameDto.GameFinishedMessageDTO;
import model.dto.GameDto.GamePlayersMessageDTO;
import model.dto.GameDto.LeaveRequestInternalDTO;
import model.dto.GameDto.VoteAcknowledgedMessageDTO;
import model.dto.GameDto.GameStartingOrContinuingMessageDTO;
import model.entity.Conversation;
import model.entity.Message;
import model.entity.Participation;
import model.entity.Player;
import model.entity.State;
import model.entity.StrategyByRole;
import model.entity.TestingScenario;
import model.entity.Vote;
import model.entity.VoteId;

@ApplicationScoped
@Transactional
public class GameService {
    @Inject
    EntityManager entityManager;

    @Inject
    @Channel("send-reply")
    @Broadcast
    Emitter<GamePlayersMessageDTO> sendReplyEmitter;

    @Inject
    @Channel("notify-reason-for-abrupt-end-of-game")
    @Broadcast
    Emitter<GameCancelledMessageDTO> notifyReasonForAbruptEndOfGame;

    @Inject
    @Channel("call-to-vote")
    @Broadcast
    Emitter<GameCallToVoteMessageDTO> callToVoteEmitter;

    @Inject
    @Channel("acknowledge-vote")
    @Broadcast
    Emitter<VoteAcknowledgedMessageDTO> acknowledgeVoteEmitter;

    @Inject
    @Channel("notify-game-as-continuing")
    @Broadcast
    Emitter<GameStartingOrContinuingMessageDTO> notifyGameAsContinuiningEmitter;

    @Inject
    @Channel("declare-game-as-finished")
    @Broadcast
    Emitter<GameFinishedMessageDTO> declareGameAsFinishedEmitter;

    @Inject
    @Channel("internal-call-to-vote")
    @Broadcast
    Emitter<Long> internallyCallToVoteEmitter;

    @Inject
    @ConfigProperty(name = "scamlab.number-of-rounds")
    Long numberOfRounds;

    @Inject
    @ConfigProperty(name = "scamlab.timeout-voting-in-seconds")
    Long timeoutForVote;

    @Inject
    @ConfigProperty(name = "scamlab.time-before-vote-in-seconds")
    Long timeBeforeVote;

    @Inject
    Scheduler scheduler;

    @Inject
    VoteRegistry voteRegistry;

    @Inject
    LLMService service;

    /*
     * @Inject
     * VoteToStartRegistry voteRegistry;
     */

    @Inject
    PlayerConnectionRegistry connectionRegistry;

    Random random = new Random();

    public Conversation findOnGoingConversationByInvolvedPlayer(UUID secondaryId) {
        return entityManager.createQuery(
                """
                        SELECT c FROM Conversation c
                        JOIN c.participants p
                        JOIN p.participationId.player pl
                        WHERE c.currentState.id IN (:state1, :state2) AND pl.secondaryId = :secondaryId
                            """,
                Conversation.class)
                .setParameter("secondaryId", secondaryId)
                .setParameter("state1", DefaultKeyValues.StateValue.RUNNING.value)
                .setParameter("state2", DefaultKeyValues.StateValue.VOTING.value)
                .getSingleResult();
    }

    public Conversation findConversationBySecondaryId(UUID secondaryId) {
        return entityManager
                .createQuery("SELECT c FROM Conversation c WHERE secondaryId = :secondaryId",
                        Conversation.class)
                .setParameter("secondaryId", secondaryId)
                .getSingleResult();
    }

    public Player findPlayerBySecondaryId(UUID secondaryId) {
        return entityManager
                .createQuery("SELECT p FROM Player p WHERE secondaryId = :secondaryId", Player.class)
                .setParameter("secondaryId", secondaryId)
                .getSingleResult();
    }

    @Incoming(value = "reply-received")
    @RunOnVirtualThread
    public void saveNewReply(GamePlayersMessageDTO message) {
        // scheduler.unscheduleJob("P" + message.senderSecondaryId());

        var conversation = findOnGoingConversationByInvolvedPlayer(
                UUID.fromString(message.senderSecondaryId()));

        var participation = conversation.getParticipants()
                .stream()
                .filter(p -> p.getParticipationId().getPlayer().getSecondaryId().toString()
                        .equals(message.senderSecondaryId()))
                .findFirst().orElseThrow();

        entityManager.persist(new Message().setParticipation(participation).setMessage(message.text().strip()));

        if (scheduler.getScheduledJob("B" + conversation.getSecondaryId().toString()) == null
                && conversation.getTestingScenario().equals(TestingScenario.OneBotTwoHumans)) {
            Integer seconds = random.nextInt(15, 30);
            scheduler.newJob("B" + conversation.getSecondaryId().toString())
                    .setInterval("PT" + seconds.toString() + "S")
                    .setConcurrentExecution(ConcurrentExecution.SKIP)
                    .setTask(t -> createNewBotReplyTriggered(conversation.getId(), message))
                    .schedule();
        }

        conversation.getParticipants()
                .stream()
                .map(p -> p.getParticipationId()
                        .getPlayer())
                .filter(p -> !p.getIsBot())
                .forEach(p -> {
                    sendReplyEmitter.send(
                            new GamePlayersMessageDTO(
                                    message.senderSecondaryId(),
                                    message.senderUsername(),
                                    p.getSecondaryId().toString(),
                                    message.text(),
                                    message.imagePath()));
                });

    }

    /*
     * @Incoming(value = "start-inactivity-timeout")
     * 
     * @RunOnVirtualThread
     * void startTimeoutTrigger(String playerSecondaryId) {
     * scheduler.newJob("P" + playerSecondaryId)
     * .setInterval("PT" + timeOutForInactivity.toString() + "S")
     * .setConcurrentExecution(ConcurrentExecution.SKIP)
     * .setTask(t -> timeoutTriggered(playerSecondaryId)).schedule();
     * }
     * 
     * void timeoutTriggered(String playerSecondaryId) {
     * Log.info("Inactivity timeout triggered for player " + playerSecondaryId);
     * scheduler.unscheduleJob("P" + playerSecondaryId);
     * 
     * handlePlayerLeavingGame(
     * new LeaveRequestInternalDTO(UUID.fromString(playerSecondaryId),
     * TransitionReason.PlayerInactivity));
     * }
     */

    @Transactional
    void createNewBotReplyTriggered(Long conversationId, GamePlayersMessageDTO newMessage) {
        // Not all replies should elicit a generated response
        if (random.nextInt(1, 4) == 3) {
            return;
        }

        var conversation = entityManager.find(Conversation.class, conversationId);

        Log.info("New LLM reply is being generated after message sent by " + newMessage.senderSecondaryId());
        scheduler.unscheduleJob("B" + conversation.getSecondaryId().toString());

        Participation botParticipant = conversation.getParticipants().stream()
                .filter(p -> p.getParticipationId().getPlayer().getIsBot()).findFirst().get();
        var role = botParticipant.getParticipationId().getRole();
        var strategyByRole = entityManager.createQuery(
                """
                        SELECT sbr FROM StrategyByRole sbr
                        WHERE sbr.strategyByRoleId.strategy.id = :strategy AND sbr.strategyByRoleId.role.id = :role
                            """,
                StrategyByRole.class)
                .setParameter("strategy", conversation.getStrategy().getId())
                .setParameter("role", role.getId())
                .getSingleResult();

        String stringBuilder = "";
        for (Message message : conversation.getMessages().stream().filter(m -> !m.isHasBeenAlreadyAnalysedByBot())
                .toList()) {
            stringBuilder += message.getParticipation().getUserName() + " said \nat " + message.getCreation().toString()
                    + ":\n";
            stringBuilder += message.getMessage() + "\n---\n";
        }

        conversation.getMessages().stream()
                .filter(m -> !m.isHasBeenAlreadyAnalysedByBot())
                .forEach(m -> {
                    m.setHasBeenAlreadyAnalysedByBot(true);
                    entityManager.persist(m);
                });

        var reply = service.generateReply(
                botParticipant.getUserName(),
                strategyByRole.getScript(),
                strategyByRole.getExample(),
                role.getName(),
                stringBuilder,
                conversationId.intValue()).strip();

        if (service.isReplyHarmful(reply)) {
            reply = service.generateAlternativeReply(strategyByRole.getEvasionExample(),
                    conversationId.intValue());
        }

        var newReply = new Message()
                .setParticipation(botParticipant)
                .setMessage(reply);

        entityManager.persist(newReply);

        conversation.getParticipants()
                .stream()
                .map(p -> p.getParticipationId()
                        .getPlayer())
                .filter(p -> !p.getIsBot())
                .forEach(p -> {
                    sendReplyEmitter.send(
                            new GamePlayersMessageDTO(
                                    botParticipant.getParticipationId().getPlayer()
                                            .getSecondaryId().toString(),
                                    botParticipant.getUserName(),
                                    p.getSecondaryId().toString(),
                                    newReply.getMessage(),
                                    ""));
                });
    }

    @Incoming(value = "handle-player-leaving-game")
    @RunOnVirtualThread
    @Lock(value = Lock.Type.WRITE, time = 1, unit = TimeUnit.SECONDS)
    public void handlePlayerLeavingGame(LeaveRequestInternalDTO request) {
        var anyConversationInvolvedIn = entityManager.createQuery(
                """
                        SELECT c FROM Conversation c
                        JOIN c.participants p
                        WHERE c.currentState.id IN (:state1, :state2)
                        AND p.participationId.player.secondaryId = :secondaryId
                            """, Conversation.class)
                .setParameter("state1", DefaultKeyValues.StateValue.RUNNING.value)
                .setParameter("state2", DefaultKeyValues.StateValue.VOTING.value)
                .setParameter("secondaryId", request.player())
                .getResultStream()
                .findFirst();

        if (anyConversationInvolvedIn.isPresent()) {
            var conversation = anyConversationInvolvedIn.get();

            Log.info("Game "
                    + conversation.getSecondaryId()
                    + " left by player "
                    + request.player().toString()
                    + " for the following reason: "
                    + request.reason().name());

            if (conversation.getCurrentState().getId().equals(DefaultKeyValues.StateValue.RUNNING.value)) {
                scheduler.unscheduleJob("V" + conversation.getSecondaryId().toString());
            }

            conversation.setCurrentState(
                    entityManager.find(State.class, DefaultKeyValues.StateValue.CANCELLED.value),
                    request.reason());
            conversation.setEnd(LocalTime.now());

            conversation.getParticipants()
                    .stream()
                    .map(p -> p.getParticipationId().getPlayer())
                    .filter(p -> !p.getIsBot())
                    .forEach(p -> {
                        notifyReasonForAbruptEndOfGame.send(
                                new GameCancelledMessageDTO(
                                        p.getSecondaryId().toString(),
                                        request.reason()));
                    });
            entityManager.persist(conversation);

            connectionRegistry.unregister(request.player().toString());
        }
    }

    @Incoming("internal-call-to-vote")
    @RunOnVirtualThread
    void timeToVoteTriggered(Long conversationId) {
        var conversation = entityManager.find(Conversation.class, conversationId);

        Log.info("Players are called to cast their vote in game " + conversation.getSecondaryId().toString());
        scheduler.unscheduleJob("V" + conversation.getSecondaryId().toString());

        conversation.setCurrentState(entityManager.find(State.class, StateValue.VOTING.value));

        conversation.getParticipants()
                .stream()
                .map(p -> p.getParticipationId().getPlayer())
                .filter(p -> !p.getIsBot())
                .forEach(p -> {
                    callToVoteEmitter.send(
                            new GameCallToVoteMessageDTO(
                                    timeoutForVote,
                                    p.getSecondaryId().toString()));
                });

        entityManager.persist(conversation);

        scheduler.newJob("VT" + conversation.getSecondaryId().toString())
                .setInterval("PT" + timeoutForVote.toString() + "S")
                .setDelayed("PT" + timeoutForVote.toString() + "S")
                .setConcurrentExecution(ConcurrentExecution.SKIP)
                .setTask(t -> voteTimeoutTriggered(conversation.getId()))
                .schedule();
    }

    @Transactional
    public void voteTimeoutTriggered(Long conversationId) {
        var conversation = entityManager.find(Conversation.class, conversationId);

        scheduler.unscheduleJob("VT" + conversation.getSecondaryId().toString());

        verifyOutcomeOfVote(conversation, true);
    }

    @Incoming(value = "register-vote")
    @RunOnVirtualThread
    @Lock(value = Lock.Type.WRITE, time = 1, unit = TimeUnit.SECONDS)
    public void registerVote(GameCastVoteMessageDTO message) {
        var conversation = findConversationBySecondaryId(UUID.fromString(message.conversationSecondaryId()));
        var player = findPlayerBySecondaryId(UUID.fromString(message.voterSecondaryId()));
        // in case the player voted blank
        Player playerOnBallot = null;
        if (!message.playerOnBallotSecondaryId().isBlank()) {
            playerOnBallot = findPlayerBySecondaryId(UUID.fromString(message.playerOnBallotSecondaryId()));
        }

        if (!voteRegistry.hasVoted(player.getId())) {
            voteRegistry.register(player.getId(), conversation.getId());
            acknowledgeVoteEmitter.send(new VoteAcknowledgedMessageDTO(player.getSecondaryId().toString()));
        }

        var round = conversation.getVotes().stream()
                .map(v -> v.getVoteId())
                .filter(v -> v.getPlayer().equals(player) && v.getConversation().equals(conversation))
                .toList().size() + 1;

        var vote = new Vote().setVoteId(
                new VoteId()
                        .setConversation(conversation)
                        .setPlayer(player)
                        .setRoundNo(round));
        vote.setPlayerVotedAgainst(playerOnBallot);

        entityManager.persist(vote);
        conversation.getVotes().add(vote);

        verifyOutcomeOfVote(conversation, false);
    }

    private void verifyOutcomeOfVote(Conversation conversation, Boolean hasTimedout) {
        boolean everyOneHasVotedToStart = conversation.getParticipants()
                .stream()
                .map(p -> p.getParticipationId().getPlayer())
                .filter(p -> !p.getIsBot())
                .allMatch(p -> voteRegistry.hasVoted(p.getId()));

        if (!everyOneHasVotedToStart && hasTimedout) {
            conversation.getParticipants()
                    .stream()
                    .map(p -> p.getParticipationId().getPlayer())
                    .filter(p -> !voteRegistry.hasVoted(p.getId()) && !p.getIsBot())
                    .forEach(p -> {
                        var round = conversation.getVotes().stream()
                                .map(v -> v.getVoteId())
                                .filter(v -> v.getPlayer().equals(p) && v.getConversation().equals(conversation))
                                .toList().size() + 1;

                        var vote = new Vote()
                                .setVoteId(
                                        new VoteId()
                                                .setConversation(conversation)
                                                .setPlayer(p)
                                                .setRoundNo(round))
                                .setPlayerVotedAgainst(null);

                        entityManager.persist(vote);
                        conversation.getVotes().add(vote);
                    });

            everyOneHasVotedToStart = true;
        }

        if (everyOneHasVotedToStart) {
            scheduler.unscheduleJob("VT" + conversation.getSecondaryId().toString());

            Long currentRound = conversation.getVotes().stream()
                    .max((v1, v2) -> v1.compareTo(v2))
                    .map(v -> v.getVoteId().getRoundNo().longValue())
                    .orElse(1l);

            if (currentRound.equals(numberOfRounds)) {
                conversation.setCurrentState(
                        entityManager.find(State.class, StateValue.FINISHED.value));

                conversation.setBotHasBeenUnmasked(conversation.getVotes().stream()
                        .filter(v -> v.getVoteId().getRoundNo() == currentRound.intValue())
                        .filter(v -> v.getPlayerVotedAgainst() != null)
                        .allMatch(v -> v.getPlayerVotedAgainst().getIsBot()));
                conversation.setEnd(LocalTime.now());
            } else {
                conversation.setCurrentState(entityManager.find(State.class, StateValue.RUNNING.value));

                scheduler.newJob("V" + conversation.getSecondaryId().toString())
                        .setInterval("PT" + timeBeforeVote.toString() + "S")
                        .setDelayed("PT" + timeBeforeVote.toString() + "S")
                        .setConcurrentExecution(ConcurrentExecution.SKIP)
                        .setTask(t -> internallyCallToVoteEmitter.send(conversation.getId()))
                        .schedule();
            }

            entityManager.persist(conversation);

            conversation.getParticipants()
                    .stream()
                    .map(p -> p.getParticipationId().getPlayer())
                    .filter(p -> !p.getIsBot())
                    .forEach(p -> {
                        if (conversation.getCurrentState().getId()
                                .equals(StateValue.FINISHED.value)) {
                            declareGameAsFinishedEmitter.send(
                                    new GameFinishedMessageDTO(
                                            p.getSecondaryId().toString()));
                        } else {
                            notifyGameAsContinuiningEmitter.send(
                                    new GameStartingOrContinuingMessageDTO(
                                            timeBeforeVote,
                                            p.getSecondaryId().toString(),
                                            conversation.getParticipants()
                                                    .stream()
                                                    .filter(p2 -> !p2.getParticipationId().getPlayer().equals(p))
                                                    .collect(Collectors.toMap(
                                                            p2 -> p2.getParticipationId().getPlayer().getSecondaryId()
                                                                    .toString(),
                                                            p2 -> p2.getUserName()))));
                        }

                        voteRegistry.unregister(p.getId());
                    });
        }
    }
}
