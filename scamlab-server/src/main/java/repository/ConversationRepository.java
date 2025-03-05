package repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.Conversation;

@ApplicationScoped
public class ConversationRepository implements PanacheRepository<Conversation> {
    
}
