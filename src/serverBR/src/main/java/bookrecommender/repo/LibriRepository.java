package bookrecommender.repo;

import bookrecommender.db.Db;
import bookrecommender.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository JDBC per l’accesso ai libri su PostgreSQL.
 *
 * <p>
 * La classe implementa le operazioni di lettura sui libri, restituendo sempre
 * oggetti {@link Book} con l’elenco degli autori aggregato tramite query SQL.
 * Supporta recupero puntuale per identificativo e ricerche per titolo, autore
 * e combinazione autore/anno, con limitazione del numero di risultati.
 * </p>
 *
 * <ul>
 *   <li>Recupero di un libro per identificativo con autori aggregati</li>
 *   <li>Elenco dei primi libri ordinati per identificativo</li>
 *   <li>Ricerche per titolo, autore e autore con anno</li>
 * </ul>
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.db.Db
 * @see bookrecommender.model.Book
 */
@SuppressWarnings("ClassCanBeRecord")
public class LibriRepository {

    private final Db db;

    /**
     * Costruisce il repository inizializzandolo con l’oggetto di accesso al database.
     *
     * <p>
     * L’istanza {@link Db} viene utilizzata per ottenere le connessioni JDBC
     * necessarie all’esecuzione delle query sui libri.
     * </p>
     *
     * @param db oggetto di accesso al database
     */
    public LibriRepository(Db db) {
        this.db = db;
    }

    /**
     * Recupera un libro a partire dal suo identificativo.
     *
     * <p>
     * La query restituisce i dati del libro e aggrega gli autori in un array,
     * costruendo un {@link Book} completo. Se il libro non esiste, il metodo
     * restituisce {@code null}.
     * </p>
     *
     * @param id identificativo del libro
     * @return libro trovato con autori aggregati, oppure {@code null} se assente
     */
    public Book findById(int id) {
        String sql = """
            SELECT l.id, l.titolo, l.anno, l.editore, l.categoria,
                   COALESCE(
                     array_agg(a.autore ORDER BY a.autore) FILTER (WHERE a.autore IS NOT NULL),
                     ARRAY[]::text[]
                   ) AS autori
            FROM br.libri l
            LEFT JOIN br.libri_autori a ON a.libro_id = l.id
            WHERE l.id = ?
            GROUP BY l.id, l.titolo, l.anno, l.editore, l.categoria
            """;

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapBook(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB findById: " + e.getMessage(), e);
        }
    }


