package service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import model.dto.GameDTO.GameCallToVoteMessageDTO;
import model.dto.GameDTO.GameCancelledMessageDTO;
import model.dto.GameDTO.GameCastVoteMessageDTO;
import model.dto.GameDTO.GameFinishedMessageDTO;
import model.dto.GameDTO.GamePlayersMessageDTO;
import model.dto.GameDTO.LeaveRequestInternalDTO;
import model.dto.GameDTO.VoteAcknowledgedMessageDTO;
import model.dto.GameDTO.GameStartingOrContinuingMessageDTO;
import model.entity.Conversation;
import model.entity.Message;
import model.entity.Participation;
import model.entity.Player;
import model.entity.State;
import model.entity.StrategyByRole;
import model.entity.TransitionReason;
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
    @ConfigProperty(name = "scamlab.timeout-inactivity-in-seconds")
    Long timeOutForInactivity;

    @Inject
    @ConfigProperty(name = "scamlab.number-of-votes")
    Long numberOfVotes;

    @Inject
    @ConfigProperty(name = "scamlab.timeout-lobby-in-seconds")
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
                .createQuery("SELECT c FROM Conversation c WHERE secondaryId = :secondaryId", Conversation.class)
                .setParameter("secondaryId", secondaryId)
                .getSingleResult();
    }

    public Player findPlayerBySecondaryId(UUID secondaryId) {
        return entityManager.createQuery("SELECT p FROM Player p WHERE secondaryId = :secondaryId", Player.class)
                .setParameter("secondaryId", secondaryId)
                .getSingleResult();
    }

    @Incoming(value = "reply-received")
    @RunOnVirtualThread
    public void saveNewReply(GamePlayersMessageDTO message) {
        scheduler.unscheduleJob("P" + message.senderSecondaryId());

        var conversation = findOnGoingConversationByInvolvedPlayer(
                UUID.fromString(message.senderSecondaryId()));

        var participation = conversation.getParticipants()
                .stream()
                .filter(p -> p.getParticipationId().getPlayer().getSecondaryId().toString()
                        .equals(message.senderSecondaryId()))
                .findFirst().orElseThrow();

        entityManager.persist(new Message().setParticipation(participation).setMessage(message.text()));

        if (scheduler.getScheduledJob("B" + conversation.getId().toString()) == null) {
            Integer seconds = random.nextInt(10, 45);
            scheduler.newJob("B" + conversation.getId().toString())
                    .setInterval("PT" + seconds.toString() + "S")
                    .setConcurrentExecution(ConcurrentExecution.SKIP)
                    .setTask(t -> createNewBotReplyTriggered(conversation.getId(), message)).schedule();
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

    @Incoming(value = "start-inactivity-timeout")
    @RunOnVirtualThread
    void startTimeoutTrigger(String playerSecondaryId) {
        scheduler.newJob("P" + playerSecondaryId)
                .setInterval("PT" + timeOutForInactivity.toString() + "S")
                .setDelayed("PT" + timeOutForInactivity.toString() + "S")
                .setConcurrentExecution(ConcurrentExecution.SKIP)
                .setTask(t -> timeoutTriggered(playerSecondaryId)).schedule();
    }

    void timeoutTriggered(String playerSecondaryId) {
        Log.info("Inactivity timeout triggered for player " + playerSecondaryId);
        scheduler.unscheduleJob("P" + playerSecondaryId);

        handlePlayerLeavingGame(
                new LeaveRequestInternalDTO(UUID.fromString(playerSecondaryId), TransitionReason.PlayerInactivity));
    }

    @Transactional
    void createNewBotReplyTriggered(Long conversationId, GamePlayersMessageDTO newMessage) {
        var conversation = entityManager.find(Conversation.class, conversationId);

        // Log.info("Inactivity timeout triggered for player " + playerSecondaryId);
        scheduler.unscheduleJob("B" + conversation.getId().toString());

        Participation botParticipant = conversation.getParticipants().stream()
                .filter(p -> p.getParticipationId().getPlayer().getIsBot()).findFirst().get();
        var role = botParticipant.getParticipationId().getRole();
        var strategyByRole = entityManager.createQuery(
                """
                        SELECT sbr FROM StrategyByRole sbr
                        WHERE sbr.strategyByRoleId.strategy.id = :strategy AND sbr.strategyByRoleId.role.id = :role
                            """, StrategyByRole.class)
                .setParameter("strategy", conversation.getStrategy().getId())
                .setParameter("role", role.getId())
                .getSingleResult();

        var reply = service.generateReply(
                botParticipant.getUserName(),
                strategyByRole.getScript(),
                strategyByRole.getExample(),
                newMessage.senderUsername(),
                newMessage.text(),
                conversationId.intValue());

        if (service.isReplyHarmful(reply)) {
            reply = service.generateAlternativeReply(strategyByRole.getEvasionExample(), conversationId.intValue());
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
                                    botParticipant.getParticipationId().getPlayer().getSecondaryId().toString(),
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
                scheduler.unscheduleJob("V" + conversation.getId().toString());
            }

            conversation.setCurrentState(
                    entityManager.find(State.class, DefaultKeyValues.StateValue.CANCELLED.value),
                    request.reason());

            conversation.getParticipants()
                    .stream()
                    .map(p -> p.getParticipationId().getPlayer())
                    .filter(p -> !p.getIsBot())
                    .forEach(p -> {
                        notifyReasonForAbruptEndOfGame.send(
                                new GameCancelledMessageDTO(p.getSecondaryId().toString(),
                                        request.reason()));
                    });
            entityManager.persist(conversation);

            connectionRegistry.unregister(request.player().toString());
        }
    }

    @Incoming("internal-call-to-vote")
    @RunOnVirtualThread
    void timeToVoteTriggered(Long conversationId) {
        Log.info("Players are called to cast their vote in game " + conversationId.toString());
        scheduler.unscheduleJob("C" + conversationId.toString());

        var conversation = entityManager.find(Conversation.class, conversationId);

        conversation.setCurrentState(entityManager.find(State.class, StateValue.VOTING.value));

        conversation.getParticipants()
                .stream()
                .map(p -> p.getParticipationId().getPlayer())
                .filter(p -> !p.getIsBot())
                .forEach(p -> {
                    callToVoteEmitter.send(
                            new GameCallToVoteMessageDTO(timeoutForVote, p.getSecondaryId().toString()));
                });

        entityManager.persist(conversation);
    }

    @Incoming(value = "register-vote")
    @RunOnVirtualThread
    @Lock(value = Lock.Type.WRITE, time = 1, unit = TimeUnit.SECONDS)
    public void registerVote(GameCastVoteMessageDTO message) {
        var conversation = findConversationBySecondaryId(UUID.fromString(message.conversationSecondaryId()));
        var player = findPlayerBySecondaryId(UUID.fromString(message.voterSecondaryId()));
        var playerOnBallot = findPlayerBySecondaryId(UUID.fromString(message.playerOnBallotSecondaryId()));

        if (!voteRegistry.hasVoted(player.getId())) {
            voteRegistry.register(player.getId(), conversation.getId());
            acknowledgeVoteEmitter.send(new VoteAcknowledgedMessageDTO(player.getSecondaryId().toString()));
        }

        var vote = new Vote();
        vote.setVoteId(
                new VoteId()
                        .setConversation(conversation)
                        .setPlayer(player)
                        .setPlayerVotedAgainst(playerOnBallot));

        boolean everyOneHasVotedToStart = conversation.getParticipants()
                .stream()
                .allMatch(p -> voteRegistry.hasVoted(p.getParticipationId().getPlayer().getId()));

        if (everyOneHasVotedToStart) {
            scheduler.unscheduleJob("C" + conversation.getId().toString());

            if (conversation.getVotes().size() == numberOfVotes * conversation.getParticipants().size()) {
                conversation.setCurrentState(entityManager.find(State.class, StateValue.FINISHED.value));
            } else {
                conversation.setCurrentState(entityManager.find(State.class, StateValue.RUNNING.value));

                scheduler.newJob("C" + conversation.getId().toString())
                    .setInterval("PT" + timeBeforeVote.toString() + "S")
                    .setDelayed("PT" + timeBeforeVote.toString() + "S")
                    .setConcurrentExecution(ConcurrentExecution.SKIP)
                    .setTask(t -> internallyCallToVoteEmitter.send(conversation.getId())).schedule();
            }

            entityManager.persist(vote);
            entityManager.persist(conversation);

            conversation.getParticipants()
                    .stream()
                    .map(p -> p.getParticipationId().getPlayer())
                    .filter(p -> !p.getIsBot())
                    .forEach(p -> {
                        if (conversation.getCurrentState().getId().equals(StateValue.FINISHED.value)) {
                            declareGameAsFinishedEmitter.send(
                                    new GameFinishedMessageDTO(
                                            p.getSecondaryId().toString()));
                        } else {
                            notifyGameAsContinuiningEmitter.send(
                                    new GameStartingOrContinuingMessageDTO(
                                            timeBeforeVote,
                                            p.getSecondaryId().toString()));
                        }

                        voteRegistry.unregister(p.getId());
                    });
        }
    }
}
