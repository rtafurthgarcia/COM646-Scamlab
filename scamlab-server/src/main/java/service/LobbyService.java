package service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import helper.DefaultKeyValues;
import helper.MathHelper;
import helper.PlayerConnectionRegistry;
import helper.VoteRegistry;
import helper.DefaultKeyValues.RoleValue;
import helper.DefaultKeyValues.StateValue;
import io.quarkus.arc.Lock;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import model.dto.GameDto.GameReconcileStateMessageDTO;
import model.dto.GameDto.LeaveRequestInternalDTO;
import model.dto.GameDto.VoteAcknowledgedMessageDTO;
import model.dto.GameDto.VoteStartRequestInternalDTO;
import model.dto.GameDto.WSReasonForWaiting;
import model.dto.GameDto.WaitingLobbyGameAssignmentMessageDTO;
import model.dto.GameDto.GameStartingOrContinuingMessageDTO;
import model.dto.GameDto.WaitingLobbyReadyToStartMessageDTO;
import model.dto.GameDto.WaitingLobbyReasonForWaitingMessageDTO;
import model.entity.Conversation;
import model.entity.Participation;
import model.entity.ParticipationId;
import model.entity.Player;
import model.entity.Role;
import model.entity.State;
import model.entity.Strategy;
import model.entity.StrategyByRole;
import model.entity.TestingScenario;

@ApplicationScoped
@Transactional
public class LobbyService {
    @Inject
    EntityManager entityManager;

    @Inject
    @ConfigProperty(name = "scamlab.max-lobbies")
    Long maxOngoingGamesCount;

    @Inject
    @ConfigProperty(name = "scamlab.timeout-lobby-in-seconds")
    Long timeOutForWaitingLobby;

    @Inject
    @ConfigProperty(name = "scamlab.time-before-vote-in-seconds")
    Long timeBeforeVote;

    @Inject
    @Channel("notify-reason-for-waiting")
    @Broadcast
    Emitter<WaitingLobbyReasonForWaitingMessageDTO> notifyReasonForWaitingEmitter;

    @Inject
    @Channel("return-game-assignment")
    @Broadcast
    Emitter<WaitingLobbyGameAssignmentMessageDTO> returnGameAssignmentEmitter;

    @Inject
    @Channel("notify-game-as-ready")
    @Broadcast
    Emitter<WaitingLobbyReadyToStartMessageDTO> notifyGameAsReadyEmitter;

    @Inject
    @Channel("acknowledge-start-vote")
    @Broadcast
    Emitter<VoteAcknowledgedMessageDTO> acknowledgeVoteEmitter;

    @Inject
    @Channel("notify-game-as-starting")
    @Broadcast
    Emitter<GameStartingOrContinuingMessageDTO> notifyGameStarting;

    @Inject
    @Channel("internal-call-to-vote")
    @Broadcast
    Emitter<Long> internallyCallToVoteEmitter;

    @Inject
    Scheduler scheduler;

    @Inject
    VoteRegistry voteRegistry;

    @Inject
    PlayerConnectionRegistry connectionRegistry;

    Lorem lorem = LoremIpsum.getInstance();

    public Player findPlayerBySecondaryId(UUID secondaryId) {
        return entityManager.createQuery("SELECT p FROM Player p WHERE secondaryId = :secondaryId", Player.class)
                .setParameter("secondaryId", secondaryId)
                .getSingleResult();
    }

    public Conversation findConversationBySecondaryId(UUID secondaryId) {
        return entityManager
                .createQuery("SELECT c FROM Conversation c WHERE secondaryId = :secondaryId", Conversation.class)
                .setParameter("secondaryId", secondaryId)
                .getSingleResult();
    }

    @Incoming(value = "put-players-on-waiting-list")
    @RunOnVirtualThread
    @Lock(value = Lock.Type.WRITE, time = 1, unit = TimeUnit.SECONDS)
    public void putPlayerOnWaitingList(Player player) {
        makeSureGameExists();
        setupGameForPlayer(player);
    }

