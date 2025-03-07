package service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import helper.DefaultKeyValues;
import helper.MathHelper;
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
    Logger logger;

    @Inject
    @ConfigProperty(name = "scamlab.max-lobbies")
    Integer maxOngoingGamesCount;

    @Inject
    @Channel("player-joined-game-out")
    Emitter<Player> emitter;

    public void putPlayerOnWaitingList(Player player) {
        var results = entityManager.createQuery(
            """
                SELECT c.id, s.id, COUNT(p) FROM Conversation c
                JOIN c.participants p
                JOIN c.strategy s
                WHERE c.currentState.id = :state
                GROUP BY c.id, s.id
                HAVING COUNT(p) < 2
                    """, Object[].class)
            .setParameter("state", DefaultKeyValues.StateValue.WAITING.value)
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
        entityManager.flush();

        emitter.send(player);
    }

    @Incoming("player-joined-game-in")
    public void addPlayerToNewGame(Player player) {
        logger.info("Incoming message received for player " + player.getSecondaryId());
    }

    public WaitingLobbyStatisticsMessageDto getWaitingLobbyStatistics() {
        var waitingPlayersCount = entityManager.createQuery(
            """
                SELECT COUNT(p) FROM conversations c
                JOIN c.participations p
                WHERE c.state = :State
                    """, Integer.class)
            .setParameter(0, DefaultKeyValues.StateValue.WAITING.value)
            .getSingleResult();
        var ongoingGamesCount = entityManager.createQuery(
            """
                SELECT COUNT(c) FROM conversations c
                WHERE c.state IN (:State1, :State2, :State3)
                    """, Integer.class)
            .setParameter(0, DefaultKeyValues.StateValue.READY.value)
            .setParameter(1, DefaultKeyValues.StateValue.RUNNING.value)
            .setParameter(2, DefaultKeyValues.StateValue.VOTING.value)
            .getSingleResult();

        return new WaitingLobbyStatisticsMessageDto(WSMessageType.NOTIFY, waitingPlayersCount, ongoingGamesCount, maxOngoingGamesCount);
    }
}
