package in.ctrlplussubmit.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * PasswordUtil — BCrypt Password Hashing Utility
 *
 * Wraps jBCrypt for secure password storage and verification.
 *
 * Usage:
 *   String hash   = PasswordUtil.hash("MyPassword@123");
 *   boolean match = PasswordUtil.verify("MyPassword@123", hash);
 *
 * Dependency in pom.xml:
 *   <dependency>
 *       <groupId>org.mindrot</groupId>
 *       <artifactId>jbcrypt</artifactId>
 *       <version>0.4</version>
 *   </dependency>
 */
public class PasswordUtil {

    /**
     * BCrypt work factor (cost).
     * 12 = ~250ms per hash on modern hardware.
     * Increase for stronger security (14–16 for production).
     */
    private static final int BCRYPT_ROUNDS = 12;

    // Private constructor — static utility class
    private PasswordUtil() {}

    /**
     * Hashes a plain-text password using BCrypt.
     *
     * @param  plainTextPassword  the raw password from the user
     * @return BCrypt hash string (60 chars, safe to store in DB)
     * @throws IllegalArgumentException if password is null or blank
     */
    public static String hash(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or empty.");
        }
        String salt = BCrypt.gensalt(BCRYPT_ROUNDS);
        System.out.println("Salt : "+salt);
        return BCrypt.hashpw(plainTextPassword, salt);
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     *
     * @param  plainTextPassword  the raw password entered by the user
     * @param  storedHash         the BCrypt hash retrieved from the database
     * @return true if the password matches, false otherwise
     */
    public static boolean verify(String plainTextPassword, String storedHash) {
        if (plainTextPassword == null || storedHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, storedHash);
        } catch (Exception e) {
            // Malformed hash in DB — treat as mismatch
            System.err.println("[PasswordUtil] Invalid hash format: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------
    // Quick test — run this to generate your first admin hash
    // -------------------------------------------------------
    public static void main(String[] args) {
        String rawPassword = "Admin@123";
        String hash = PasswordUtil.hash(rawPassword);

        System.out.println("Raw Password : " + rawPassword);
        System.out.println("BCrypt Hash  : " + hash);
        System.out.println("Verify Test  : " + PasswordUtil.verify(rawPassword, hash));

        // Paste the hash output into your SQL seed INSERT or UPDATE statement:
        // UPDATE users SET password_hash = '<hash>' WHERE email = 'admin@taskflow.com';
    }
}