    public void makeSureGameExists() {
        var results = entityManager.createQuery(
                """
                        SELECT c.id, c.testingScenario, COUNT(p) FROM Conversation c
                        LEFT OUTER JOIN c.participants p
                        WHERE c.currentState.id IN (:state1)
                        GROUP BY c.id, c.testingScenario
                        HAVING (c.testingScenario = :scenario1 AND COUNT(p) < :scenario1HumanCount) OR (c.testingScenario = :scenario2 AND COUNT(p) < :scenario2HumanCount)
                            """,
                Object[].class)
                .setParameter("state1", DefaultKeyValues.StateValue.WAITING.value)
                .setParameter("scenario1", TestingScenario.OneBotTwoHumans)
                .setParameter("scenario2", TestingScenario.ThreeHumans)
                .setParameter("scenario1HumanCount", TestingScenario.OneBotTwoHumans.numberOfHumans)
                .setParameter("scenario2HumanCount", TestingScenario.ThreeHumans.numberOfHumans)
                .getResultList();

        if (results.isEmpty()) {
            var strategies = entityManager.createQuery("SELECT s FROM Strategy s", Strategy.class)
                    .getResultList();

            var randomlyPickedStrategy = strategies.get(MathHelper.getRandomNumber(0, (int) strategies.size()));
            var randomlyPickedScenario = TestingScenario.values()[MathHelper.getRandomNumber(0,
                    TestingScenario.values().length)];

            var newConversation = new Conversation()
                    .setCurrentState(
                            entityManager.find(State.class, helper.DefaultKeyValues.StateValue.WAITING.value))
                    .setStrategy(randomlyPickedStrategy)
                    .setTestingScenario(randomlyPickedScenario)
                    .setStart(LocalTime.now());
            entityManager.persist(newConversation);

            Log.info("Created new game ID: " + newConversation.getSecondaryId());
        }
    }

    public void sendReasonForTheWaitingIfAny(Conversation conversation) {
        List<WSReasonForWaiting> reasonsList = new ArrayList<>();

        var tooLittlePlayers = conversation.getParticipants()
                .size() != conversation.getTestingScenario().numberOfHumans;

        if (tooLittlePlayers) {
            reasonsList.add(WSReasonForWaiting.NOT_ENOUGH_PLAYERS);
        }

        var ongoingGamesCount = entityManager.createQuery(
                """
                        SELECT COUNT(c) FROM Conversation c
                        WHERE c.currentState.id IN (:state1, :state2, :state3)
                            """, Long.class)
                .setParameter("state1", DefaultKeyValues.StateValue.READY.value)
                .setParameter("state2", DefaultKeyValues.StateValue.RUNNING.value)
                .setParameter("state3", DefaultKeyValues.StateValue.VOTING.value)
                .getSingleResult();

        if (ongoingGamesCount.equals(maxOngoingGamesCount)) {
            reasonsList.add(WSReasonForWaiting.ALL_LOBBIES_OCCUPIED);
        }

        // basically only other reason left
        if (reasonsList.isEmpty()) {
            reasonsList.add(WSReasonForWaiting.START_CANCELLED_TIEMOUT);
        }

        for (var player : conversation.getParticipants()) {
            notifyReasonForWaitingEmitter.send(
                    new WaitingLobbyReasonForWaitingMessageDTO(
                            player.getParticipationId().getPlayer().getSecondaryId().toString(),
                            reasonsList));
        }
    }

    public Role getNextAppropriateRoleForConversation(Conversation conversation) {
        if (conversation.getTestingScenario().numberOfHumans.equals(conversation.getParticipants().size())) {
            return null;
        }

        if (conversation.getParticipants().isEmpty()) {
            var id = MathHelper.getRandomNumber(1, RoleValue.values().length);
            return entityManager.find(Role.class, id);
        } else {
            var isScammerRoleAlreadyAttributed = conversation
                    .getParticipants()
                    .stream()
                    .filter(p -> p.getParticipationId()
                            .getRole()
                            .getId()
                            .equals(RoleValue.SCAMMER.value))
                    .findAny()
                    .isPresent();

            if (isScammerRoleAlreadyAttributed) {
                return entityManager.find(Role.class, RoleValue.SCAMBAITER.value);
            } else {
                return entityManager.find(Role.class, RoleValue.SCAMMER.value);
            }
        }
    }

