package bookrecommender.repo;

import bookrecommender.db.Db;
import bookrecommender.model.Suggestion;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Repository JDBC per la gestione dei suggerimenti di libri.
 *
 * <p>
 * La classe fornisce metodi di accesso ai dati per la tabella dei suggerimenti,
 * eseguendo operazioni JDBC con gestione transazionale quando necessario e
 * restituendo i risultati sotto forma di oggetti {@link Suggestion}.
 * È applicato un vincolo applicativo di massimo tre suggerimenti per
 * ciascuna coppia (utente, libro).
 * </p>
 *
 * <ul>
 *   <li>Recupero dei suggerimenti per libro con aggregazione per utente</li>
 *   <li>Recupero dei suggerimenti per utente con raggruppamento per libro</li>
 *   <li>Inserimento, sostituzione e cancellazione dei suggerimenti</li>
 * </ul>
 *
 * @author Matteo Ferrario
 * @version 2.0
 * @see bookrecommender.db.Db
 * @see bookrecommender.model.Suggestion
 */
@SuppressWarnings("ClassCanBeRecord")
public class ConsigliRepository {

    private final Db db;

    /**
     * Costruisce il repository inizializzandolo con l’oggetto di accesso al database.
     *
     * <p>
     * L’istanza {@link Db} viene utilizzata per ottenere le connessioni JDBC
     * necessarie all’esecuzione delle operazioni sui suggerimenti.
     * </p>
     *
     * @param db oggetto di accesso al database
     */
    public ConsigliRepository(Db db) {
        this.db = db;
    }

    /**
     * Recupera tutti i suggerimenti associati a un determinato libro,
     * aggregandoli per utente.
     *
     * <p>
     * Per ciascun utente viene restituito un oggetto {@link Suggestion}
     * contenente l’identificativo dell’utente, del libro e la lista
     * dei libri suggeriti ordinati.
     * </p>
     *
     * @param bookId identificativo del libro
     * @return lista di suggerimenti aggregati per utente
     */
    public List<Suggestion> findByBookId(int bookId) {
        String sql = """
            SELECT userid, libro_id,
                   array_agg(suggerito_id ORDER BY suggerito_id) AS suggeriti
            FROM br.consigli_libri
            WHERE libro_id = ?
            GROUP BY userid, libro_id
            """;

        List<Suggestion> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, bookId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Array a = rs.getArray("suggeriti");
                    Integer[] arr = (Integer[]) a.getArray();

                    List<Integer> sug = new ArrayList<>();
                    if (arr != null) {
                        for (Integer x : arr) if (x != null) sug.add(x);
                    }

                    out.add(new Suggestion(
                            rs.getString("userid"),
                            rs.getInt("libro_id"),
                            sug
                    ));
                }
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB findByBookId (consigli): " + e.getMessage(), e);
        }
    }


    /**
     * Recupera tutti i suggerimenti inseriti da un utente,
     * raggruppandoli per libro.
     *
     * @param userid identificativo dell’utente
     * @return lista di suggerimenti raggruppati per libro
     */
    public List<Suggestion> findByUserId(String userid) {
        String sql = """
            SELECT userid, libro_id, suggerito_id
            FROM br.consigli_libri
            WHERE userid = ?
            ORDER BY libro_id, suggerito_id
        """;

        Map<Integer, List<Integer>> map = new LinkedHashMap<>();

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int bookId = rs.getInt("libro_id");
                    int suggestedId = rs.getInt("suggerito_id");
                    map.computeIfAbsent(bookId, k -> new ArrayList<>()).add(suggestedId);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB findByUserId (consigli): " + e.getMessage(), e);
        }

        List<Suggestion> out = new ArrayList<>();
        for (var e : map.entrySet()) {
            out.add(new Suggestion(userid, e.getKey(), e.getValue()));
        }
        return out;
    }

    /**
     * Sostituisce completamente i suggerimenti per una coppia (utente, libro),
     * applicando il limite massimo di tre elementi.
     *
     * @param userid identificativo dell’utente
     * @param bookId identificativo del libro
     * @param suggestedIds lista dei libri suggeriti
     * @return true se l’operazione è completata correttamente
     */
    public boolean replaceSuggestionsMax3(String userid, int bookId, List<Integer> suggestedIds) {
        if (userid == null || userid.isBlank()) throw new IllegalArgumentException("userid vuoto");
        if (suggestedIds == null) suggestedIds = List.of();

        List<Integer> ids = suggestedIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .toList();

        String delSql = "DELETE FROM br.consigli_libri WHERE userid=? AND libro_id=?";
        String insSql = "INSERT INTO br.consigli_libri(userid, libro_id, suggerito_id) VALUES (?, ?, ?)";

        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement del = c.prepareStatement(delSql)) {
                del.setString(1, userid);
                del.setInt(2, bookId);
                del.executeUpdate();
            }

            try (PreparedStatement ins = c.prepareStatement(insSql)) {
                for (Integer sid : ids) {
                    ins.setString(1, userid);
                    ins.setInt(2, bookId);
                    ins.setInt(3, sid);
                    ins.addBatch();
                }
                ins.executeBatch();
            }

            c.commit();
            return true;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB replaceSuggestionsMax3: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina tutti i suggerimenti associati a una coppia (utente, libro).
     *
     * @param userid identificativo dell’utente
     * @param bookId identificativo del libro
     */
    public void deleteAllForUserBook(String userid, int bookId) {
        String sql = "DELETE FROM br.consigli_libri WHERE userid=? AND libro_id=?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userid);
            ps.setInt(2, bookId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB deleteAllForUserBook: " + e.getMessage(), e);
        }
    }
}
