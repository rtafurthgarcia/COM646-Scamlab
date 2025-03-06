package service;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import helper.DefaultKeyValues;
import helper.MathHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import model.entity.Conversation;
import model.entity.Player;
import model.entity.State;
import model.entity.Strategy;
import model.entity.TestingScenario;

@ApplicationScoped
@Transactional
public class ConversationService {
    @Inject
    EntityManager entityManager; 

    @Inject
    Logger logger;

    @Inject
    @Channel("player-joined-waiting-lobby")
    Emitter<Player> emitter;

    public void putPlayerOnWaitingList(Player player) {
        var conversationOnWaiting = entityManager.createQuery(
            """
                SELECT c.id, COUNT(p) FROM conversations c
                JOIN c.participations p
                WHERE c.state = :State
                GROUP BY c.id
                    """, Conversation.class)
                    .setParameter(0, DefaultKeyValues.StateValue.WAITING.value)
                    .getSingleResult();
        
        if (conversationOnWaiting == null) {
            var strategies = entityManager.createQuery("SELECT s FROM strategies", Strategy.class)
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

    public Integer getCountOfPlayersWaiting() {
        throw new UnsupportedOperationException();
    }
}