    public void setupGameForPlayer(Player player) {
        var conversationsWithParticipants = entityManager.createQuery(
                """
                        SELECT c FROM Conversation c
                        WHERE c.currentState.id = :state
                            """, Conversation.class)
                .setParameter("state", DefaultKeyValues.StateValue.WAITING.value)
                .getResultList();

        var runningOrReadyConversationsCount = entityManager.createQuery(
                """
                        SELECT COUNT(c) FROM Conversation c
                        WHERE c.currentState.id IN (:state1, :state2)
                            """, Long.class)
                .setParameter("state1", DefaultKeyValues.StateValue.READY.value)
                .setParameter("state2", DefaultKeyValues.StateValue.RUNNING.value)
                .getFirstResult();

        var playerAssigned = false;
        for (Conversation conversation : conversationsWithParticipants) {
            if (conversation.getTestingScenario().numberOfHumans > conversation.getParticipants().size()
                    && !playerAssigned) {
                var participant = new Participation();
                participant.setParticipationId(
                        new ParticipationId()
                                .setConversation(conversation)
                                .setPlayer(player)
                                .setRole(this.getNextAppropriateRoleForConversation(conversation)))
                        .setUserName(lorem.getFirstName());

                conversation.getParticipants().add(participant);

                playerAssigned = true;
                entityManager.persist(participant);

                Log.info("Adding player " + player.getSecondaryId().toString() + " to new game");

                returnGameAssignmentEmitter.send(getPlayersAssignedStrategy(player, conversation));

                sendReasonForTheWaitingIfAny(conversation);
            }

            if (conversation.getTestingScenario().numberOfHumans == conversation.getParticipants().size()
                    && runningOrReadyConversationsCount < maxOngoingGamesCount) {
                conversation.setCurrentState(entityManager.find(State.class, StateValue.READY.value));

                Log.info("Set game " + conversation.getSecondaryId() + " as ready. Will wait for players to start.");

                conversation.getParticipants().forEach(p -> {
                    notifyGameAsReadyEmitter.send(
                            new WaitingLobbyReadyToStartMessageDTO(
                                    timeOutForWaitingLobby,
                                    p.getParticipationId().getPlayer().getSecondaryId().toString()));
                });

                scheduler.newJob("LT" + conversation.getSecondaryId().toString())
                        .setInterval("PT" + timeOutForWaitingLobby.toString() + "S")
                        .setDelayed("PT" + timeOutForWaitingLobby.toString() + "S")
                        .setConcurrentExecution(ConcurrentExecution.SKIP)
                        .setTask(t -> timeoutTriggered(conversation.getId())).schedule();
            }

            entityManager.persist(conversation);
        }

        entityManager.flush();
    }

    void timeoutTriggered(Long conversationId) {
        Log.info("Timeout triggered for start for game " + conversationId.toString());
        var conversation = entityManager.find(Conversation.class, conversationId);
        scheduler.unscheduleJob("LT" + conversation.getSecondaryId().toString());

        /*conversation.getParticipants().forEach(p -> {
            notifyReasonForWaitingEmitter.send(
                    new WaitingLobbyReasonForWaitingMessageDTO(
                            p.getParticipationId().getPlayer().getSecondaryId().toString(),
                            Arrays.asList(WSReasonForWaiting.START_CANCELLED_TIEMOUT)));
        });*/
        var players = conversation.getParticipants().stream()
                .map(p -> p.getParticipationId().getPlayer()).toList();

        conversation.getParticipants().clear();
        conversation.setCurrentState(entityManager.find(State.class, StateValue.WAITING.value));

        entityManager.persist(conversation);
        entityManager.flush();

        players.forEach(p -> {
            voteRegistry.unregister(p.getId());
            putPlayerOnWaitingList(p);
        });
    }

    public WaitingLobbyGameAssignmentMessageDTO getPlayersAssignedStrategy(Player player, Conversation conversation) {
        Participation playersParticipation = conversation.getParticipants().stream()
                .filter(p -> p.getParticipationId().getPlayer().equals(player)).findFirst().get();
        var role = playersParticipation.getParticipationId().getRole();
        var strategyByRole = entityManager.createQuery(
                """
                        SELECT sbr FROM StrategyByRole sbr
                        WHERE sbr.strategyByRoleId.strategy.id = :strategy AND sbr.strategyByRoleId.role.id = :role
                            """, StrategyByRole.class)
                .setParameter("strategy", conversation.getStrategy().getId())
                .setParameter("role", role.getId())
                .getSingleResult();
        var strategy = conversation.getStrategy();

        return new WaitingLobbyGameAssignmentMessageDTO(
                player.getSecondaryId().toString(),
                conversation.getSecondaryId().toString(),
                playersParticipation.getParticipationId().getRole().getName(),
                strategyByRole.getScript(),
                strategyByRole.getExample(),
                strategy.getName(),
                playersParticipation.getUserName(),
                timeBeforeVote);
    }

