package service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import exception.PlayerException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import model.entity.Player;
import model.entity.SystemRole;
import io.smallrye.jwt.build.Jwt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class AuthenticationService {
    @Inject
    EntityManager entityManager;

    static final String LOCALHOST = "127.0.0.1";
    static final Integer TOKEN_EXPIRATION_SECONDS = 3600;

    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String Issuer;

    @Inject
    Logger logger;

    public Player registerNewPlayer(String ipAddress) {
        var isPlayerAlreadyAssignedToken = ! entityManager.createQuery(
            "SELECT p FROM Player p WHERE p.ipAddress = :ipAddress AND p.token IS NOT NULL", 
            Player.class)
            .setParameter("ipAddress", ipAddress).getResultList().isEmpty();

        if (isPlayerAlreadyAssignedToken) {
            throw new PlayerException("One device cannot play more than once at the same time");
        }

        var player = new Player().setIpAddress(ipAddress).setIsBot(false);

        if (ipAddress.equals(LOCALHOST)) {
            player.setSystemRole(SystemRole.ADMIN);
        }

        var token = generateToken(player.getSecondaryId(), ipAddress, player.getSystemRole());
        player.setToken(token);

        entityManager.persist(player);
        entityManager.flush();
        return player;
    }

    public void unregisterPlayersToken(Player player) {
        player.setToken(null);
        entityManager.merge(player);
        entityManager.flush();
    }

    public Player findUserBySecondaryId(UUID secondaryId) {
        return entityManager.createQuery("SELECT p FROM Player p WHERE secondaryId = :secondaryId", Player.class)
            .setParameter("secondaryId", secondaryId)
            .getSingleResult();   
    }

    private String generateToken(UUID secondaryId, String ipAddress, SystemRole role) {
        var embeddedRole = new HashSet<>(Arrays.asList(SystemRole.USER.name()));
        if (role.equals(SystemRole.ADMIN)) {
            embeddedRole.add(SystemRole.ADMIN.name());
        }

        try {
            String token = Jwt.issuer(Issuer) // This value must match the server-side mp.jwt.verify.issuer configuration for the token to be considered valid.
                .upn(secondaryId.toString()) // Using the player's secondaryId as the subject -> makes it easier to search within the SecurityContext
                .groups(embeddedRole) 
                .claim("address", ipAddress)
                .audience("using-jwt")
                // Set token expiration in seconds (e.g., 3600 seconds = 60 minutes)
                .expiresIn(TOKEN_EXPIRATION_SECONDS)
                .sign();

            logger.info("TOKEN generated: " + token);
            return token;
        } catch (Exception e) {
            logger.error("Error generating JWT", e);
            throw new RuntimeException(e);
        }
    }
}