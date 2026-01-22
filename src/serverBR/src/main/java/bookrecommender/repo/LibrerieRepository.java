package bookrecommender.repo;

import bookrecommender.db.Db;
import bookrecommender.model.Library;

import java.sql.*;
import java.util.*;

/**
 * Repository JDBC per la gestione delle librerie degli utenti.
 *
 * <p>
 * La classe fornisce metodi di accesso ai dati relativi alle librerie personali,
 * permettendo di recuperare, salvare e cancellare librerie e di verificare
 * l’appartenenza di un libro a una libreria utente. Le operazioni sono
 * implementate tramite JDBC e, quando necessario, gestite in modo transazionale.
 * </p>
 *
 * <ul>
 *   <li>Recupero delle librerie di un utente con i relativi libri</li>
 *   <li>Salvataggio completo di una libreria con sovrascrittura dei contenuti</li>
 *   <li>Cancellazione di una libreria</li>
 *   <li>Verifica della presenza di un libro nelle librerie di un utente</li>
 * </ul>
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.db.Db
 * @see bookrecommender.model.Library
 */
@SuppressWarnings("ALL")
public class LibrerieRepository {

    private final Db db;

    /**
     * Costruisce il repository inizializzandolo con l’oggetto di accesso al database.
     *
     * <p>
     * L’istanza {@link Db} viene utilizzata per ottenere le connessioni JDBC
     * necessarie all’esecuzione delle operazioni sulle librerie.
     * </p>
     *
     * @param db oggetto di accesso al database
     */
    public LibrerieRepository(Db db) {
        this.db = db;
    }

    /**
     * Recupera tutte le librerie associate a un determinato utente.
     *
     * <p>
     * Per ciascuna libreria vengono recuperati i libri associati e aggregati
     * in un oggetto {@link Library}. Le librerie vengono restituite ordinate
     * per nome.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @return lista delle librerie dell’utente con i relativi libri
     */
    public List<Library> findByUserId(String userid) {
        String sql = """
            SELECT l.userid, l.nome, lb.libro_id
            FROM br.librerie l
            LEFT JOIN br.librerie_libri lb
              ON lb.userid = l.userid AND lb.nome = l.nome
            WHERE l.userid = ?
            ORDER BY l.nome
        """;

        Map<String, LinkedHashSet<Integer>> map = new LinkedHashMap<>();

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nome = rs.getString("nome");
                    Integer libroId = (Integer) rs.getObject("libro_id");

                    map.putIfAbsent(nome, new LinkedHashSet<>());
                    if (libroId != null) map.get(nome).add(libroId);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB findByUserId (librerie): " + e.getMessage(), e);
        }

        List<Library> out = new ArrayList<>();
        for (var entry : map.entrySet()) {
            out.add(new Library(userid, entry.getKey(), entry.getValue()));
        }
        return out;
    }

    /**
     * Salva una libreria sovrascrivendo completamente i libri contenuti.
     *
     * <p>
     * L’operazione viene eseguita in transazione: la libreria viene inserita
     * se non esiste, quindi tutti i libri associati vengono rimossi e reinseriti
     * in base allo stato dell’oggetto {@link Library} fornito.
     * </p>
     *
     * @param lib libreria da salvare
     * @throws IllegalArgumentException se la libreria è nulla
     */
    public void saveLibrary(Library lib) {
        if (lib == null) throw new IllegalArgumentException("Library null");

        String insLib = "INSERT INTO br.librerie(userid, nome) VALUES (?, ?) ON CONFLICT DO NOTHING";
        String delBooks = "DELETE FROM br.librerie_libri WHERE userid=? AND nome=?";
        String insBook = "INSERT INTO br.librerie_libri(userid, nome, libro_id) VALUES (?, ?, ?)";

        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps1 = c.prepareStatement(insLib);
                 PreparedStatement ps2 = c.prepareStatement(delBooks);
                 PreparedStatement ps3 = c.prepareStatement(insBook)) {

                ps1.setString(1, lib.getUserid());
                ps1.setString(2, lib.getNome());
                ps1.executeUpdate();

                ps2.setString(1, lib.getUserid());
                ps2.setString(2, lib.getNome());
                ps2.executeUpdate();

                for (int bookId : lib.getBookIds()) {
                    ps3.setString(1, lib.getUserid());
                    ps3.setString(2, lib.getNome());
                    ps3.setInt(3, bookId);
                    ps3.addBatch();
                }
                ps3.executeBatch();

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB saveLibrary: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina una libreria associata a un determinato utente.
     *
     * <p>
     * La cancellazione rimuove la libreria identificata dalla coppia
     * (utente, nome). I libri associati vengono eliminati per effetto
     * dei vincoli di integrità del database.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @param nome nome della libreria
     */
    public void deleteLibrary(String userid, String nome) {
        String sql = "DELETE FROM br.librerie WHERE userid=? AND nome=?";

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userid);
            ps.setString(2, nome);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB deleteLibrary: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se un utente possiede un determinato libro in una qualsiasi libreria.
     *
     * <p>
     * L’operazione termina alla prima occorrenza trovata, restituendo
     * un valore booleano che indica la presenza del libro.
     * </p>
     *
     * @param userid identificativo dell’utente
     * @param bookId identificativo del libro
     * @return true se il libro è presente in almeno una libreria dell’utente, false altrimenti
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean userHasBook(String userid, int bookId) {
        String sql = """
            SELECT 1
            FROM br.librerie_libri
            WHERE userid = ? AND libro_id = ?
            LIMIT 1
        """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userid);
            ps.setInt(2, bookId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB userHasBook: " + e.getMessage(), e);
        }
    }
}
