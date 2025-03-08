package service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import helper.DefaultKeyValues;
import helper.MathHelper;
import helper.VoteRegistry;
import helper.DefaultKeyValues.RoleValue;
import helper.DefaultKeyValues.StateValue;
import io.quarkus.scheduler.Scheduler;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import model.dto.GameDto.WSMessageType;
import model.dto.GameDto.WaitingLobbyStatisticsMessageDto;
import model.entity.Conversation;
import model.entity.Participation;
import model.entity.ParticipationId;
import model.entity.Player;
import model.entity.Role;
import model.entity.State;
import model.entity.Strategy;
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
    @Channel("player-joined-game-out")
    @Broadcast
    Emitter<Player> playerJoinedEmitter;

    @Inject
    @Channel("notify-evolution-out")
    @Broadcast
    Emitter<Conversation> notifyEvolutionEmitter;

    @Inject
    @Channel("game-ready-out")
    @Broadcast
    Emitter<Conversation> gameReadyEmitter;

    @Inject
    Scheduler scheduler;

    @Inject
    VoteRegistry registry;

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
        }
        entityManager.flush();

        playerJoinedEmitter.send(player);
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

    private record PrepareNewGameQueryResult(Conversation conversation, Integer count) {};

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
                
                scheduler.newJob(r.conversation.getId().toString())
                    .setDelayed("PT" + timeOutForWaitingLobby.toString() + "S")
                    .setTask(t -> timeoutTriggered(r.conversation)).schedule();
            } else if (r.conversation.getTestingScenario().numberOfHumans < r.count) {
                var participant = new Participation();
                participant.setParticipationId(
                    new ParticipationId()
                        .setConversation(r.conversation)
                        .setPlayer(player)
                        .setRole(this.getNextAppropriateRoleForConversation(r.conversation)));
                
                        r.conversation.getParticipants().add(participant);
                
                entityManager.persist(participant);
            }
            entityManager.persist(r.conversation);
            
            notifyEvolutionEmitter.send(r.conversation);
        });

        entityManager.flush();
    }

    private void timeoutTriggered(Conversation conversation) {
        conversation.getParticipants().forEach(p -> registry.unregister(p.getParticipationId().getPlayer().getId()));
        conversation.getParticipants().clear();
        conversation.setCurrentState(entityManager.find(State.class, StateValue.WAITING.value));

        entityManager.persist(conversation);
        entityManager.flush();

        notifyEvolutionEmitter.send(conversation);
    }

    private void registerStartGame(Conversation conversation, Player player) {
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

            gameReadyEmitter.send(conversation);
        }
    }
}
