package bookrecommender.repo;

import bookrecommender.db.Db;
import bookrecommender.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository JDBC per ValutazioniLibri.
 */
public class ValutazioniRepository {

    private final Db db;

    public ValutazioniRepository(Db db) {
        this.db = db;
    }

    public List<Review> findByBookId(int bookId) {
        String sql = """
            SELECT userid, libro_id, stile, contenuto, gradevolezza, originalita, edizione, voto_finale, commento
            FROM br.valutazioni_libri
            WHERE libro_id = ?
            """;

        List<Review> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, bookId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Review(
                            rs.getString("userid"),
                            rs.getInt("libro_id"),
                            rs.getInt("stile"),
                            rs.getInt("contenuto"),
                            rs.getInt("gradevolezza"),
                            rs.getInt("originalita"),
                            rs.getInt("edizione"),
                            rs.getInt("voto_finale"),
                            rs.getString("commento")
                    ));
                }
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB findByBookId (valutazioni): " + e.getMessage(), e);
        }
    }

    /**
     * Salva o aggiorna la valutazione (PK: userid+libro_id).
     */
    public void upsert(Review r) {
        if (r == null) throw new IllegalArgumentException("Review null");

        String sql = """
            INSERT INTO br.valutazioni_libri
              (userid, libro_id, stile, contenuto, gradevolezza, originalita, edizione, voto_finale, commento)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (userid, libro_id) DO UPDATE
            SET stile = EXCLUDED.stile,
                contenuto = EXCLUDED.contenuto,
                gradevolezza = EXCLUDED.gradevolezza,
                originalita = EXCLUDED.originalita,
                edizione = EXCLUDED.edizione,
                voto_finale = EXCLUDED.voto_finale,
                commento = EXCLUDED.commento
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, r.getUserid());
            ps.setInt(2, r.getBookId());
            ps.setInt(3, r.getStile());
            ps.setInt(4, r.getContenuto());
            ps.setInt(5, r.getGradevolezza());
            ps.setInt(6, r.getOriginalita());
            ps.setInt(7, r.getEdizione());
            ps.setInt(8, r.getVotoFinale());
            ps.setString(9, r.getCommento());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB upsert valutazione: " + e.getMessage(), e);
        }
    }

    public List<Review> findByUserId(String userid) {
        String sql = """
            SELECT userid, libro_id, stile, contenuto, gradevolezza, originalita, edizione, voto_finale, commento
            FROM br.valutazioni_libri
            WHERE userid = ?
            ORDER BY libro_id
        """;

        List<Review> out = new ArrayList<>();

        try (Connection c = db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review(
                            rs.getString("userid"),
                            rs.getInt("libro_id"),
                            rs.getInt("stile"),
                            rs.getInt("contenuto"),
                            rs.getInt("gradevolezza"),
                            rs.getInt("originalita"),
                            rs.getInt("edizione"),
                            rs.getInt("voto_finale"),
                            rs.getString("commento")
                    );
                    out.add(r);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB findByUserId (valutazioni): " + e.getMessage(), e);
        }

        return out;
    }

    public boolean delete(String userid, int libroId) {
        String sql = "DELETE FROM br.valutazioni_libri WHERE userid = ? AND libro_id = ?";
        try (Connection c = db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userid);
            ps.setInt(2, libroId);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB delete review: " + e.getMessage(), e);
        }
    }


}
