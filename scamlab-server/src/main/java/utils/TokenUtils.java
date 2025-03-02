package utils;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Utilities for generating a JWT for testing
 */
public class TokenUtils {

    private TokenUtils() {
    }

    public static String generateTokenString(JwtClaims claims) throws Exception {
        // Use the private key associated with the public key for a valid signature
        var pk = readPrivateKey("/privatekey.pem");

        return generateTokenString(pk, "/privatekey.pem", claims);
    }

    private static String generateTokenString(PrivateKey privateKey, String kid, JwtClaims claims) throws Exception {

        var currentTimeInSecs = currentTimeInSecs();

        claims.setIssuedAt(NumericDate.fromSeconds(currentTimeInSecs));
        claims.setClaim(Claims.auth_time.name(), NumericDate.fromSeconds(currentTimeInSecs));

        for (var entry : claims.getClaimsMap().entrySet()) {
            System.out.printf("\tAdded claim: %s, value: %s\n", entry.getKey(), entry.getValue());
        }

        var jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(privateKey);
        jws.setKeyIdHeaderValue(kid);
        jws.setHeader("typ", "JWT");
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        return jws.getCompactSerialization();
    }

    /**
     * Read a PEM encoded private key from the classpath
     *
     * @param pemResName - key file resource name
     * @return PrivateKey
     * @throws Exception on decode failure
     */
    public static PrivateKey readPrivateKey(final String pemResName) throws Exception {
        var contentIS = TokenUtils.class.getResourceAsStream(pemResName);
        var tmp = new byte[4096];
        var length = contentIS.read(tmp);
        return decodePrivateKey(new String(tmp, 0, length, "UTF-8"));
    }

    /**
     * Decode a PEM encoded private key string to an RSA PrivateKey
     *
     * @param pemEncoded - PEM string for private key
     * @return PrivateKey
     * @throws Exception on decode failure
     */
    public static PrivateKey decodePrivateKey(final String pemEncoded) throws Exception {
        var encodedBytes = toEncodedBytes(pemEncoded);

        var keySpec = new PKCS8EncodedKeySpec(encodedBytes);
        var kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

    private static byte[] toEncodedBytes(final String pemEncoded) {
        final var normalizedPem = removeBeginEnd(pemEncoded);
        return Base64.getDecoder().decode(normalizedPem);
    }

    private static String removeBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN (.*)-----", "");
        pem = pem.replaceAll("-----END (.*)----", "");
        pem = pem.replaceAll("\r\n", "");
        pem = pem.replaceAll("\n", "");
        return pem.trim();
    }

    /**
     * @return the current time in seconds since epoch
     */
    public static int currentTimeInSecs() {
        var currentTimeMS = System.currentTimeMillis();
        return (int) (currentTimeMS / 1000);
    }
}