    /**
     * Cerca libri il cui titolo contiene una substring, con confronto case-insensitive.
     *
     * <p>
     * Il parametro viene normalizzato tramite {@code trim}; se nullo o vuoto,
     * il metodo restituisce una lista vuota. I risultati includono sempre gli autori
     * aggregati e sono limitati dal parametro {@code limit}.
     * </p>
     *
     * @param titolo substring da ricercare nel titolo
     * @param limit numero massimo di risultati da restituire
     * @return lista dei libri trovati con autori aggregati
     */
    public List<Book> searchByTitleContains(String titolo, int limit) {
        if (titolo == null) return List.of();
        String q = titolo.trim();
        if (q.isEmpty()) return List.of();

        String sql = """
            SELECT l.id, l.titolo, l.anno, l.editore, l.categoria,
                   COALESCE(
                     array_agg(a.autore ORDER BY a.autore) FILTER (WHERE a.autore IS NOT NULL),
                     ARRAY[]::text[]
                   ) AS autori
            FROM br.libri l
            LEFT JOIN br.libri_autori a ON a.libro_id = l.id
            WHERE l.titolo ILIKE '%' || ? || '%'
            GROUP BY l.id, l.titolo, l.anno, l.editore, l.categoria
            ORDER BY l.id
            LIMIT ?
            """;

        List<Book> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, q);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapBook(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB searchByTitleContains: " + e.getMessage(), e);
        }
    }

    /**
     * Cerca libri che hanno almeno un autore contenente una substring, con confronto case-insensitive.
     *
     * <p>
     * Il parametro viene normalizzato tramite {@code trim}; se nullo o vuoto,
     * il metodo restituisce una lista vuota. La selezione utilizza una subquery
     * per verificare l’esistenza di autori compatibili e restituisce i libri con
     * autori aggregati, limitati da {@code limit}.
     * </p>
     *
     * @param autore substring da ricercare nel nome autore
     * @param limit numero massimo di risultati da restituire
     * @return lista dei libri trovati con autori aggregati
     */
    public List<Book> searchByAuthorContains(String autore, int limit) {
        if (autore == null) return List.of();
        String q = autore.trim();
        if (q.isEmpty()) return List.of();

        String sql = """
            SELECT l.id, l.titolo, l.anno, l.editore, l.categoria,
                   COALESCE(
                     array_agg(a.autore ORDER BY a.autore) FILTER (WHERE a.autore IS NOT NULL),
                     ARRAY[]::text[]
                   ) AS autori
            FROM br.libri l
            LEFT JOIN br.libri_autori a ON a.libro_id = l.id
            WHERE EXISTS (
                SELECT 1
                FROM br.libri_autori a2
                WHERE a2.libro_id = l.id
                  AND a2.autore ILIKE '%' || ? || '%'
            )
            GROUP BY l.id, l.titolo, l.anno, l.editore, l.categoria
            ORDER BY l.id
            LIMIT ?
            """;

        List<Book> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, q);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapBook(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB searchByAuthorContains: " + e.getMessage(), e);
        }
    }

    /**
     * Cerca libri per anno e autore contenente una substring, con confronto case-insensitive.
     *
     * <p>
     * Il parametro autore viene normalizzato tramite {@code trim}; se nullo o vuoto,
     * il metodo restituisce una lista vuota. I risultati includono gli autori aggregati
     * e sono limitati dal parametro {@code limit}.
     * </p>
     *
     * @param autore substring da ricercare nel nome autore
     * @param anno anno di pubblicazione da filtrare
     * @param limit numero massimo di risultati da restituire
     * @return lista dei libri trovati con autori aggregati
     */
    public List<Book> searchByAuthorAndYear(String autore, int anno, int limit) {
        if (autore == null) return List.of();
        String q = autore.trim();
        if (q.isEmpty()) return List.of();

        String sql = """
            SELECT l.id, l.titolo, l.anno, l.editore, l.categoria,
                   COALESCE(
                     array_agg(a.autore ORDER BY a.autore) FILTER (WHERE a.autore IS NOT NULL),
                     ARRAY[]::text[]
                   ) AS autori
            FROM br.libri l
            LEFT JOIN br.libri_autori a ON a.libro_id = l.id
            WHERE l.anno = ?
              AND EXISTS (
                SELECT 1
                FROM br.libri_autori a2
                WHERE a2.libro_id = l.id
                  AND a2.autore ILIKE '%' || ? || '%'
              )
            GROUP BY l.id, l.titolo, l.anno, l.editore, l.categoria
            ORDER BY l.id
            LIMIT ?
            """;

        List<Book> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, anno);
            ps.setString(2, q);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapBook(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB searchByAuthorAndYear: " + e.getMessage(), e);
        }
    }

    /**
     * Converte una riga del {@link ResultSet} in un oggetto {@link Book}.
     *
     * <p>
     * Estrae i campi del libro e costruisce la lista degli autori a partire dall array SQL
     * prodotto dall’aggregazione nella query, scartando valori null o vuoti.
     * </p>
     *
     * @param rs result set posizionato sulla riga del libro
     * @return istanza di {@link Book} costruita dai dati della riga corrente
     * @throws SQLException in caso di errore di lettura dal result set
     */
    private static Book mapBook(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String titolo = rs.getString("titolo");
        Integer anno = (Integer) rs.getObject("anno");
        String editore = rs.getString("editore");
        String categoria = rs.getString("categoria");

        Array a = rs.getArray("autori");
        List<String> autori = new ArrayList<>();
        if (a != null) {
            String[] arr = (String[]) a.getArray();
            if (arr != null) {
                for (String s : arr) if (s != null && !s.isBlank()) autori.add(s);
            }
        }
        return new Book(id, titolo, autori, anno, editore, categoria);
    }
}
