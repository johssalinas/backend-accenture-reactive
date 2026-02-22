package co.com.accenture.usecase.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class IdempotencyHashHelper {

    private IdempotencyHashHelper() {
    }

    public static String hash(String payload) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = messageDigest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("No fue posible inicializar SHA-256", error);
        }
    }
}
