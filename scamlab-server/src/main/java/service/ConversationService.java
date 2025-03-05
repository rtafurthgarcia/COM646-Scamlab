package service;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import helper.DefaultKeyValues;
import helper.MathHelper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import model.entity.Conversation;
import model.entity.Player;
import model.entity.TestingScenario;
import repository.ConversationRepository;
import repository.ParticipationRepository;
import repository.StateRepository;
import repository.StrategyRepository;

@ApplicationScoped
public class ConversationService {
    @Inject
    ConversationRepository conversationRepository;

    @Inject
    StateRepository stateRepository;

    @Inject
    StrategyRepository strategyRepository;

    @Inject
    Logger logger;

    @Outgoing("player-joined-waiting-lobby")
    public Uni<Player> putPlayerOnWaitingList(Player player) {
        var conversationOnWaiting = conversationRepository.find("state_id", helper.DefaultKeyValues.StateValue.WAITING.value).firstResult();
        
        if (conversationOnWaiting == null) {
            var randomlyPickedStrategy = strategyRepository.findAll().list().get(MathHelper.getRandomNumber(0, (int) strategyRepository.count()-1));
            var randomlyPickedScenario =  TestingScenario.values()[MathHelper.getRandomNumber(0, TestingScenario.values().length -1)];
            
            conversationRepository.persist(
                new Conversation()
                    .setCurrentState(stateRepository.findById(helper.DefaultKeyValues.StateValue.WAITING.value))
                    .setStrategy(randomlyPickedStrategy)
                    .setTestingScenario(randomlyPickedScenario)
            );
        }
        conversationRepository.flush();

        return Uni.createFrom().item(player);
    }

    public Integer getCountOfPlayersWaiting() {
        throw new UnsupportedOperationException();
    }
}
