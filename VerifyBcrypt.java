import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class VerifyBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String dbHash = "$2a$10$kZ7G0xbMvefoz5D.3r39eeasgmsNx9lO4TIc8hfGWCR2QaV3GQt4K";
        String initHash = "$2a$10$pJqMd5MkmTQUNbEFrAFaj.PMkVh4RrQXmuBKLNjXpglFJ.xIfZic.";
        check(encoder, "dbHash", dbHash);
        check(encoder, "initHash", initHash);
    }

    private static void check(BCryptPasswordEncoder encoder, String name, String hash) {
        try {
            boolean m = encoder.matches("admin123", hash);
            System.out.println(name + " matches(admin123)=" + m + " (no exception)");
        } catch (Exception e) {
            System.out.println(name + " THROWS: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
