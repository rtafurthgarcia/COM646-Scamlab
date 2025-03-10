package service;

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
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import model.dto.GameDto.CancellationRequestDto;
import model.dto.GameDto.VoteStartRequestDto;
import model.dto.GameDto.WSReasonForWaiting;
import model.dto.GameDto.WaitingLobbyAssignedStrategyMessageDto;
import model.dto.GameDto.WaitingLobbyGameStartingMessageDto;
import model.dto.GameDto.WaitingLobbyReadyToStartMessageDto;
import model.dto.GameDto.WaitingLobbyReasonForWaitingMessageDto;
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
    @Channel("notify-evolution")
    @Broadcast
    Emitter<WaitingLobbyReasonForWaitingMessageDto> notifyEvolutionEmitter;

    @Inject
    @Channel("assign-new-role")
    @Broadcast
    Emitter<WaitingLobbyAssignedStrategyMessageDto> assignNewRoleEmitter;

    @Inject
    @Channel("notify-game-as-ready")
    @Broadcast
    Emitter<WaitingLobbyReadyToStartMessageDto> notifyGameAsReadyEmitter;

    @Inject
    @Channel("notify-game-as-starting")
    @Broadcast
    Emitter<WaitingLobbyGameStartingMessageDto> notifyGameStarting;

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
        return entityManager.createQuery("SELECT c FROM Conversation p WHERE secondaryId = :secondaryId", Conversation.class)
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
                JOIN c.participants p
                WHERE c.currentState.id = :state
                GROUP BY c.id, c.testingScenario
                HAVING (c.testingScenario = :scenario1 AND COUNT(p) < :scenario1HumanCount) OR (c.testingScenario = :scenario2 AND COUNT(p) < :scenario2HumanCount)
                    """, Object[].class)
            .setParameter("state", DefaultKeyValues.StateValue.WAITING.value)
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
        var tooLittlePlayers = ! entityManager.createQuery(
            """
                SELECT c.id, c.testingScenario, COUNT(p) FROM Conversation c
                JOIN c.participants p
                WHERE c.id = :id
                GROUP BY c.id, c.testingScenario
                HAVING (c.testingScenario = :scenario1 AND COUNT(p) < :scenario1HumanCount) OR (c.testingScenario = :scenario2 AND COUNT(p) < :scenario2HumanCount)
                    """, Object[].class)
            .setParameter("id", conversation.getId())
            .setParameter("scenario1", TestingScenario.OneBotTwoHumans)
            .setParameter("scenario2", TestingScenario.ThreeHumans)
            .setParameter("scenario1HumanCount", TestingScenario.OneBotTwoHumans.numberOfHumans)
            .setParameter("scenario2HumanCount", TestingScenario.ThreeHumans.numberOfHumans)
            .getResultList()
            .isEmpty();
        
        if (tooLittlePlayers) {
            notifyEvolutionEmitter.send(
                new WaitingLobbyReasonForWaitingMessageDto(
                    conversation.getSecondaryId().toString(),
                    WSReasonForWaiting.NOT_ENOUGH_PLAYERS
                )
            );
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
            notifyEvolutionEmitter.send(
                new WaitingLobbyReasonForWaitingMessageDto(
                    conversation.getSecondaryId().toString(),
                    WSReasonForWaiting.NOT_ENOUGH_PLAYERS
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
            if (conversation.getTestingScenario().numberOfHumans == conversation.getParticipants().size() 
            && runningOrReadyConversationsCount < maxOngoingGamesCount) {
                conversation.setCurrentState(entityManager.find(State.class, StateValue.READY.value));

                Log.info("Preparing new game " + conversation.getSecondaryId());
                
                notifyGameAsReadyEmitter.send(new WaitingLobbyReadyToStartMessageDto(timeOutForWaitingLobby, player.getSecondaryId().toString()));

                scheduler.newJob(conversation.getId().toString())
                    .setDelayed("PT" + timeOutForWaitingLobby.toString() + "S")
                    .setTask(t -> timeoutTriggered(conversation)).schedule();
            } else if (conversation.getTestingScenario().numberOfHumans > conversation.getParticipants().size()
            && ! playerAssigned) {
                var participant = new Participation();
                participant.setParticipationId(
                    new ParticipationId()
                        .setConversation(conversation)
                        .setPlayer(player)
                        .setRole(this.getNextAppropriateRoleForConversation(conversation)))
                    .setUserName(lorem.getName());
                
                conversation.getParticipants().add(participant);
                
                playerAssigned = true;
                entityManager.persist(participant);

                Log.info("Adding player " + player.getSecondaryId().toString() + " to new game");

                assignNewRoleEmitter.send(getPlayersAssignedStrategy(player, conversation));
            }
            entityManager.persist(conversation);
            
            sendReasonForTheWaitingIfAny(conversation);
        }

        entityManager.flush();
    }

    private void timeoutTriggered(Conversation conversation) {
        conversation.getParticipants().forEach(p -> registry.unregister(p.getParticipationId().getPlayer().getId()));
        conversation.getParticipants().clear();
        conversation.setCurrentState(entityManager.find(State.class, StateValue.WAITING.value));

        entityManager.persist(conversation);
        entityManager.flush();

        notifyEvolutionEmitter.send(
            new WaitingLobbyReasonForWaitingMessageDto(
                conversation.getSecondaryId().toString(), 
                WSReasonForWaiting.START_CANCELLED_TIEMOUT)
            );
    }

    public WaitingLobbyAssignedStrategyMessageDto getPlayersAssignedStrategy(Player player, Conversation conversation) {
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

        return new WaitingLobbyAssignedStrategyMessageDto(
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
    public void registerStartGame(VoteStartRequestDto request) {
        var conversation = findConversationBySecondaryId(request.conversation());
        var player = findPlayerBySecondaryId(request.player());

        if (! registry.hasVoted(player.getId())) {
            registry.register(player.getId(), conversation.getId());
        }

        var everyOneHasVotedToStart = conversation.getParticipants()
            .stream()
            .allMatch(p -> registry.hasVoted(p.getParticipationId().getPlayer().getId()));

        if (everyOneHasVotedToStart) {
            scheduler.unscheduleJob(conversation.getId().toString());

            conversation.setCurrentState(entityManager.find(State.class, StateValue.RUNNING.value));

            entityManager.persist(conversation);
            entityManager.flush();

            notifyGameStarting.send(new WaitingLobbyGameStartingMessageDto(player.getSecondaryId().toString()));
        }
    }

    @Incoming(value = "cancel-if-necessary")
    @RunOnVirtualThread
    public void cancelIfNecessary(CancellationRequestDto request) {
        var anyConversationInvolvedIn = entityManager.createQuery(
            """
                SELECT c FROM Conversation c
                JOIN c.participants p
                WHERE c.currentState.id IN (:state1, :state2)
                AND p.participationId.player.secondaryId = :secondaryId
                    """, Conversation.class)
            .setParameter("state1", DefaultKeyValues.StateValue.RUNNING.value)
            .setParameter("state2", DefaultKeyValues.StateValue.VOTING.value)
            .setParameter("secondaryId", request.player().toString())
            .getResultStream()
            .findFirst();

        if (anyConversationInvolvedIn.isPresent()) {
            var conversation = anyConversationInvolvedIn.get();

            Log.info("Game "
                + conversation.getSecondaryId() 
                + " cancelled by player " 
                + request.player().toString()
                + " for the following reason: " 
                + request.reason().name()
            );

            conversation.setCurrentState(
                entityManager.find(State.class, DefaultKeyValues.StateValue.CANCELLED.value),
                request.reason()
            );

            entityManager.persist(conversation);
        }
    }
}
