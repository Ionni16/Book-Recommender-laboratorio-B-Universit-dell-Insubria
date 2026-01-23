package bookrecommender.server;

import bookrecommender.db.Db;
import bookrecommender.model.Book;
import bookrecommender.model.Library;
import bookrecommender.model.Review;
import bookrecommender.model.Suggestion;
import bookrecommender.model.User;
import bookrecommender.net.Request;
import bookrecommender.net.RequestType;
import bookrecommender.net.Response;
import bookrecommender.repo.ConsigliRepository;
import bookrecommender.repo.LibrerieRepository;
import bookrecommender.repo.LibriRepository;
import bookrecommender.repo.UtentiRepository;
import bookrecommender.repo.ValutazioniRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.SearchService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point del server del sistema Book Recommender basato su socket.
 *
 * <p>
 * La classe avvia un {@link ServerSocket} su una porta configurata e gestisce richieste
 * provenienti dai client tramite stream di oggetti. All’avvio legge da input i parametri
 * di connessione a PostgreSQL, istanzia repository e servizi applicativi e, per ogni
 * connessione, delega la gestione a un thread dedicato che interpreta {@link RequestType}
 * e produce una {@link Response}.
 * </p>
 *
 * <ul>
 *   <li>Inizializzazione della connessione al database e dei componenti applicativi</li>
 *   <li>Accettazione di connessioni client e gestione concorrente tramite thread</li>
 *   <li>Dispatch delle richieste (ricerca, autenticazione, librerie, consigli, valutazioni)</li>
 * </ul>
 *
 * @author Richard Zefi
 * @version 1.0
 * @see ServerSocket
 * @see Request
 * @see Response
 */
public class MainServer {

