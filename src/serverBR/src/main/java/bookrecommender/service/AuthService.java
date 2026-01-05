package bookrecommender.service;

import bookrecommender.model.User;
import bookrecommender.repo.UtentiRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AuthService {

    private final UtentiRepository repo;

    public AuthService(UtentiRepository repo) {
        this.repo = repo;
    }

    public User login(String userid, String passwordOrHash) {
        if (userid == null || passwordOrHash == null) return null;

        User u = repo.findByUserid(userid);
        if (u == null) return null;

        String incomingHash = normalizeToHash(passwordOrHash);
        String storedHash = u.getPasswordHash(); // nel file è già hash

        if (storedHash == null) return null;

        if (!storedHash.equalsIgnoreCase(incomingHash)) return null;
        return u;
    }

    public boolean registrazione(User u) {
        if (u == null) return false;
        if (u.getUserid() == null || u.getUserid().isBlank()) return false;
        if (u.getPasswordHash() == null || u.getPasswordHash().isBlank()) return false;

        if (repo.exists(u.getUserid())) return false;

        // Normalizza SEMPRE a hash prima di salvare
        String normalizedHash = normalizeToHash(u.getPasswordHash());
        User normalizedUser = new User(u.getUserid(), normalizedHash, u.getNome(), u.getCognome(), u.getCodiceFiscale(), u.getEmail());

        repo.add(normalizedUser);
        return true;
    }

    // --- helpers ---
    private static String normalizeToHash(String passwordOrHash) {
        // se sembra già SHA-256 hex (64 char) lo accetto
        if (passwordOrHash.matches("^[a-fA-F0-9]{64}$")) {
            return passwordOrHash.toLowerCase();
        }
        // altrimenti lo hasho
        return sha256(passwordOrHash);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean changePassword(String userid, String newPlainPassword) {
        if (userid == null || userid.isBlank()) return false;
        if (newPlainPassword == null || newPlainPassword.isBlank()) return false;

        // usa lo STESSO meccanismo di login/registrazione
        String newHash = normalizeToHash(newPlainPassword);

        return repo.updatePasswordHash(userid, newHash);
    }


}
