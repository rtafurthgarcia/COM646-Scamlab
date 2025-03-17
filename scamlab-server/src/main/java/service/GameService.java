package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import helper.DefaultKeyValues;
import helper.MathHelper;
import helper.VoteToStartRegistry;
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
import model.dto.GameDTO.LeaveRequestDTO;
import model.dto.GameDTO.VoteAcknowledgedMessageDTO;
import model.dto.GameDTO.VoteStartRequestDTO;
import model.dto.GameDTO.WSReasonForWaiting;
import model.dto.GameDTO.WaitingLobbyAssignedStrategyMessageDTO;
import model.dto.GameDTO.WaitingLobbyGameStartingMessageDTO;
import model.dto.GameDTO.WaitingLobbyReadyToStartMessageDTO;
import model.dto.GameDTO.WaitingLobbyReasonForWaitingMessageDTO;
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
public class GameService {
    @Inject
    EntityManager entityManager; 

    @Inject
    @ConfigProperty(name = "scamlab.max-lobbies")
    Long maxOngoingGamesCount;

    @Inject
    @ConfigProperty(name = "scamlab.timeout-lobby-in-seconds")
    Long timeOutForWaitingLobby;

    @Inject
    @Channel("notify-reason-for-waiting")
    @Broadcast
    Emitter<WaitingLobbyReasonForWaitingMessageDTO> notifyReasonForWaitingEmitter;

    @Inject
    @Channel("assign-new-role")
    @Broadcast
    Emitter<WaitingLobbyAssignedStrategyMessageDTO> assignNewRoleEmitter;

    @Inject
    @Channel("notify-game-as-ready")
    @Broadcast
    Emitter<WaitingLobbyReadyToStartMessageDTO> notifyGameAsReadyEmitter;

    @Inject
    @Channel("acknowledge-start-vote")
    @Broadcast
    Emitter<VoteAcknowledgedMessageDTO> acknowledgeStartVoteEmitter;

    @Inject
    @Channel("notify-game-as-starting")
    @Broadcast
    Emitter<WaitingLobbyGameStartingMessageDTO> notifyGameStarting;

    @Inject
    Scheduler scheduler;

    @Inject
    VoteToStartRegistry registry;

    Lorem lorem = LoremIpsum.getInstance();

    public Player findPlayerBySecondaryId(UUID secondaryId) {
        return entityManager.createQuery("SELECT p FROM Player p WHERE secondaryId = :secondaryId", Player.class)
            .setParameter("secondaryId", secondaryId)
            .getSingleResult();   
    }

