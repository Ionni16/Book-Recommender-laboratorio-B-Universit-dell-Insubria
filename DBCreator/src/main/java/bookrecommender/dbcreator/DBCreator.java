package bookrecommender.dbcreator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class DBCreator {

    private static final int NUM_USERS = 100;

    // ====== FOCUS SETTINGS ======
    private static final int NUM_FOCUS_BOOKS = 3;              // 2 o 3: qui 3
    private static final int FOCUS_REVIEWS_PER_BOOK = 70;      // >= 50
    private static final int FOCUS_SUGGESTIONS_PER_BOOK = 30;  // >= 20
    private static final int FOCUS_USERS = 80;                 // quanti utenti “caricano” i libri focus nelle librerie
    private static final int FOCUS_LIBRARY_EXTRA_BOOKS = 40;   // quanti altri libri metto nella libreria "Preferiti" dei focus users

    private static final List<String> LIB_NAMES = List.of(
            "Preferiti", "Da leggere", "Consigliati", "Letti", "Wishlist", "Classici", "Fantasy", "Thriller"
    );

    // commento max 256
    private static final List<String> COMMENTS = List.of(
            "Scorrevole e coinvolgente.",
            "Buona storia, ma ritmo altalenante.",
            "Personaggi ben costruiti.",
            "Non mi ha convinto fino in fondo.",
            "Finale molto soddisfacente.",
            "Ottimo stile, lo consiglio.",
            "Carino, ma mi aspettavo di più.",
            "Atmosfera pazzesca, letto in due giorni.",
            "Idea interessante, esecuzione migliorabile.",
            "Mi ha lasciato qualcosa."
    );

    private static final String[] FIRST_NAMES = {
            "Luca","Marco","Giulia","Sara","Paolo","Francesca","Matteo","Elena","Davide","Chiara",
            "Simone","Valentina","Andrea","Martina","Alessandro","Federica","Giorgio","Ilaria","Stefano","Laura"
    };
    private static final String[] LAST_NAMES = {
            "Rossi","Bianchi","Verdi","Russo","Ferrari","Esposito","Romano","Gallo","Costa","Fontana",
            "Conti","Marino","Greco","Lombardi","Mancini","Barbieri","Moretti","Giordano","Santoro","Rizzo"
    };

    public static void main(String[] args) {

        try (Scanner sc = new Scanner(System.in)) {

            Class.forName("org.postgresql.Driver");

            System.out.print("DB url (jdbc:postgresql://localhost:5432/bookrecommender?currentSchema=br):");
            String url = sc.nextLine().trim();
            if (url.isBlank()) throw new IllegalArgumentException("URL JDBC vuoto. Devi inserirlo.");

            System.out.print("DB user: ");
            String user = sc.nextLine().trim();
            if (user.isBlank()) throw new IllegalArgumentException("User DB vuoto.");

            System.out.print("DB password: ");
            String password = sc.nextLine(); // può essere vuota

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                boolean reset = Arrays.asList(args).contains("--reset");

                conn.setAutoCommit(false);
                try {
                    if (reset) runSql(conn, "data/0-reset.sql");

                    runSql(conn, "data/1-schema.sql");

                    if (isSchemaEmpty(conn)) {
                        runSql(conn, "data/2-seed.sql"); // libri + autori dal dataset
                    } else {
                        System.out.println("DB già popolato (libri presenti): skip 2-seed.sql");
                    }

                    seedExtraIfNeeded(conn);

                    conn.commit();
                    System.out.println("✅ Tutto completato.");
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
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
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;

                current.append(line).append('\n');

                if (trimmed.endsWith(";")) {
                    String stmt = current.toString().trim();
                    current.setLength(0);

                    if (stmt.isEmpty()) continue;

                    st.execute(stmt);
                    executed++;

                    if (executed % 1000 == 0) {
                        System.out.println("... eseguiti " + executed + " statement da " + resourcePath);
                    }
                }
            }

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

    // =========================
    //   EXTRA SEED (JDBC)
    // =========================

    private static void seedExtraIfNeeded(Connection conn) throws Exception {
        int bookCount = count(conn, "SELECT COUNT(*) FROM libri");
        if (bookCount == 0) {
            System.out.println("⚠️ Nessun libro presente: skip seed extra.");
            return;
        }

        int userCount = count(conn, "SELECT COUNT(*) FROM utenti_registrati");
        if (userCount > 0) {
            System.out.println("DB già popolato (utenti presenti): skip seed utenti/recensioni/consigli/librerie");
            return;
        }

        System.out.println("➡️ Popolo utenti/recensioni/consigli/librerie...");

        List<Integer> bookIds = loadBookIds(conn);
        List<User> users = generateUsers(NUM_USERS);

        // scegli 2/3 libri focus (qui 3): primi ID disponibili
        List<Integer> focusBookIds = bookIds.subList(0, Math.min(NUM_FOCUS_BOOKS, bookIds.size()));
        System.out.println("🎯 Libri focus: " + focusBookIds);

        truncateDynamicTables(conn);

        insertUsers(conn, users);
        insertLibrariesAndBooks(conn, users, bookIds, focusBookIds);
        insertRatings(conn, users, bookIds, focusBookIds);
        insertSuggestions(conn, users, bookIds, focusBookIds);

        System.out.println("✅ Seed extra completato.");
    }

    private static int count(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static List<Integer> loadBookIds(Connection conn) throws SQLException {
        List<Integer> ids = new ArrayList<>(8192);
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM libri ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getInt(1));
        }
        System.out.println("📚 Libri trovati: " + ids.size());
        return ids;
    }

    private static void truncateDynamicTables(Connection conn) throws SQLException {
        String sql = """
                TRUNCATE TABLE
                    consigli_libri,
                    librerie_libri,
                    librerie,
                    valutazioni_libri,
                    utenti_registrati
                RESTART IDENTITY CASCADE
                """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
        System.out.println("🧹 Tabelle dinamiche troncate (extra seed).");
    }

    private static void insertUsers(Connection conn, List<User> users) throws Exception {
        String sql = """
                INSERT INTO utenti_registrati
                    (userid, password_hash, nome, cognome, codice_fiscale, email)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (User u : users) {
                ps.setString(1, u.userid);
                ps.setString(2, u.passwordHash);
                ps.setString(3, u.nome);
                ps.setString(4, u.cognome);
                ps.setString(5, u.codiceFiscale);
                ps.setString(6, u.email);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        System.out.println("👤 Utenti inseriti: " + users.size());
    }

    // Librerie:
    // - per i primi FOCUS_USERS utenti: crea sicuramente "Preferiti", ci mette tutti i focusBookIds + tanti altri libri
    // - per gli altri utenti: comportamento simile a prima (random)
    private static void insertLibrariesAndBooks(Connection conn, List<User> users, List<Integer> bookIds, List<Integer> focusBookIds) throws SQLException {
        String insLib = "INSERT INTO librerie (userid, nome) VALUES (?, ?)";
        String insLibBook = "INSERT INTO librerie_libri (userid, nome, libro_id) VALUES (?, ?, ?)";

        int focusNUsers = Math.min(FOCUS_USERS, users.size());

        try (PreparedStatement psLib = conn.prepareStatement(insLib);
             PreparedStatement psLibBook = conn.prepareStatement(insLibBook)) {

            // --- Focus users ---
            for (int i = 0; i < focusNUsers; i++) {
                User u = users.get(i);

                // garantisco "Preferiti"
                String fav = "Preferiti";
                psLib.setString(1, u.userid);
                psLib.setString(2, fav);
                psLib.addBatch();

                // dentro: tutti i libri focus + tanti altri
                Set<Integer> chosen = new HashSet<>();
                chosen.addAll(focusBookIds);
                chosen.addAll(pickDistinctIntsExcluding(bookIds, FOCUS_LIBRARY_EXTRA_BOOKS, chosen));

                for (int bookId : chosen) {
                    psLibBook.setString(1, u.userid);
                    psLibBook.setString(2, fav);
                    psLibBook.setInt(3, bookId);
                    psLibBook.addBatch();
                }

                // + 1..2 librerie extra random
                int extraLib = randInt(1, 2);
                List<String> extraNames = pickDistinct(LIB_NAMES, extraLib);
                for (String libName : extraNames) {
                    if (libName.equals(fav)) continue;

                    psLib.setString(1, u.userid);
                    psLib.setString(2, libName);
                    psLib.addBatch();

                    int numBooks = randInt(5, 15);
                    List<Integer> moreBooks = pickDistinctInts(bookIds, numBooks);
                    for (int bookId : moreBooks) {
                        psLibBook.setString(1, u.userid);
                        psLibBook.setString(2, libName);
                        psLibBook.setInt(3, bookId);
                        psLibBook.addBatch();
                    }
                }
            }

            // --- Other users (random as before) ---
            for (int i = focusNUsers; i < users.size(); i++) {
                User u = users.get(i);

                int numLib = randInt(1, 3);
                List<String> picked = pickDistinct(LIB_NAMES, numLib);

                for (String libName : picked) {
                    psLib.setString(1, u.userid);
                    psLib.setString(2, libName);
                    psLib.addBatch();

                    int numBooks = randInt(5, 15);
                    List<Integer> chosenBooks = pickDistinctInts(bookIds, numBooks);
                    for (int bookId : chosenBooks) {
                        psLibBook.setString(1, u.userid);
                        psLibBook.setString(2, libName);
                        psLibBook.setInt(3, bookId);
                        psLibBook.addBatch();
                    }
                }
            }

            psLib.executeBatch();
            psLibBook.executeBatch();
        }

        System.out.println("📦 Librerie + libri in libreria inseriti (focus incluso).");
    }

    // Recensioni:
    // - per ogni focus book: almeno FOCUS_REVIEWS_PER_BOOK recensioni da utenti distinti
    // - poi recensioni random come prima (evitando duplicati PK)
    private static void insertRatings(Connection conn, List<User> users, List<Integer> bookIds, List<Integer> focusBookIds) throws SQLException {
        String sql = """
                INSERT INTO valutazioni_libri
                    (userid, libro_id, stile, contenuto, gradevolezza, originalita, edizione, voto_finale, commento)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1) Focus reviews garantite
            int maxUsers = users.size();
            int perFocus = Math.min(FOCUS_REVIEWS_PER_BOOK, maxUsers);

            // per evitare duplicati PK, teniamo traccia (userid->set libro_id recensiti)
            Map<String, Set<Integer>> reviewed = new HashMap<>(users.size() * 2);
            for (User u : users) reviewed.put(u.userid, new HashSet<>());

            for (int focusBookId : focusBookIds) {
                for (int i = 0; i < perFocus; i++) {
                    User u = users.get(i);
                    addRatingBatch(ps, u.userid, focusBookId);
                    reviewed.get(u.userid).add(focusBookId);
                }
            }

            // 2) Random reviews (come prima), evitando di recensire 2 volte stesso libro dallo stesso utente
            for (User u : users) {
                int n = randInt(3, 12);
                List<Integer> chosenBooks = pickDistinctInts(bookIds, n);

                for (int bookId : chosenBooks) {
                    if (reviewed.get(u.userid).contains(bookId)) continue; // evita PK clash
                    addRatingBatch(ps, u.userid, bookId);
                    reviewed.get(u.userid).add(bookId);
                }
            }

            ps.executeBatch();
        }

        System.out.println("⭐ Valutazioni inserite (focus books stressati).");
    }

    private static void addRatingBatch(PreparedStatement ps, String userid, int bookId) throws SQLException {
        int stile = randInt(1,5);
        int contenuto = randInt(1,5);
        int gradevolezza = randInt(1,5);
        int originalita = randInt(1,5);
        int edizione = randInt(1,5);

        int votoFinale = (int) Math.round((stile + contenuto + gradevolezza + originalita + edizione) / 5.0);
        String commento = COMMENTS.get(randInt(0, COMMENTS.size() - 1));

        ps.setString(1, userid);
        ps.setInt(2, bookId);
        ps.setInt(3, stile);
        ps.setInt(4, contenuto);
        ps.setInt(5, gradevolezza);
        ps.setInt(6, originalita);
        ps.setInt(7, edizione);
        ps.setInt(8, votoFinale);
        ps.setString(9, commento);
        ps.addBatch();
    }

    // Consigli:
    // - per ogni focus book: almeno FOCUS_SUGGESTIONS_PER_BOOK consigli (base = focus)
    // - poi consigli random come prima
    private static void insertSuggestions(Connection conn, List<User> users, List<Integer> bookIds, List<Integer> focusBookIds) throws SQLException {
        String sql = "INSERT INTO consigli_libri (userid, libro_id, suggerito_id) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1) Focus suggestions garantiti
            for (int focusBookId : focusBookIds) {
                // scegli suggeriti distinti, diversi dal focus
                List<Integer> candidates = new ArrayList<>(bookIds);
                candidates.remove((Integer) focusBookId);
                Collections.shuffle(candidates);

                int k = Math.min(FOCUS_SUGGESTIONS_PER_BOOK, candidates.size());
                List<Integer> sugList = candidates.subList(0, k);

                // distribuisci su utenti (anche lo stesso utente può dare più consigli, va bene)
                for (int i = 0; i < sugList.size(); i++) {
                    User u = users.get(i % users.size());
                    int sug = sugList.get(i);

                    ps.setString(1, u.userid);
                    ps.setInt(2, focusBookId);
                    ps.setInt(3, sug);
                    ps.addBatch();
                }
            }

            // 2) Consigli random come prima (evito duplicati per utente)
            for (User u : users) {
                int n = randInt(5, 15);
                Set<String> used = new HashSet<>(n * 2);

                for (int i = 0; i < n; i++) {
                    int base = bookIds.get(randInt(0, bookIds.size() - 1));
                    int sug;
                    do {
                        sug = bookIds.get(randInt(0, bookIds.size() - 1));
                    } while (sug == base);

                    String key = base + "->" + sug;
                    if (!used.add(key)) continue;

                    ps.setString(1, u.userid);
                    ps.setInt(2, base);
                    ps.setInt(3, sug);
                    ps.addBatch();
                }
            }

            ps.executeBatch();
        }

        System.out.println("💡 Consigli inseriti (focus books stressati).");
    }

    // =========================
    //   GENERAZIONE DATI
    // =========================

    private static List<User> generateUsers(int count) throws Exception {
        List<User> users = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String userid = String.format("user%03d", i);

            String nome = FIRST_NAMES[randInt(0, FIRST_NAMES.length - 1)];
            String cognome = LAST_NAMES[randInt(0, LAST_NAMES.length - 1)];

            String email = userid + "@example.com";
            String cf = fakeCodiceFiscale();

            // 64 char -> CHECK OK
            String passwordHash = sha256Hex("Password123!" + userid);

            users.add(new User(userid, passwordHash, nome, cognome, cf, email));
        }
        return users;
    }

    private static int randInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static <T> List<T> pickDistinct(List<T> source, int howMany) {
        if (howMany >= source.size()) return new ArrayList<>(source);
        List<T> copy = new ArrayList<>(source);
        Collections.shuffle(copy);
        return copy.subList(0, howMany);
    }

    private static List<Integer> pickDistinctInts(List<Integer> source, int howMany) {
        if (howMany >= source.size()) return new ArrayList<>(source);
        List<Integer> copy = new ArrayList<>(source);
        Collections.shuffle(copy);
        return copy.subList(0, howMany);
    }

    private static Collection<Integer> pickDistinctIntsExcluding(List<Integer> source, int howMany, Set<Integer> exclude) {
        List<Integer> copy = new ArrayList<>(source.size());
        for (Integer x : source) if (!exclude.contains(x)) copy.add(x);
        Collections.shuffle(copy);
        int k = Math.min(howMany, copy.size());
        return copy.subList(0, k);
    }

    private static String fakeCodiceFiscale() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(alphabet.charAt(randInt(0, alphabet.length() - 1)));
        }
        return sb.toString();
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return toHex(dig);
    }

    private static String toHex(byte[] bytes) {
        char[] hex = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(out);
    }

    private record User(String userid, String passwordHash, String nome, String cognome, String codiceFiscale, String email) {}
}
