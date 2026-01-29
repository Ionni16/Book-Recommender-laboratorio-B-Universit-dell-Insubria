package bookrecommender.repo;

import bookrecommender.db.Db;
import bookrecommender.model.User;

import java.sql.*;

/**
 * Repository JDBC per la gestione degli utenti registrati.
 *
 * <p>
 * La classe fornisce operazioni di accesso ai dati per la tabella degli utenti registrati,
 * includendo conteggio, ricerca per identificativo, inserimento e aggiornamenti puntuali
 * (email e hash password). Include inoltre una cancellazione a "cascata" applicativa
 * che rimuove l’account e i dati collegati tramite una transazione JDBC.
 * </p>
 *
 * <ul>
 *   <li>Conteggio degli utenti registrati</li>
 *   <li>Verifica esistenza e recupero dati utente</li>
 *   <li>Inserimento utente e aggiornamenti di credenziali/contatti</li>
 *   <li>Cancellazione definitiva dell’account con rimozione dei dati correlati</li>
 * </ul>
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.db.Db
 * @see bookrecommender.model.User
 */
@SuppressWarnings("ClassCanBeRecord")
public class UtentiRepository {

    private final Db db;

    /**
     * Costruisce il repository inizializzandolo con l’oggetto di accesso al database.
     *
     * <p>
     * L’istanza {@link Db} viene utilizzata per ottenere le connessioni JDBC
     * necessarie all’esecuzione delle operazioni sugli utenti registrati.
     * </p>
     *
     * @param db oggetto di accesso al database
     */
    public UtentiRepository(Db db) {
        this.db = db;
    }

    /**
     * Restituisce il numero totale di utenti registrati.
     *
     * <p>
     * Esegue una query di conteggio sulla tabella degli utenti registrati.
     * </p>
     *
     * @return numero totale di utenti presenti
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM br.utenti_registrati";
        try (Connection c = db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Errore DB utenti count: " + e.getMessage(), e);
        }
    }

    /**
     * Aggiorna l hash della password di un utente.
     *
     * <p>
     * Esegue un aggiornamento puntuale della colonna {@code password_hash} per l’utente
     * identificato da {@code userid}. Il metodo restituisce {@code true} solo se viene
     * aggiornata esattamente una riga.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @param newHash nuovo hash della password
     * @return true se l’aggiornamento ha modificato una sola riga, false altrimenti
     */
    public boolean updatePasswordHash(String userid, String newHash) {
        String sql = "UPDATE br.utenti_registrati SET password_hash=? WHERE userid=?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, userid);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Errore DB updatePasswordHash: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica l’esistenza di un utente registrato a partire dall’identificativo.
     *
     * <p>
     * La verifica è basata sul recupero dell’utente tramite {@link #findByUserid(String)}.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @return true se l’utente esiste, false altrimenti
     */
    public boolean exists(String userid) {
        return findByUserid(userid) != null;
    }

    /**
     * Recupera un utente registrato a partire dall’identificativo.
     *
     * <p>
     * Se {@code userid} è nullo o vuoto, il metodo restituisce {@code null}.
     * Se non esiste alcun record corrispondente, restituisce {@code null}.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @return utente trovato, oppure {@code null} se assente o se {@code userid} non valido
     */
    public User findByUserid(String userid) {
        if (userid == null || userid.isBlank()) return null;

        String sql = """
            SELECT userid, password_hash, nome, cognome, codice_fiscale, email
            FROM br.utenti_registrati
            WHERE userid = ?
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userid);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(
                        rs.getString("userid"),
                        rs.getString("password_hash"),
                        rs.getString("nome"),
                        rs.getString("cognome"),
                        rs.getString("codice_fiscale"),
                        rs.getString("email")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB findByUserid: " + e.getMessage(), e);
        }
    }

    /**
     * Inserisce un nuovo utente registrato.
     *
     * <p>
     * Richiede un oggetto {@link User} non nullo e un {@code userid} valorizzato.
     * Il campo {@code passwordHash} deve essere già calcolato dall’esterno
     * (ad esempio tramite SHA-256) prima dell’inserimento.
     * </p>
     *
     * @param u utente da inserire
     * @throws IllegalArgumentException se {@code u} è nullo o se {@code userid} è nullo/vuoto
     */
    public void add(User u) {
        if (u == null) throw new IllegalArgumentException("User null");
        if (u.getUserid() == null || u.getUserid().isBlank()) throw new IllegalArgumentException("userid vuoto");

        String sql = """
            INSERT INTO br.utenti_registrati(userid, password_hash, nome, cognome, codice_fiscale, email)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, u.getUserid());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getNome());
            ps.setString(4, u.getCognome());
            ps.setString(5, u.getCodiceFiscale());
            ps.setString(6, u.getEmail());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB add user: " + e.getMessage(), e);
        }
    }

    /**
     * Aggiorna l’indirizzo email di un utente.
     *
     * <p>
     * Esegue un aggiornamento puntuale della colonna {@code email} per l’utente
     * identificato da {@code userid}. Il metodo restituisce {@code true} solo se viene
     * aggiornata esattamente una riga.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @param newEmail nuovo indirizzo email
     * @return true se l’aggiornamento ha modificato una sola riga, false altrimenti
     */
    public boolean updateEmail(String userid, String newEmail) {
        String sql = "UPDATE br.utenti_registrati SET email=? WHERE userid=?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, newEmail);
            ps.setString(2, userid);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB updateEmail: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina definitivamente un account e i dati collegati dell’utente.
     *
     * <p>
     * L’operazione è eseguita in transazione e applica un ordine di cancellazione
     * progettato per ridurre il rischio di violazioni di vincoli di integrità
     * referenziale. Se {@code userid} è nullo o vuoto, il metodo restituisce {@code false}.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @return true se l’utente è stato eliminato (una riga rimossa), false altrimenti
     */
    public boolean deleteAccountCascade(String userid) {
        if (userid == null || userid.isBlank()) return false;

        String delReviews = "DELETE FROM br.valutazioni_libri WHERE userid = ?";
        String delSuggestions = "DELETE FROM br.consigli_libri WHERE userid = ?";
        String delLibBooks = "DELETE FROM br.librerie_libri WHERE userid = ?";
        String delLibs = "DELETE FROM br.librerie WHERE userid = ?";
        String delUser = "DELETE FROM br.utenti_registrati WHERE userid = ?";

        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps1 = c.prepareStatement(delReviews);
                 PreparedStatement ps2 = c.prepareStatement(delSuggestions);
                 PreparedStatement ps3 = c.prepareStatement(delLibBooks);
                 PreparedStatement ps4 = c.prepareStatement(delLibs);
                 PreparedStatement ps5 = c.prepareStatement(delUser)) {

                ps1.setString(1, userid);
                ps1.executeUpdate();

                ps2.setString(1, userid);
                ps2.executeUpdate();

                ps3.setString(1, userid);
                ps3.executeUpdate();

                ps4.setString(1, userid);
                ps4.executeUpdate();

                ps5.setString(1, userid);
                int rows = ps5.executeUpdate();

                c.commit();
                return rows == 1;

            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB deleteAccountCascade: " + e.getMessage(), e);
        }
    }
}
