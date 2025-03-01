package service;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logmanager.Logger;
import org.jose4j.jwt.JwtClaims;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import repository.PlayerRepository;
import resource.PlayerResource.SystemRole;
import utils.TokenUtils;

import java.util.Arrays;
import java.util.UUID;

@RequestScoped
public class PlayerService {

    @Inject
    PlayerRepository playerRepository;

    public final static Logger LOGGER = Logger.getLogger(PlayerService.class.getSimpleName());

    public String registerNewPlayer(String ipAddress) {
        SystemRole role = SystemRole.PLAYER;
        
        if (ipAddress.equals("127.0.0.1")) {
            role = SystemRole.ADMIN;
        }

        return generateToken(ipAddress, role);
    }

    private String generateToken(String subject, SystemRole role) {
        try {
            JwtClaims jwtClaims = new JwtClaims();
            jwtClaims.setIssuer("DonauTech"); // change to your company
            jwtClaims.setJwtId(UUID.randomUUID().toString());
            jwtClaims.setSubject(subject);
            jwtClaims.setClaim(Claims.upn.name(), subject);
            jwtClaims.setClaim(Claims.preferred_username.name(), UUID.randomUUID()); //add more
            jwtClaims.setClaim(Claims.groups.name(), Arrays.asList(role.name()));
            jwtClaims.setAudience("using-jwt");
            jwtClaims.setExpirationTimeMinutesInTheFuture(60);


            String token = TokenUtils.generateTokenString(jwtClaims);
            LOGGER.info("TOKEN generated: " + token);
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}