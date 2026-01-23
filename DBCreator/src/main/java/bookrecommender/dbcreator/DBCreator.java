package bookrecommender.dbcreator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;

public class DBCreator {

    public static void main(String[] args) {

        try (Scanner sc = new Scanner(System.in)) {

            // Forza il load del driver (utile se l'ambiente è strano)
            Class.forName("org.postgresql.Driver");

            System.out.print("DB url (jdbc:postgresql://localhost:5432/bookrecommender?currentSchema=br):");
            String url = sc.nextLine().trim();
            if (url.isBlank()) {
                throw new IllegalArgumentException("URL JDBC vuoto. Devi inserirlo.");
            }

            System.out.print("DB user: ");
            String user = sc.nextLine().trim();
            if (user.isBlank()) {
                throw new IllegalArgumentException("User DB vuoto.");
            }

            System.out.print("DB password: ");
            String password = sc.nextLine(); // può essere vuota

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                boolean reset = Arrays.asList(args).contains("--reset");

                if (reset) {
                    runSql(conn, "data/0-reset.sql");
                }

                runSql(conn, "data/1-schema.sql");

                if (isSchemaEmpty(conn)) {
                    runSql(conn, "data/2-seed.sql");
                } else {
                    System.out.println("DB già popolato: skip 2-seed.sql");
                }

            }

        } catch (Exception e) {
            System.err.println("\nERRORE: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runSql(Connection conn, String resourcePath) throws Exception {
        String sql = readResourceAsString(resourcePath);

        StringBuilder current = new StringBuilder();
        int executed = 0;

        try (Statement st = conn.createStatement()) {
            for (String line : sql.split("\n")) {
                String trimmed = line.trim();

                // salta commenti e vuoti
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;

                current.append(line).append('\n');

                if (trimmed.endsWith(";")) {
                    String stmt = current.toString().trim();
                    current.setLength(0);

                    if (stmt.isEmpty()) continue;

                    st.execute(stmt);
                    executed++;

                    // progress ogni 1000 statement (utile per seed enorme)
                    if (executed % 1000 == 0) {
                        System.out.println("... eseguiti " + executed + " statement da " + resourcePath);
                    }
                }
            }

            // se resta roba senza ';' finale, eseguita comunque
            String tail = current.toString().trim();
            if (!tail.isEmpty()) {
                st.execute(tail);
                executed++;
            }
        } catch (SQLException ex) {
            throw new SQLException(
                    "Errore eseguendo " + resourcePath +
                            " dopo " + executed + " statement. SQLState=" + ex.getSQLState() +
                            " ErrorCode=" + ex.getErrorCode(),
                    ex
            );
        }

        System.out.println("Eseguito: " + resourcePath + " (statement=" + executed + ")");
    }


    private static String readResourceAsString(String resourcePath) throws Exception {
        InputStream in = DBCreator.class.getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("Resource non trovata nel classpath: " + resourcePath +
                    "\nControlla che sia in DBCreator/src/main/resources/" + resourcePath);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }

    private static boolean isSchemaEmpty(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM libri";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }


}
