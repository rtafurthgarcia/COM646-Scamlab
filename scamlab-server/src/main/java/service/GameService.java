package service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import helper.DefaultKeyValues;
import helper.MathHelper;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import model.dto.GameDto.WSMessageType;
import model.dto.GameDto.WaitingLobbyStatisticsMessageDto;
import model.entity.Conversation;
import model.entity.Player;
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
    @Channel("player-joined-game-out")
    @Broadcast
    Emitter<Player> emitter;

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

        emitter.send(player);
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

        return new WaitingLobbyStatisticsMessageDto(WSMessageType.NOTIFY, waitingPlayersCount, ongoingGamesCount, maxOngoingGamesCount);
    }

    public void prepareNewGame() {
        var results = entityManager.createQuery(
            """
                SELECT c, COUNT(p) FROM Conversation c
                JOIN c.participants p
                WHERE c.currentState.id = :state
                GROUP BY c
                HAVING (c.testingScenario = :scenario1 AND COUNT(p) < :scenario1HumanCount) OR (c.testingScenario = :scenario2 AND COUNT(p) < :scenario2HumanCount)
                    """, Object[].class)
            .setParameter("state", DefaultKeyValues.StateValue.WAITING.value)
            .setParameter("scenario1", TestingScenario.OneBotTwoHumans.name())
            .setParameter("scenario2", TestingScenario.ThreeHumans.name())
            .setParameter("scenario1HumanCount", TestingScenario.OneBotTwoHumans.numberOfHumans)
            .setParameter("scenario2HumanCount", TestingScenario.ThreeHumans.numberOfHumans)
            .getResultList();

        if (results.isEmpty()) {
            createNewConversation();
        } else {
            
        }
    }
}
