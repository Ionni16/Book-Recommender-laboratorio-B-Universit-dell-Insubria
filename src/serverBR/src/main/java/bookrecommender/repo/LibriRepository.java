package bookrecommender.repo;

import bookrecommender.db.Db;
import bookrecommender.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository JDBC per l'accesso ai libri su PostgreSQL.
 * Ritorna sempre i libri con lista autori aggregata.
 */
public class LibriRepository {

    private final Db db;

    public LibriRepository(Db db) {
        this.db = db;
    }

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

    public List<Book> listFirst(int limit) {
        String sql = """
            SELECT l.id, l.titolo, l.anno, l.editore, l.categoria,
                   COALESCE(
                     array_agg(a.autore ORDER BY a.autore) FILTER (WHERE a.autore IS NOT NULL),
                     ARRAY[]::text[]
                   ) AS autori
            FROM br.libri l
            LEFT JOIN br.libri_autori a ON a.libro_id = l.id
            GROUP BY l.id, l.titolo, l.anno, l.editore, l.categoria
            ORDER BY l.id
            LIMIT ?
            """;

        List<Book> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapBook(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Errore DB listFirst: " + e.getMessage(), e);
        }
    }

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
