package bookrecommender.service;

import bookrecommender.model.User;
import bookrecommender.repo.UtentiRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Servizio applicativo per autenticazione e gestione credenziali utente.
 *
 * <p>
 * La classe incapsula la logica di login, registrazione e cambio password, delegando
 * le operazioni di persistenza a {@link UtentiRepository}. Le credenziali vengono
 * normalizzate a hash SHA-256 in formato esadecimale: se l’input è già un hash valido
 * (64 caratteri hex) viene accettato, altrimenti viene calcolato l’hash dell’input.
 * </p>
 *
 * <ul>
 *   <li>Autenticazione tramite confronto tra hash memorizzato e hash normalizzato in ingresso</li>
 *   <li>Registrazione con normalizzazione preventiva della password a hash</li>
 *   <li>Aggiornamento della password tramite lo stesso meccanismo di normalizzazione</li>
 * </ul>
 *
 * @author Richard Zefi
 * @version 1.0
 * @see bookrecommender.repo.UtentiRepository
 * @see bookrecommender.model.User
 */
@SuppressWarnings("ClassCanBeRecord")
public class AuthService {

    private final UtentiRepository repo;

    /**
     * Costruisce il servizio inizializzandolo con il repository utenti.
     *
     * <p>
     * Il repository viene utilizzato per recuperare, inserire e aggiornare i dati
     * relativi agli utenti registrati.
     * </p>
     *
     * @param repo repository per l’accesso agli utenti registrati
     */
    public AuthService(UtentiRepository repo) {
        this.repo = repo;
    }

    /**
     * Esegue il login validando le credenziali fornite.
     *
     * <p>
     * Recupera l’utente dal repository e confronta l’hash memorizzato con l’hash
     * derivato dall’input {@code passwordOrHash} tramite normalizzazione a SHA-256.
     * Se un qualsiasi controllo fallisce, il metodo restituisce {@code null}.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @param passwordOrHash password in chiaro oppure hash SHA-256 esadecimale
     * @return utente autenticato, oppure {@code null} in caso di credenziali non valide
     */
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

    /**
     * Registra un nuovo utente normalizzando la password a hash SHA-256 prima del salvataggio.
     *
     * <p>
     * Verifica la validità minima dei dati (utente non nullo, userid e password valorizzati)
     * e l’assenza di un utente già esistente con lo stesso identificativo. La password
     * viene normalizzata a hash SHA-256: se l’input è già un hash valido viene utilizzato,
     * altrimenti viene calcolato l’hash dell’input.
     * </p>
     *
     * @param u utente da registrare
     * @return true se la registrazione è completata, false se i dati non sono validi o l’utente esiste già
     */
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

    /**
     * Normalizza una credenziale a hash SHA-256 esadecimale.
     *
     * <p>
     * Se l’input rispetta il formato di un hash SHA-256 in esadecimale (64 caratteri),
     * viene accettato e convertito in minuscolo; altrimenti viene calcolato l’hash
     * SHA-256 dell’input.
     * </p>
     *
     * @param passwordOrHash password in chiaro oppure hash SHA-256 esadecimale
     * @return hash SHA-256 esadecimale in minuscolo
     */
    private static String normalizeToHash(String passwordOrHash) {
        // se già SHA-256 hex (64 char) lo accetto
        if (passwordOrHash.matches("^[a-fA-F0-9]{64}$")) {
            return passwordOrHash.toLowerCase();
        }
        // altrimenti hashing
        return sha256(passwordOrHash);
    }

    /**
     * Calcola l’hash SHA-256 di una stringa e lo restituisce in formato esadecimale.
     *
     * <p>
     * L’input viene convertito in byte utilizzando {@link StandardCharsets#UTF_8}.
     * </p>
     *
     * @param input stringa di input
     * @return hash SHA-256 esadecimale in minuscolo
     * @throws RuntimeException in caso di errore nella creazione dell’algoritmo o nel calcolo dell’hash
     * @see java.security.MessageDigest
     */
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

    /**
     * Aggiorna la password di un utente applicando la normalizzazione a hash SHA-256.
     *
     * <p>
     * Valida {@code userid} e password in ingresso e aggiorna la credenziale nel repository
     * utilizzando lo stesso meccanismo di normalizzazione impiegato per login e registrazione.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @param newPlainPassword nuova password (in chiaro o già in formato hash SHA-256 esadecimale)
     * @return true se l’aggiornamento ha avuto esito positivo, false altrimenti
     */
    public boolean changePassword(String userid, String newPlainPassword) {
        if (userid == null || userid.isBlank()) return false;
        if (newPlainPassword == null || newPlainPassword.isBlank()) return false;

        // usa lo STESSO meccanismo di login/registrazione
        String newHash = normalizeToHash(newPlainPassword);

        return repo.updatePasswordHash(userid, newHash);
    }
}
