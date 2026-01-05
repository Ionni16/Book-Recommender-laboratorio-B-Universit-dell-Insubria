package bookrecommender.repo;

import bookrecommender.db.Db;
import bookrecommender.model.User;

import java.sql.*;

/**
 * Repository JDBC per UtentiRegistrati.
 */
public class UtentiRepository {

    private final Db db;

    public UtentiRepository(Db db) {
        this.db = db;
    }

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


    public boolean exists(String userid) {
        return findByUserid(userid) != null;
    }

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
     * Inserisce un utente (passwordHash deve essere già SHA-256).
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
     * Elimina definitivamente l'account e tutti i dati collegati dell'utente.
     * Ordine di cancellazione pensato per evitare vincoli FK.
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