    @Incoming(value = "register-start-game")
    @RunOnVirtualThread
    @Lock(value = Lock.Type.WRITE, time = 1, unit = TimeUnit.SECONDS)
    public void registerStartGame(VoteStartRequestInternalDTO request) {
        var conversation = findConversationBySecondaryId(request.conversation());
        var player = findPlayerBySecondaryId(request.player());

        if (!voteRegistry.hasVoted(player.getId())) {
            voteRegistry.register(player.getId(), conversation.getId());
            acknowledgeVoteEmitter.send(new VoteAcknowledgedMessageDTO(player.getSecondaryId().toString()));
        }

        var everyOneHasVotedToStart = conversation.getParticipants()
                .stream()
                .allMatch(p -> voteRegistry.hasVoted(p.getParticipationId().getPlayer().getId()));

        if (everyOneHasVotedToStart) {
            scheduler.unscheduleJob("LT" + conversation.getSecondaryId().toString());

            conversation.setCurrentState(entityManager.find(State.class, StateValue.RUNNING.value));

            var newBotPlayer = new Player()
                    .setIsBot(true)
                    .setIpAddress("127.0.0.1");
            entityManager.persist(newBotPlayer);

            var newBotParticipant = new Participation().setParticipationId(
                    new ParticipationId()
                            .setConversation(conversation)
                            .setPlayer(newBotPlayer)
                            .setRole(entityManager.find(Role.class, RoleValue.SCAMBAITER.value)))
                    .setUserName(lorem.getFirstName());
            entityManager.persist(newBotParticipant);

            conversation.getParticipants().add(newBotParticipant);

            entityManager.persist(conversation);
            entityManager.flush();

            conversation.getParticipants()
                    .stream()
                    .map(p -> p.getParticipationId().getPlayer())
                    .filter(p -> !p.getIsBot())
                    .forEach(p -> {
                        notifyGameStarting.send(new GameStartingOrContinuingMessageDTO(
                                timeBeforeVote,
                                p.getSecondaryId().toString(),
                                conversation.getParticipants()
                                        .stream()
                                        .filter(p2 -> !p2.getParticipationId().getPlayer().equals(p))
                                        .collect(Collectors.toMap(
                                                p2 -> p2.getParticipationId().getPlayer().getSecondaryId().toString(),
                                                p2 -> p2.getUserName()))));
                        voteRegistry.unregister(p.getId());
                    });

            scheduler.newJob("V" + conversation.getSecondaryId().toString())
                    .setInterval("PT" + timeBeforeVote.toString() + "S")
                    .setDelayed("PT" + timeBeforeVote.toString() + "S")
                    .setConcurrentExecution(ConcurrentExecution.SKIP)
                    .setTask(t -> internallyCallToVoteEmitter.send(conversation.getId())).schedule();
        }
    }

    @Incoming(value = "handle-player-leaving")
    @RunOnVirtualThread
    @Lock(value = Lock.Type.WRITE, time = 1, unit = TimeUnit.SECONDS)
    public void handlePlayerLeavingLobby(LeaveRequestInternalDTO request) {
        var anyConversationInvolvedIn = entityManager.createQuery(
                """
                        SELECT c FROM Conversation c
                        JOIN c.participants p
                        WHERE c.currentState.id IN (:state1, :state2)
                        AND p.participationId.player.secondaryId = :secondaryId
                            """, Conversation.class)
                .setParameter("state1", DefaultKeyValues.StateValue.WAITING.value)
                .setParameter("state2", DefaultKeyValues.StateValue.READY.value)
                .setParameter("secondaryId", request.player())
                .getResultStream()
                .findFirst();

        if (anyConversationInvolvedIn.isPresent()) {
            var conversation = anyConversationInvolvedIn.get();

            Log.info("Lobby "
                    + conversation.getSecondaryId()
                    + " left by player "
                    + request.player().toString()
                    + " for the following reason: "
                    + request.reason().name());

            // if
            // (conversation.getCurrentState().getId().equals(DefaultKeyValues.StateValue.READY.value))
            // {
            scheduler.unscheduleJob("LT" + conversation.getSecondaryId().toString());
            // }

            conversation.setCurrentState(
                    entityManager.find(State.class, DefaultKeyValues.StateValue.WAITING.value),
                    request.reason());

            conversation.getParticipants()
                    .removeIf(p -> p.getParticipationId().getPlayer().getSecondaryId().equals(request.player()));
            conversation.getParticipants().forEach(p -> {
                notifyReasonForWaitingEmitter.send(
                        new WaitingLobbyReasonForWaitingMessageDTO(
                                p.getParticipationId().getPlayer().getSecondaryId().toString(),
                                Arrays.asList(WSReasonForWaiting.OTHER_PLAYERS_LEFT,
                                        WSReasonForWaiting.NOT_ENOUGH_PLAYERS)));
            });
            var playersLeft = conversation.getParticipants().stream().map(p -> p.getParticipationId().getPlayer())
                    .toList();
            conversation.getParticipants().clear();
            entityManager.persist(conversation);

            playersLeft.forEach(p -> {
                voteRegistry.unregister(p.getId());
                putPlayerOnWaitingList(p);
            });

            connectionRegistry.unregister(request.player().toString());
        }
    }

    public GameReconcileStateMessageDTO reconcileStateForClient(String conversationSecondaryId,
            String playerSecondaryId) {
        var conversation = findConversationBySecondaryId(UUID.fromString(conversationSecondaryId));
        var player = findPlayerBySecondaryId(UUID.fromString(playerSecondaryId));

        // just to make sure its not missing on the client-side
        returnGameAssignmentEmitter.send(getPlayersAssignedStrategy(player, conversation));

        return new GameReconcileStateMessageDTO(conversationSecondaryId, conversation.getCurrentState().getId());
    }
}