    public Conversation findConversationBySecondaryId(UUID secondaryId) {
        return entityManager.createQuery("SELECT c FROM Conversation c WHERE secondaryId = :secondaryId", Conversation.class)
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
                    """, Object[].class)
            .setParameter("state1", DefaultKeyValues.StateValue.WAITING.value)
            //.setParameter("state2", DefaultKeyValues.StateValue.READY.value)
            .setParameter("scenario1", TestingScenario.OneBotTwoHumans)
            .setParameter("scenario2", TestingScenario.ThreeHumans)
            .setParameter("scenario1HumanCount", TestingScenario.OneBotTwoHumans.numberOfHumans)
            .setParameter("scenario2HumanCount", TestingScenario.ThreeHumans.numberOfHumans)
            .getResultList();
        
        if (results.isEmpty()) {        
            var strategies = entityManager.createQuery("SELECT s FROM Strategy s", Strategy.class)
                .getResultList(); 
                
            var randomlyPickedStrategy = strategies.get(MathHelper.getRandomNumber(0, (int) strategies.size()-1));
            var randomlyPickedScenario =  TestingScenario.values()[MathHelper.getRandomNumber(0, TestingScenario.values().length -1)];
            
            entityManager.persist(
                new Conversation()
                    .setCurrentState(entityManager.find(State.class, helper.DefaultKeyValues.StateValue.WAITING.value))
                    .setStrategy(randomlyPickedStrategy)
                    .setTestingScenario(randomlyPickedScenario)
            );
        }
    }

    public void sendReasonForTheWaitingIfAny(Conversation conversation) {
        List<WSReasonForWaiting> reasonsList = new ArrayList<>();

        var tooLittlePlayers = conversation.getParticipants().size() != conversation.getTestingScenario().numberOfHumans;

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

        if (ongoingGamesCount == maxOngoingGamesCount) {
            reasonsList.add(WSReasonForWaiting.ALL_LOBBIES_OCCUPIED);
        }

        if (reasonsList.isEmpty()) {
            reasonsList.add(WSReasonForWaiting.SYNCHRONISING);
        }

        for (var player: conversation.getParticipants()) {
            notifyReasonForWaitingEmitter.send(
                new WaitingLobbyReasonForWaitingMessageDTO(
                    player.getParticipationId().getPlayer().getSecondaryId().toString(),
                    reasonsList
                )
            );
        }
    }

    public Role getNextAppropriateRoleForConversation(Conversation conversation) {
        if (conversation.getTestingScenario().numberOfHumans.equals(conversation.getParticipants().size())) {
            return null;
        }

        if (conversation.getParticipants().isEmpty()) {
            var id =  MathHelper.getRandomNumber(1, RoleValue.values().length);
            return entityManager.find(Role.class, id); 
        } else {
            var isScammerRoleAlreadyAttributed = conversation
                .getParticipants()
                .stream()
                .filter(p -> p.getParticipationId()
                    .getRole()
                    .getId()
                    .equals(RoleValue.SCAMMER.value)
                ).findAny()
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
            && ! playerAssigned) {
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

                assignNewRoleEmitter.send(getPlayersAssignedStrategy(player, conversation));

                sendReasonForTheWaitingIfAny(conversation);
            }

            if (conversation.getTestingScenario().numberOfHumans == conversation.getParticipants().size() 
            && runningOrReadyConversationsCount < maxOngoingGamesCount) {
                conversation.setCurrentState(entityManager.find(State.class, StateValue.READY.value));

                Log.info("Preparing new game " + conversation.getSecondaryId());
                
                conversation.getParticipants().forEach(p -> {
                    notifyGameAsReadyEmitter.send(
                        new WaitingLobbyReadyToStartMessageDTO(
                            timeOutForWaitingLobby, 
                            p.getParticipationId().getPlayer().getSecondaryId().toString()
                        )
                    );
                });

                scheduler.newJob(conversation.getId().toString())
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
        conversation.getParticipants().forEach(p -> {
            notifyReasonForWaitingEmitter.send(
                new WaitingLobbyReasonForWaitingMessageDTO
                (
                    p.getParticipationId().getPlayer().getSecondaryId().toString(), 
                    Arrays.asList(WSReasonForWaiting.START_CANCELLED_TIEMOUT)
                )
            );
            //registry.unregister(p.getParticipationId().getPlayer().getId());
        });
        var players = conversation.getParticipants().stream()
            .map(p -> p.getParticipationId().getPlayer()).toList();

        conversation.getParticipants().clear();
        conversation.setCurrentState(entityManager.find(State.class, StateValue.WAITING.value));

        entityManager.persist(conversation);
        entityManager.flush();

        scheduler.unscheduleJob(conversationId.toString());

        // put players back on the queue
        players.forEach(p -> putPlayerOnWaitingList(p));
    }

    public WaitingLobbyAssignedStrategyMessageDTO getPlayersAssignedStrategy(Player player, Conversation conversation) {
        Participation playersParticipation = conversation.getParticipants().stream().filter(p -> p.getParticipationId().getPlayer().equals(player)).findFirst().get();
        var role = playersParticipation.getParticipationId().getRole();
        var strategyByRole = entityManager.createQuery(
            """
                SELECT sbr FROM StrategyByRole sbr
                WHERE sbr.strategyByRoleId.strategy.id = :strategy AND sbr.strategyByRoleId.role.id = :role
                    """
            , StrategyByRole.class)
            .setParameter("strategy", conversation.getStrategy().getId())
            .setParameter("role", role.getId())
            .getSingleResult();
        var strategy = conversation.getStrategy(); 

        return new WaitingLobbyAssignedStrategyMessageDTO(
            player.getSecondaryId().toString(),
            conversation.getSecondaryId().toString(),
            playersParticipation.getParticipationId().getRole().getName(),
            strategyByRole.getScript(),
            strategyByRole.getExample(),
            strategy.getName(),
            playersParticipation.getUserName()
        );
    }

    @Incoming(value = "register-start-game")
    @RunOnVirtualThread
    @Lock(value = Lock.Type.WRITE, time = 1, unit = TimeUnit.SECONDS)  
    public void registerStartGame(VoteStartRequestDTO request) {
        var conversation = findConversationBySecondaryId(request.conversation());
        var player = findPlayerBySecondaryId(request.player());

        if (! registry.hasVoted(player.getId())) {
            registry.register(player.getId(), conversation.getId());
            acknowledgeStartVoteEmitter.send(new VoteAcknowledgedMessageDTO(player.getSecondaryId().toString()));
        }

        var everyOneHasVotedToStart = conversation.getParticipants()
            .stream()
            .allMatch(p -> registry.hasVoted(p.getParticipationId().getPlayer().getId()));

        if (everyOneHasVotedToStart) {
            scheduler.unscheduleJob(conversation.getId().toString());

            conversation.setCurrentState(entityManager.find(State.class, StateValue.RUNNING.value));

            entityManager.persist(conversation);
            entityManager.flush();

            notifyGameStarting.send(new WaitingLobbyGameStartingMessageDTO(player.getSecondaryId().toString()));
        }
    }

    @Incoming(value = "handle-player-leaving")
    @RunOnVirtualThread
    @Lock(value = Lock.Type.WRITE, time = 1, unit = TimeUnit.SECONDS)  
    public void handlePlayerLeavingLobby(LeaveRequestDTO request) {
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

            Log.info("Game "
                + conversation.getSecondaryId() 
                + " left by player " 
                + request.player().toString()
                + " for the following reason: " 
                + request.reason().name()
            );

            if (conversation.getCurrentState().getId().equals(DefaultKeyValues.StateValue.READY.value)) {
                scheduler.unscheduleJob(conversation.getId().toString());
            }

            conversation.setCurrentState(
                entityManager.find(State.class, DefaultKeyValues.StateValue.WAITING.value),
                request.reason()
            );

            conversation.getParticipants().removeIf(p -> p.getParticipationId().getPlayer().getSecondaryId().equals(request.player()));
            conversation.getParticipants().forEach(p -> {
                notifyReasonForWaitingEmitter.send(
                    new WaitingLobbyReasonForWaitingMessageDTO
                    (
                        p.getParticipationId().getPlayer().getSecondaryId().toString(), 
                        Arrays.asList(WSReasonForWaiting.OTHER_PLAYERS_LEFT, WSReasonForWaiting.NOT_ENOUGH_PLAYERS)
                    )
                );
            });
            var playersLeft = conversation.getParticipants().stream().map(p -> p.getParticipationId().getPlayer()).toList();
            conversation.getParticipants().clear();
            entityManager.persist(conversation);

            playersLeft.forEach(p -> putPlayerOnWaitingList(p));
        }
    }
}
