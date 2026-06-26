/**
 * Computes the SHA-256 hash of the given input string.
 *
 * @param input logical key to hash
 * @return 64-character hexadecimal SHA-256 hash
 */
package hashing;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
    private HashUtil() {
    }

    public static String hash(String input) {
        byte[] bytes;
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "SHA-256 algorithm not available", e);
        }
        bytes = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
        BigInteger number = new BigInteger(1, bytes);

        StringBuilder sb = new StringBuilder(number.toString(16));

        while (sb.length() < 64) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }
}
