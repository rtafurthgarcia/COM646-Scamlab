package service;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;
import org.jose4j.jwt.JwtClaims;

import exception.PlayerException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import model.entity.Player;
import model.entity.Player.SystemRole;
import repository.PlayerRepository;
import utils.TokenUtils;

import java.util.Arrays;
import java.util.UUID;

@ApplicationScoped
public class PlayerService {

    @Inject
    PlayerRepository repository;

    @Inject
    Logger logger;

    public Player registerNewPlayer(String ipAddress) {
        var role = SystemRole.PLAYER;

        if (ipAddress.equals("127.0.0.1")) {
            role = SystemRole.ADMIN;
        }

        var player = repository.find("ipAddress", ipAddress).firstResult();

        if (player != null && ! player.getToken().isEmpty()) {
            throw new PlayerException("One device cannot play more than once at the same time");
        }

        player = new Player().setIpAddress(ipAddress).setSystemRole(role).setIsBot(false);
        var token = generateToken(player.getSecondaryId().toString(), ipAddress, role);
        player.setToken(token);

        repository.persistAndFlush(player);
        return player;
    }

    public void unregisterPlayersToken(Player player) {
        player.setToken(null);
        repository.persistAndFlush(player);
    }

    public Player findUserBySecondaryId(UUID secondaryId) {
        return repository.find("secondary_id", secondaryId).firstResult();   
    }

    private String generateToken(String secondaryId, String ipAddress, SystemRole role) {
        try {
            var jwtClaims = new JwtClaims();
            jwtClaims.setIssuer("Richard Tafurth-Garcia");
            jwtClaims.setJwtId(UUID.randomUUID().toString());
            jwtClaims.setSubject(secondaryId);
            jwtClaims.setClaim(Claims.address.name(), ipAddress);
            jwtClaims.setClaim(Claims.groups.name(), Arrays.asList(role.name()));
            jwtClaims.setAudience("using-jwt");
            jwtClaims.setExpirationTimeMinutesInTheFuture(60);

            var token = TokenUtils.generateTokenString(jwtClaims);
            logger.info("TOKEN generated: " + token);
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}