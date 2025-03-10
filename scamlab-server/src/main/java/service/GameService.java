package service;

import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import helper.DefaultKeyValues;
import helper.MathHelper;
import helper.VoteRegistry;
import helper.DefaultKeyValues.RoleValue;
import helper.DefaultKeyValues.StateValue;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduler;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import model.dto.GameDto.WSMessageType;
import model.dto.GameDto.WaitingLobbyAssignedStrategyMessageDto;
import model.dto.GameDto.WaitingLobbyReadyToStartMessageDto;
import model.dto.GameDto.WaitingLobbyStatisticsMessageDto;
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
    @Channel("notify-evolution-out")
    @Broadcast
    Emitter<WaitingLobbyStatisticsMessageDto> notifyEvolutionEmitter;

    @Inject
    @Channel("new-assigned-role-out")
    @Broadcast
    Emitter<WaitingLobbyAssignedStrategyMessageDto> newAssignedRoleEmitter;

    @Inject
    @Channel("game-ready-out")
    @Broadcast
    Emitter<WaitingLobbyReadyToStartMessageDto> gameReadyEmitter;

    @Inject
    @Channel("game-starting-out")
    @Broadcast
    Emitter<Conversation> gameStartingEmitter;

    @Inject
    Scheduler scheduler;

    @Inject
    VoteRegistry registry;

    Lorem lorem = LoremIpsum.getInstance();

    public Player findUserBySecondaryId(UUID secondaryId) {
        return entityManager.createQuery("SELECT p FROM Player p WHERE secondaryId = :secondaryId", Player.class)
            .setParameter("secondaryId", secondaryId)
            .getSingleResult();   
    }

    public Conversation findConversationBySecondaryId(UUID secondaryId) {
        return entityManager.createQuery("SELECT c FROM Conversation p WHERE secondaryId = :secondaryId", Conversation.class)
            .setParameter("secondaryId", secondaryId)
            .getSingleResult();   
    }

    @Incoming(value = "put-players-on-waiting-list-in")
    public void putPlayerOnWaitingList(Player player) {
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
           createNewConversation();
           entityManager.flush();
        }

        notifyEvolutionEmitter.send(getWaitingLobbyStatistics());
        prepareNewGame(player);
    }

    public void createNewConversation() {
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

    public WaitingLobbyStatisticsMessageDto getWaitingLobbyStatistics() {
        var waitingPlayersCount = entityManager.createQuery(
            """
                SELECT COUNT(p) FROM Conversation c
                JOIN c.participants p
                WHERE c.currentState.id = :state
                    """, Long.class)
            .setParameter("state", DefaultKeyValues.StateValue.WAITING.value)
            .getSingleResult();
        var ongoingGamesCount = entityManager.createQuery(
            """
                SELECT COUNT(c) FROM Conversation c
                WHERE c.currentState.id IN (:state1, :state2, :state3)
                    """, Long.class)
            .setParameter("state1", DefaultKeyValues.StateValue.READY.value)
            .setParameter("state2", DefaultKeyValues.StateValue.RUNNING.value)
            .setParameter("state3", DefaultKeyValues.StateValue.VOTING.value)
            .getSingleResult();

        return new WaitingLobbyStatisticsMessageDto(WSMessageType.NOTIFY_WAITING_LOBBY_STATISTICS, waitingPlayersCount, ongoingGamesCount, maxOngoingGamesCount);
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

    private record PrepareNewGameQueryResult(Conversation conversation, Long count) {};

    public void prepareNewGame(Player player) {
        var conversationsWithParticipants = entityManager.createQuery(
            """
                SELECT c, COUNT(p) FROM Conversation c
                JOIN c.participants p
                WHERE c.currentState.id = :state
                GROUP BY c
                    """, PrepareNewGameQueryResult.class)
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

        conversationsWithParticipants.forEach(r -> {
            if (r.conversation.getTestingScenario().numberOfHumans.equals(r.count) 
            && runningOrReadyConversationsCount < maxOngoingGamesCount) {
                r.conversation.setCurrentState(entityManager.find(State.class, StateValue.READY.value));

                Log.info("Preparing new game " + r.conversation.getSecondaryId());
                
                gameReadyEmitter.send(new WaitingLobbyReadyToStartMessageDto(timeOutForWaitingLobby));

                scheduler.newJob(r.conversation.getId().toString())
                    .setDelayed("PT" + timeOutForWaitingLobby.toString() + "S")
                    .setTask(t -> timeoutTriggered(r.conversation)).schedule();
            } else if (r.conversation.getTestingScenario().numberOfHumans < r.count) {
                var participant = new Participation();
                participant.setParticipationId(
                    new ParticipationId()
                        .setConversation(r.conversation)
                        .setPlayer(player)
                        .setRole(this.getNextAppropriateRoleForConversation(r.conversation)))
                    .setUserName(lorem.getName());
                
                r.conversation.getParticipants().add(participant);
                
                entityManager.persist(participant);

                Log.info("Adding player " + player.getSecondaryId().toString() + " to new game");

                newAssignedRoleEmitter.send(getPlayersAssignedStrategy(player, r.conversation));
            }
            entityManager.persist(r.conversation);
            
            notifyEvolutionEmitter.send(getWaitingLobbyStatistics());
        });

        entityManager.flush();
    }

    private void timeoutTriggered(Conversation conversation) {
        conversation.getParticipants().forEach(p -> registry.unregister(p.getParticipationId().getPlayer().getId()));
        conversation.getParticipants().clear();
        conversation.setCurrentState(entityManager.find(State.class, StateValue.WAITING.value));

        entityManager.persist(conversation);
        entityManager.flush();

        notifyEvolutionEmitter.send(getWaitingLobbyStatistics());
    }

    public WaitingLobbyAssignedStrategyMessageDto getPlayersAssignedStrategy(Player player, Conversation conversation) {
        Participation playersParticipation = conversation.getParticipants().stream().filter(p -> p.getParticipationId().getPlayer().equals(player)).findFirst().get();
        var role = playersParticipation.getParticipationId().getRole();
        var strategyByRole = entityManager.createQuery(
            """
                SELECT sbr FROM strategyByRole sbr
                WHERE sbr.strategyByRoleId.strategy.id = :strategy AND sbr.strategyByRoleId.role.id = :role
                    """
            , StrategyByRole.class)
            .setParameter("strategy", conversation.getStrategy().getId())
            .setParameter("role", role.getId())
            .getSingleResult();
        var strategy = conversation.getStrategy(); 

        return new WaitingLobbyAssignedStrategyMessageDto(
            playersParticipation.getParticipationId().getRole().getName(),
            strategyByRole.getScript(),
            strategyByRole.getExample(),
            strategy.getName(),
            playersParticipation.getUserName(),
            conversation.getSecondaryId().toString()
        );
    }

    public void registerStartGame(Conversation conversation, Player player) {
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

            gameStartingEmitter.send(conversation);
        }
    }
}
