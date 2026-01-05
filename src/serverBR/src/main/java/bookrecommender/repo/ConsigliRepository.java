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
 * Repository JDBC per ConsigliLibri.
 */
public class ConsigliRepository {

    private final Db db;

    public ConsigliRepository(Db db) {
        this.db = db;
    }

    /**
     * Serve per l'aggregazione: restituisce TUTTI i suggerimenti per un bookId,
     * raggruppati per utente (Suggestion = userid + bookId + lista suggeriti).
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
     * Inserisce un singolo suggerimento (userid, bookId -> suggestedId) rispettando MAX 3.
     * Ritorna true se inserito, false se già presente o se superi max 3.
     */
    public boolean addSuggestionMax3(String userid, int bookId, int suggestedId) {
        String countSql = """
            SELECT COUNT(*)
            FROM br.consigli_libri
            WHERE userid = ? AND libro_id = ?
            FOR UPDATE
            """;

        String insSql = """
            INSERT INTO br.consigli_libri(userid, libro_id, suggerito_id)
            VALUES (?, ?, ?)
            ON CONFLICT DO NOTHING
            """;

        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);

            int count;
            try (PreparedStatement ps = c.prepareStatement(countSql)) {
                ps.setString(1, userid);
                ps.setInt(2, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    count = rs.getInt(1);
                }
            }

            if (count >= 3) {
                c.rollback();
                return false;
            }

            int rows;
            try (PreparedStatement ps = c.prepareStatement(insSql)) {
                ps.setString(1, userid);
                ps.setInt(2, bookId);
                ps.setInt(3, suggestedId);
                rows = ps.executeUpdate();
            }

            c.commit();
            return rows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB addSuggestionMax3: " + e.getMessage(), e);
        }
    }

    public void removeSuggestion(String userid, int bookId, int suggestedId) {
        String sql = """
            DELETE FROM br.consigli_libri
            WHERE userid = ? AND libro_id = ? AND suggerito_id = ?
            """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userid);
            ps.setInt(2, bookId);
            ps.setInt(3, suggestedId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB removeSuggestion: " + e.getMessage(), e);
        }
    }

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

    public boolean replaceSuggestionsMax3(String userid, int bookId, List<Integer> suggestedIds) {
        if (userid == null || userid.isBlank()) throw new IllegalArgumentException("userid vuoto");
        if (suggestedIds == null) suggestedIds = List.of();

        // max 3
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