    /**
     * Avvia il server: acquisisce i parametri del database, inizializza repository/servizi
     * e apre la socket di ascolto sulla porta configurata.
     *
     * <p>
     * Per ogni connessione accettata viene creato un thread che delega la gestione
     * a {@link #handleClient(Socket, LibriRepository, SearchService, AuthService, ValutazioniRepository, UtentiRepository, ConsigliRepository, LibrerieRepository)}.
     * </p>
     *
     * @param args argomenti da riga di comando (non utilizzati)
     * @throws Exception in caso di errori non gestiti durante l’avvio
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws Exception {
        int port = 5050;

        /* =======================
           LETTURA PARAMETRI DB
           ======================= */
        try (Scanner sc = new Scanner(System.in)) {

            System.out.print("Host DB (es. localhost): ");
            String host = sc.nextLine().trim();

            System.out.print("Porta DB (default 5432): ");
            String p = sc.nextLine().trim();
            String dbPort = p.isEmpty() ? "5432" : p;

            System.out.print("Nome DB (es. bookrecommender): ");
            String dbName = sc.nextLine().trim();

            System.out.print("User DB: ");
            String user = sc.nextLine().trim();

            System.out.print("Password DB: ");
            String pass = sc.nextLine(); // non trim

            // IMPORTANT: usa lo schema br
            String url = "jdbc:postgresql://" + host + ":" + dbPort + "/" + dbName + "?currentSchema=br";

            Db db = new Db(url, user, pass);

            /* =======================
               REPOSITORY + SERVICES
               ======================= */
            LibriRepository libriRepo = new LibriRepository(db);
            SearchService searchService = new SearchService(libriRepo);

            UtentiRepository utentiRepo = new UtentiRepository(db);
            AuthService authService = new AuthService(utentiRepo);

            ValutazioniRepository valutazioniRepo = new ValutazioniRepository(db);
            ConsigliRepository consigliRepo = new ConsigliRepository(db);
            LibrerieRepository librerieRepo = new LibrerieRepository(db);

            System.out.println("Utenti nel DB: " + utentiRepo.count());

            /* =======================
               SERVER SOCKET
               ======================= */
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Server avviato su porta " + port);

                while (true) {
                    Socket client = serverSocket.accept();

                    new Thread(() -> handleClient(
                            client,
                            libriRepo,
                            searchService,
                            authService,
                            valutazioniRepo,
                            utentiRepo,
                            consigliRepo,
                            librerieRepo
                    )).start();
                }
            }
        }
    }

    /**
     * Gestisce una connessione client interpretando una richiesta e restituendo una risposta.
     *
     * <p>
     * Legge un oggetto dallo stream di input e verifica che sia una {@link Request}.
     * In base al {@link RequestType} effettua il dispatch verso repository e servizi,
     * applicando i vincoli applicativi previsti (ad esempio: valutazioni e consigli
     * consentiti solo per libri presenti in almeno una libreria dell’utente).
     * La risposta viene inviata come {@link Response} tramite stream di output.
     * </p>
     *
     * @param client socket del client connesso
     * @param libriRepo repository dei libri
     * @param searchService servizio di ricerca libri
     * @param authService servizio di autenticazione e gestione credenziali
     * @param valutazioniRepo repository delle valutazioni
     * @param utentiRepo repository degli utenti registrati
     * @param consigliRepo repository dei consigli
     * @param librerieRepo repository delle librerie
     */
    private static void handleClient(
            Socket client,
            LibriRepository libriRepo,
            SearchService searchService,
            AuthService authService,
            ValutazioniRepository valutazioniRepo,
            UtentiRepository utentiRepo,
            ConsigliRepository consigliRepo,
            LibrerieRepository librerieRepo
    ) {
        try (client;
             ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {

            Object obj = in.readObject();
            if (!(obj instanceof Request req)) {
                out.writeObject(Response.fail("Bad request"));
                out.flush();
                return;
            }

            Response resp = switch (req.type) {

                case PING -> Response.ok("PONG");

                /* =======================
                   RICERCHE
                   ======================= */
                case SEARCH_BY_TITLE -> {
                    String titolo = (String) req.payload;
                    List<Book> result = searchService.cercaLibroPerTitolo(titolo, 50);
                    yield Response.ok(result);
                }

                case SEARCH_BY_AUTHOR -> {
                    String autore = (String) req.payload;
                    List<Book> result = searchService.cercaLibroPerAutore(autore, 50);
                    yield Response.ok(result);
                }

                case DELETE_ACCOUNT -> {
                    String userid = (String) req.payload;
                    userid = (userid == null) ? "" : userid.trim();

                    if (userid.isBlank()) yield Response.fail("Userid mancante");

                    boolean ok = utentiRepo.deleteAccountCascade(userid);
                    yield ok ? Response.ok(true) : Response.fail("Account non eliminato (utente non trovato?)");
                }

                /* =======================
                   LOGIN / REGISTER
                   ======================= */
                case LOGIN -> {
                    String[] p = (String[]) req.payload;
                    String userid = (p[0] == null) ? "" : p[0].trim();
                    String pass = (p[1] == null) ? "" : p[1].trim();

                    User u = authService.login(userid, pass);
                    yield (u == null)
                            ? Response.fail("Credenziali non valide")
                            : Response.ok(u);
                }

                case REGISTER -> {
                    User u = (User) req.payload;
                    boolean ok = authService.registrazione(u);
                    yield ok
                            ? Response.ok(true)
                            : Response.fail("Userid già esistente o dati non validi");
                }

                case LOGOUT -> Response.ok(true);

                /* =======================
                   VALUTAZIONI
                   ======================= */
                case SAVE_REVIEW -> {
                    Review r = (Review) req.payload;

                    if (r == null) yield Response.fail("Review null");

                    // vincolo specifica: puoi valutare solo libri presenti in almeno una tua libreria
                    if (!librerieRepo.userHasBook(r.getUserid(), r.getBookId())) {
                        yield Response.fail("Puoi valutare solo libri presenti in una tua libreria");
                    }

                    valutazioniRepo.upsert(r);
                    yield Response.ok(true);
                }

                case GET_REVIEWS_BY_BOOK -> {
                    int bookId = (Integer) req.payload;
                    List<Review> reviews = valutazioniRepo.findByBookId(bookId);
                    yield Response.ok(reviews);
                }

                // payload: String userid
                case LIST_REVIEWS_BY_USER -> {
                    String userid = (String) req.payload;
                    yield Response.ok(valutazioniRepo.findByUserId(userid));
                }

                case UPDATE_EMAIL -> {
                    Object[] p = (Object[]) req.payload;
                    String userid = ((String) p[0]).trim();
                    String newEmail = ((String) p[1]).trim();
                    boolean ok = utentiRepo.updateEmail(userid, newEmail);
                    yield ok ? Response.ok(true) : Response.fail("Email non aggiornata");
                }

                case SEARCH_BY_AUTHOR_YEAR -> {
                    Object[] p = (Object[]) req.payload; // { String autore, Integer anno, (Integer limit)? }

                    String autore = (String) p[0];
                    int anno = (Integer) p[1];
                    int limit = (p.length >= 3 && p[2] != null) ? (Integer) p[2] : 50;

                    List<Book> result = searchService.cercaLibroPerAutoreEAnno(autore, anno, limit);
                    yield Response.ok(result);
                }

                case DELETE_REVIEW -> {
                    Object[] p = (Object[]) req.payload; // payload: { String userid, Integer bookId }
                    String userid = (String) p[0];
                    int bookId = (Integer) p[1];

                    boolean ok = valutazioniRepo.delete(userid, bookId);
                    yield ok ? Response.ok(true) : Response.fail("Recensione non trovata o non eliminabile");
                }

                /* =======================
                   CONSIGLI
                   ======================= */
                case GET_SUGGESTIONS_BY_BOOK -> {
                    int bookId = (Integer) req.payload;
                    List<Suggestion> s = consigliRepo.findByBookId(bookId);
                    yield Response.ok(s);
                }

                // payload: Object[] { String userid, Integer bookId, Integer suggestedId }
                case SAVE_SUGGESTION -> {
                    Suggestion s = (Suggestion) req.payload;

                    if (s == null) yield Response.fail("Suggestion null");

                    String userid = s.getUserid();
                    int bookId = s.getBookId();
                    List<Integer> suggested = s.getSuggeriti();

                    // normalizza: unici, max 3, niente bookId uguale
                    LinkedHashSet<Integer> set = new LinkedHashSet<>();
                    for (Integer id : suggested) {
                        if (id == null) continue;
                        if (id == bookId) continue;
                        set.add(id);
                        if (set.size() >= 3) break;
                    }

                    if (set.isEmpty()) yield Response.fail("Devi selezionare almeno 1 libro da consigliare");
                    if (set.size() > 3) yield Response.fail("Massimo 3 consigli");

                    // vincolo: puoi consigliare solo se il libro base è in almeno una tua libreria
                    if (!librerieRepo.userHasBook(userid, bookId)) {
                        yield Response.fail("Puoi consigliare solo libri presenti in una tua libreria");
                    }

                    for (int sid : set) {
                        if (!librerieRepo.userHasBook(userid, sid)) {
                            yield Response.fail("Puoi consigliare solo libri presenti nelle tue librerie (ID " + sid + ")");
                        }
                    }

                    boolean ok = consigliRepo.replaceSuggestionsMax3(userid, bookId, new ArrayList<>(set));
                    yield ok ? Response.ok(true) : Response.fail("Salvataggio consiglio fallito");
                }

                // payload: Object[] { String userid, Integer bookId, Integer suggestedId }
                case DELETE_SUGGESTION -> {
                    Object[] p = (Object[]) req.payload; // { String userid, Integer bookId }
                    String userid = (String) p[0];
                    int bookId = (Integer) p[1];

                    consigliRepo.deleteAllForUserBook(userid, bookId);
                    yield Response.ok(true);
                }

                // payload: String userid
                case LIST_SUGGESTIONS_BY_USER -> {
                    String userid = (String) req.payload;
                    yield Response.ok(consigliRepo.findByUserId(userid));
                }

                /* =======================
                   LIBRERIE
                   ======================= */
                // payload: String userid
                case LIST_LIBRARIES_BY_USER -> {
                    String userid = (String) req.payload;
                    yield Response.ok(librerieRepo.findByUserId(userid));
                }

                case GET_BOOK_BY_ID -> {
                    Object[] p = (Object[]) req.payload;
                    int bookId = (Integer) p[0];

                    Book b = libriRepo.findById(bookId); // repo JDBC lato server
                    yield Response.ok(b); // se null va bene, client gestisce
                }

                // payload: Library
                case SAVE_LIBRARY -> {
                    Library lib = (Library) req.payload;
                    librerieRepo.saveLibrary(lib);
                    yield Response.ok(true);
                }

                // payload: Object[] { String userid, String nome }
                case DELETE_LIBRARY -> {
                    Object[] p = (Object[]) req.payload;
                    String userid = (String) p[0];
                    String nome = (String) p[1];
                    librerieRepo.deleteLibrary(userid, nome);
                    yield Response.ok(true);
                }

                case CHANGE_PASSWORD -> {
                    Object[] p = (Object[]) req.payload;
                    String userid = (String) p[0];
                    String newPass = (String) p[1];
                    boolean ok = authService.changePassword(userid, newPass);
                    yield ok ? Response.ok(true) : Response.fail("Cambio password fallito");
                }

                case RENAME_LIBRARY -> {
                    Object[] a = (Object[]) req.payload;
                    String userid = (String) a[0];
                    String oldName = (String) a[1];
                    String newName = (String) a[2];

                    boolean ok = librerieRepo.renameLibrary(userid, oldName, newName);
                    yield Response.ok(ok);
                }
            };

            out.writeObject(resp);
            out.flush();

        } catch (Exception e) {
            System.err.println(e.getMessage());

        }
    }
}
