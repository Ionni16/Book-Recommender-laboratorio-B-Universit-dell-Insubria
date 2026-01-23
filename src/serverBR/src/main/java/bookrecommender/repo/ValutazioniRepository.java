package bookrecommender.repo;

import bookrecommender.db.Db;
import bookrecommender.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository JDBC per la gestione delle valutazioni dei libri.
 *
 * <p>
 * La classe fornisce operazioni di accesso ai dati per la tabella delle valutazioni,
 * consentendo il recupero delle recensioni per libro o per utente, l’inserimento/aggiornamento
 * atomico tramite upsert e la cancellazione puntuale di una valutazione identificata dalla
 * chiave composta (userid, libro_id).
 * </p>
 *
 * <ul>
 *   <li>Recupero delle valutazioni associate a un libro</li>
 *   <li>Recupero delle valutazioni inserite da un utente</li>
 *   <li>Inserimento o aggiornamento della valutazione per chiave composta</li>
 *   <li>Cancellazione di una valutazione specifica</li>
 * </ul>
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.db.Db
 * @see bookrecommender.model.Review
 */
@SuppressWarnings("ClassCanBeRecord")
public class ValutazioniRepository {

    private final Db db;

    /**
     * Costruisce il repository inizializzandolo con l’oggetto di accesso al database.
     *
     * <p>
     * L’istanza {@link Db} viene utilizzata per ottenere le connessioni JDBC
     * necessarie all’esecuzione delle operazioni sulle valutazioni.
     * </p>
     *
     * @param db oggetto di accesso al database
     */
    public ValutazioniRepository(Db db) {
        this.db = db;
    }

    /**
     * Recupera tutte le valutazioni associate a un determinato libro.
     *
     * <p>
     * Esegue una query di selezione filtrata per {@code libro_id} e costruisce
     * una lista di {@link Review} a partire dalle righe restituite.
     * </p>
     *
     * @param bookId identificativo del libro
     * @return lista delle valutazioni del libro indicato
     */
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
     * Inserisce o aggiorna una valutazione identificata da chiave composta (userid, libro_id).
     *
     * <p>
     * Esegue un’operazione di upsert: se la valutazione non esiste viene inserita,
     * altrimenti viene aggiornata sostituendo i valori dei campi di punteggio e commento
     * con quelli forniti. L’oggetto {@link Review} deve essere non nullo.
     * </p>
     *
     * @param r valutazione da inserire o aggiornare
     * @throws IllegalArgumentException se {@code r} è nullo
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

    /**
     * Recupera tutte le valutazioni inserite da un determinato utente.
     *
     * <p>
     * Esegue una query filtrata per {@code userid} e restituisce le valutazioni
     * ordinate per identificativo del libro.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @return lista delle valutazioni dell’utente ordinate per libro
     */
    public List<Review> findByUserId(String userid) {
        String sql = """
            SELECT userid, libro_id, stile, contenuto, gradevolezza, originalitaF, edizione, voto_finale, commento
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

    /**
     * Elimina una valutazione identificata da utente e libro.
     *
     * <p>
     * Esegue una cancellazione puntuale sulla tabella delle valutazioni.
     * Il metodo restituisce {@code true} solo se viene eliminata esattamente una riga.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @param libroId identificativo del libro
     * @return true se la valutazione è stata eliminata, false altrimenti
     */
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
