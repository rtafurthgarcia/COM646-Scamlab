package service;

import org.jboss.logging.Logger;
import exception.PlayerException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import model.entity.Player;
import model.entity.SystemRole;
import repository.PlayerRepository;
import io.smallrye.jwt.build.Jwt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

@ApplicationScoped
public class PlayerService {

    @Inject
    PlayerRepository repository;

    @Inject
    Logger logger;

    public Player registerNewPlayer(String ipAddress) {
        var player = repository.find("ipAddress", ipAddress).firstResult();

        if (player != null && player.getToken() != null) {
            throw new PlayerException("One device cannot play more than once at the same time");
        }

        player = new Player().setIpAddress(ipAddress).setIsBot(false);

        if (ipAddress.equals("127.0.0.1")) {
            player.setSystemRole(SystemRole.ADMIN);
        }

        var token = generateToken(player.getSecondaryId(), ipAddress, player.getSystemRole());
        player.setToken(token);

        repository.persistAndFlush(player);
        return player;
    }

    public void unregisterPlayersToken(Player player) {
        player.setToken(null);
        repository.persistAndFlush(player);
    }

    public Player findUserBySecondaryId(UUID secondaryId) {
        return repository.find("secondaryId", secondaryId).firstResult();   
    }

    private String generateToken(UUID secondaryId, String ipAddress, SystemRole role) {
        var embeddedRole = new HashSet<>(Arrays.asList(SystemRole.USER.name()));
        if (role.equals(SystemRole.ADMIN)) {
            embeddedRole.add(SystemRole.ADMIN.name());
        }

        try {
            String token = Jwt.issuer("RichardTafurthGarcia") // This value must match the server-side mp.jwt.verify.issuer configuration for the token to be considered valid.
                .upn(secondaryId.toString()) // Using the player's secondaryId as the subject -> makes it easier to search within the SecurityContext
                .groups(embeddedRole) 
                .claim("address", ipAddress)
                .audience("using-jwt")
                // Set token expiration in seconds (e.g., 3600 seconds = 60 minutes)
                .expiresIn(3600)
                .sign();

            logger.info("TOKEN generated: " + token);
            return token;
        } catch (Exception e) {
            logger.error("Error generating JWT", e);
            throw new RuntimeException(e);
        }
    }
}