package repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import model.entity.Player;

@ApplicationScoped
public class PlayerRepository implements PanacheRepository<Player> {
    
}
