package bookrecommender.repo;

import bookrecommender.db.Db;
import bookrecommender.model.Library;

import java.sql.*;
import java.util.*;

public class LibrerieRepository {

    private final Db db;

    public LibrerieRepository(Db db) {
        this.db = db;
    }

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

    /** Salva la libreria: sovrascrive i libri contenuti (delete+insert) */
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
