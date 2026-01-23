package bookrecommender.ui;

import bookrecommender.model.*;
import bookrecommender.net.Request;
import bookrecommender.net.RequestType;
import bookrecommender.net.Response;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.*;
import bookrecommender.net.BRProxy;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Classe principale dell'interfaccia grafica JavaFX dell'applicazione
 * <code>Book Recommender</code>.
 * <p>
 * Requisiti chiave (Lab B):
 * - Architettura Client/Server con DB PostgreSQL e JDBC lato server
 * - Tabelle DB: Libri, UtentiRegistrati, Librerie, ValutazioniLibri, ConsigliLibri
 * - Ricerche: titolo, autore, autore+anno (case-insensitive, substring)
 * - Valutazioni e consigli solo per libri presenti nelle librerie dell'utente
 *
 * @author Ionut Puiu
 * @version 3.0
 */
public class BookRecommenderFX extends Application {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5050;
    private static final Path IGNORED_PATH = Path.of(".");
    private LibriRepository libriRepo;
    private SearchService searchService;
    private AuthService authService;
    private LibraryService libraryService;
    private ReviewService reviewService;
    private SuggestionService suggestionService;
    private AggregationService aggregationService;

    // Proxy diretto solo per la chiamata SEARCH_BY_AUTHOR_YEAR con limit
    private BRProxy proxy;

    // Cache locale libri visti
    private final Map<Integer, Book> bookCache = new HashMap<>();

    // ---- AppBar buttons ----
    private Button btnLogin;
    private Button btnRegister;
    private Button btnLogout;

    // ---- UI ----
    private Label lblUserBadge;
    private Label lblStatus;

    private ComboBox<SearchMode> cbSearchMode;

    private TextField tfTitle;
    private TextField tfAuthor;
    private Spinner<Integer> spYear;
    private Spinner<Integer> spLimit;
    private CheckBox ckOnlyMyLibraries;

    private TableView<Book> tbl;
    private final ObservableList<Book> data = FXCollections.observableArrayList();

    // detail panel
    private Label dTitle, dAuthors, dMeta, dCategory, dPublisher;
    private Label dAvg;
    private VBox dStarsBox;
    private VBox dSuggestions;

    private Button btnRateThis;
    private Button btnSuggestThis;

    private Book selectedBook; // libro selezionato per azioni rapide

    /**
     * Modalità di ricerca disponibili nell'interfaccia.
     * <ul>
     *   <li><code>TITLE</code>: ricerca per titolo</li>
     *   <li><code>AUTHOR</code>: ricerca per autore</li>
     *   <li><code>AUTHOR_YEAR</code>: ricerca per autore e anno (richiesta dalle specifiche)</li>
     * </ul>
     */
    private enum SearchMode {
        TITLE("Titolo"),
        AUTHOR("Autore"),
        AUTHOR_YEAR("Autore + Anno");

        private final String label;
        SearchMode(String l) {this.label = l;}
        @Override public String toString() { return label; }
    }

    private static final DecimalFormat DF1 = new DecimalFormat("0.0");

    // validazioni registrazione
    private static final Pattern EMAIL_RX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern CF_RX = Pattern.compile("^[A-Za-z0-9]{16}$");


    /**
     * Entry point JavaFX: inizializza proxy/servizi, costruisce la UI e carica risultati iniziali.
     * <p>
     * In particolare:
     * </p>
     * <ul>
     *   <li>crea {@link BRProxy} e I service (auth, search, library, review, suggestion, aggregation)</li>
     *   <li>Costruisce scene e layout principali</li>
     *   <li>Aggancia scorciatoia <code>F5</code> per {@link #refresh(Stage)}</li>
     *   <li>Carica risultati iniziali con {@link #loadInitialResults()}</li>
     * </ul>
     *
     * @param stage stage principale dell'applicazione
     */
    @Override
    public void start(Stage stage) {
        // === Init rete/servizi ===
        proxy = new BRProxy(SERVER_HOST, SERVER_PORT);

        // Repo client NON legge file
        libriRepo = new LibriRepository(IGNORED_PATH); // Path ignorato

        searchService = new SearchService(libriRepo);
        authService = new AuthService(SERVER_HOST, SERVER_PORT);
        libraryService = new LibraryService(IGNORED_PATH); // Path ignorato
        reviewService = new ReviewService(IGNORED_PATH, IGNORED_PATH);
        suggestionService = new SuggestionService(IGNORED_PATH, IGNORED_PATH); // Path ignorati
        aggregationService = new AggregationService(IGNORED_PATH, IGNORED_PATH); // Path ignorati

        StackPane stack = new StackPane();
        BorderPane app = new BorderPane();
        app.getStyleClass().add("app-bg");
        stack.getChildren().add(app);

        StackPane.setAlignment(app, Pos.TOP_LEFT);
        app.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        app.setTop(buildAppBar(stage));
        app.setCenter(buildMain(stage));
        app.setBottom(buildStatusBar());

        Scene scene = new Scene(stack, 1280, 740);

        URL css = getClass().getResource("/bookrecommender/ui/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("CSS NON trovato: /bookrecommender/ui/app.css");
        }



        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F5) {
                e.consume();
                refresh(stage);
            }
        });


        stage.setTitle("Book Recommender");
        stage.setScene(scene);
        stage.show();
        loadInitialResults();


        refreshUserUi();
        lblStatus.setText("Pronto (server " + SERVER_HOST + ":" + SERVER_PORT + ")");
        clearDetail();
    }

    // ---------------- AppBar ----------------
    /**
     * Costruisce la barra superiore (AppBar) con titolo, badge utente e azioni principali.
     * <p>
     * Include i pulsanti:
     * </p>
     * <ul>
     *   <li>Login / registrazione (visibili solo se non autenticato)</li>
     *   <li>Logout (visibile solo se autenticato)</li>
     *   <li>Accesso all'area riservata</li>
     * </ul>
     *
     * @param owner stage proprietario per l'apertura dei dialog
     * @return nodo JavaFX contenente la barra superiore
     * @see #openLogin(Stage)
     * @see #openRegister(Stage)
     * @see #openReservedHome(Stage)
     * @see #refreshUserUi()
     */
    private Node buildAppBar(Stage owner) {
        Label title = new Label("Book Recommender");
        title.getStyleClass().add("title");
        Label sub = new Label("Cerca libri, gestisci librerie, valutazioni e consigli");
        sub.getStyleClass().add("subtitle");

        VBox left = new VBox(2, title, sub);

        lblUserBadge = new Label("Ospite");
        lblUserBadge.getStyleClass().add("badge");

        btnLogin = new Button("Accedi");
        btnLogin.getStyleClass().add("primary");
        btnLogin.setOnAction(e -> openLogin(owner));

        btnRegister = new Button("Registrati");
        btnRegister.setOnAction(e -> openRegister(owner));

        btnLogout = new Button("Logout");
        btnLogout.getStyleClass().add("ghost");
        btnLogout.setOnAction(e -> {
            authService.logout();
            refreshUserUi();
            FxUtil.toast(owner.getScene(), "Logout effettuato");
        });

        Button btnArea = new Button("Area riservata");
        btnArea.setOnAction(e -> {
            openReservedHome(owner);
            refreshUserUi(); // Aggiorna badge/bottoni dopo chiusura finestre
        });


        HBox right = new HBox(10, lblUserBadge, btnArea, btnLogin, btnRegister, btnLogout);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox(20, left, new Pane(), right);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("appbar");
        bar.setPadding(new Insets(18, 18, 14, 18));
        bar.setMinHeight(88);
        bar.setPrefHeight(88);
        bar.setMaxHeight(88);
        return bar;
    }


    /**
     * Aggiorna lo stato della UI in base all'autenticazione corrente.
     * <p>
     * Effetti principali:
     * </p>
     * <ul>
     *   <li>Aggiorna il badge utente (<code>lblUserBadge</code>)</li>
     *   <li>Mostra/nasconde i pulsanti login/registrazione/logout</li>
     *   <li>Abilita/disabilita opzioni che richiedono login (es. <code>ckOnlyMyLibraries</code>)</li>
     *   <li>Abilita/disabilita azioni rapide su libro selezionato (valuta/consiglia)</li>
     * </ul>
     */
    private void refreshUserUi() {
        String u = authService.getCurrentUserid();
        boolean logged = (u != null);

        lblUserBadge.setText(logged ? ("Loggato: " + u) : "Ospite");

        btnLogin.setVisible(!logged);
        btnLogin.setManaged(!logged);
        btnRegister.setVisible(!logged);
        btnRegister.setManaged(!logged);
        btnLogout.setVisible(logged);
        btnLogout.setManaged(logged);

        if (ckOnlyMyLibraries != null) {
            ckOnlyMyLibraries.setDisable(!logged);
            if (!logged) ckOnlyMyLibraries.setSelected(false);
        }

        if (btnRateThis != null) {
            btnRateThis.setDisable(!logged || selectedBook == null);
            btnSuggestThis.setDisable(!logged || selectedBook == null);
            btnRateThis.setText(logged ? "Valuta questo libro" : "Valuta (login richiesto)");
            btnSuggestThis.setText(logged ? "Consiglia libri" : "Consiglia (login richiesto)");
        }
    }

    // ---------------- Main layout ----------------
    /**
     * Costruisce il layout principale a tre colonne:
     * <ul>
     *   <li>Sinistra: pannello ricerca (scrollable)</li>
     *   <li>Centro: tabella risultati</li>
     *   <li>Destra: pannello dettaglio (scrollable)</li>
     * </ul>
     *
     * @param owner stage proprietario
     * @return nodo principale da inserire al centro del {@link BorderPane}
     * @see #buildSearchCard(Stage)
     * @see #buildTableCard(Stage)
     * @see #buildDetailCard(Stage)
     */
    private Node buildMain(Stage owner) {
        VBox searchCard = buildSearchCard(owner);
        VBox tableCard = buildTableCard(owner);
        VBox detailCard = buildDetailCard(owner);

        // Scroll per sinistra/destra
        ScrollPane left = wrapScroll(searchCard);
        ScrollPane right = wrapScroll(detailCard);

        HBox main = new HBox(14, left, tableCard, right);
        main.setPadding(new Insets(14));
        main.setFillHeight(true);

        // larghezze stabili
        left.setMinWidth(320);
        left.setPrefWidth(340);
        left.setMaxWidth(360);

        tableCard.setMinWidth(520);
        tableCard.setPrefWidth(640);
        tableCard.setMaxHeight(Double.MAX_VALUE);

        right.setMinWidth(400);
        right.setPrefWidth(420);
        right.setMaxWidth(460);

        // grow centro
        HBox.setHgrow(left, Priority.NEVER);
        HBox.setHgrow(right, Priority.NEVER);
        HBox.setHgrow(tableCard, Priority.ALWAYS);

        return main;
    }


    /**
     * Costruisce la "card" di ricerca (selezione modalità e filtri).
     * <p>
     * Contiene:
     * </p>
     * <ul>
     *   <li>Selettore modalità ({@link #cbSearchMode})</li>
     *   <li>Campi titolo/autore/anno in base alla modalità</li>
     *   <li>Limite risultati</li>
     *   <li>Opzione ricerca nelle sole librerie dell'utente (richiede login)</li>
     * </ul>
     *
     * @param owner stage proprietario
     * @return card JavaFX con controlli di ricerca
     * @see #applySearchMode(SearchMode)
     * @see #doSearch(Stage)
     */
    private VBox buildSearchCard(Stage owner) {
        Label t = new Label("Cerca un libro");
        t.getStyleClass().add("card-title");

        cbSearchMode = new ComboBox<>();
        cbSearchMode.getItems().setAll(SearchMode.values());
        cbSearchMode.getSelectionModel().select(SearchMode.TITLE);

        tfTitle = new TextField();
        tfTitle.setPromptText("Titolo… (min 2 caratteri)");

        tfAuthor = new TextField();
        tfAuthor.setPromptText("Autore… (min 2 caratteri)");

        spYear = new Spinner<>();
        spYear.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1400, 2100, 2000));
        spYear.setEditable(true);

        spLimit = new Spinner<>();
        spLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 500, 50, 10));
        spLimit.setEditable(true);

        installSpinnerCommit(spYear);
        installSpinnerCommit(spLimit);

        ckOnlyMyLibraries = new CheckBox("Cerca solo nelle mie librerie (login)");
        ckOnlyMyLibraries.setSelected(false);

        cbSearchMode.valueProperty().addListener((ignoreObs, ignoreOld, n) -> applySearchMode(n));
        applySearchMode(cbSearchMode.getValue());

        Button btnSearch = new Button("Cerca");
        btnSearch.getStyleClass().add("primary");
        btnSearch.setMaxWidth(Double.MAX_VALUE);
        btnSearch.setOnAction(e -> doSearch(owner));
        btnSearch.setDefaultButton(true);

        tfTitle.setOnAction(e -> doSearch(owner));
        tfAuthor.setOnAction(e -> doSearch(owner));
        spYear.getEditor().setOnAction(e -> doSearch(owner));

        Button btnClear = new Button("Reset");
        btnClear.getStyleClass().add("ghost");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setOnAction(e -> {
            tfTitle.clear();
            tfAuthor.clear();
            ckOnlyMyLibraries.setSelected(false);
            data.clear();
            bookCache.clear();
            clearDetail();
            lblStatus.setText("Campi resettati");
        });

        VBox box = new VBox(
                10,
                t,
                new Separator(),
                label("Modalità ricerca"), cbSearchMode,
                label("Titolo"), tfTitle,
                label("Autore"), tfAuthor,
                label("Anno"), spYear,
                label("Limite risultati"), spLimit,
                ckOnlyMyLibraries,
                new Separator(),
                btnSearch,
                btnClear
        );
        box.getStyleClass().add("card");

        return box;
    }


    /**
     * Applica la modalità di ricerca selezionata, mostrando/nascondendo i controlli necessari.
     * <p>
     * Regole:
     * </p>
     * <ul>
     *   <li>{@link SearchMode#TITLE}: mostra solo campo titolo</li>
     *   <li>{@link SearchMode#AUTHOR}: mostra solo campo autore</li>
     *   <li>{@link SearchMode#AUTHOR_YEAR}: mostra campo autore + anno</li>
     * </ul>
     *
     * @param mode modalità di ricerca selezionata
     * @see #setVisibleManaged(Control, boolean)
     */
    private void applySearchMode(SearchMode mode) {
        setVisibleManaged(tfTitle, false);
        setVisibleManaged(tfAuthor, false);
        setVisibleManaged(spYear, false);

        if (mode == SearchMode.TITLE) {
            setVisibleManaged(tfTitle, true);
        } else if (mode == SearchMode.AUTHOR) {
            setVisibleManaged(tfAuthor, true);
        } else { // AUTHOR_YEAR
            setVisibleManaged(tfAuthor, true);
            setVisibleManaged(spYear, true);
        }
    }


    /**
     * Imposta visibilità e "managed" su un controllo.
     * <p>
     * In JavaFX, <code>managed=false</code> fa sì che il nodo non occupi spazio nel layout.
     * </p>
     *
     * @param c controllo da aggiornare
     * @param on <code>true</code> per renderlo visibile e gestito dal layout, <code>false</code> per nasconderlo
     */
    private static void setVisibleManaged(Control c, boolean on) {
        c.setVisible(on);
        c.setManaged(on);
    }

    // ---------------- TABLE ----------------
    /**
     * Costruisce la card centrale contenente la tabella dei risultati di ricerca.
     * <p>
     * La tabella visualizza:
     * </p>
     * <ul>
     *   <li>ID libro</li>
     *   <li>titolo</li>
     *   <li>autori</li>
     *   <li>Anno di pubblicazione</li>
     * </ul>
     *
     * <p>
     * Comportamento:
     * </p>
     * <ul>
     *   <li>Selezione singola → aggiorna il pannello dettaglio</li>
     *   <li>Doppio click su una riga → apre il dettaglio del libro</li>
     *   <li>pulsante di refresh manuale (equivalente a F5)</li>
     * </ul>
     *
     * @param owner stage proprietario
     * @return card JavaFX con la tabella dei risultati
     * @see #showDetail(Stage, Book)
     * @see #refresh(Stage)
     */
    private VBox buildTableCard(Stage owner) {
        Label t = new Label("Risultati");
        t.getStyleClass().add("card-title");

        tbl = new TableView<>(data);
        tbl.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tbl.setPlaceholder(new Label("Esegui una ricerca per visualizzare risultati."));

        TableColumn<Book, Integer> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getId()));
        cId.setPrefWidth(70);

        TableColumn<Book, String> cTitle = new TableColumn<>("Titolo");
        cTitle.setCellValueFactory(new PropertyValueFactory<>("titolo"));
        cTitle.setPrefWidth(340);

        TableColumn<Book, String> cAuthor = new TableColumn<>("Autore/i");
        cAuthor.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(
                v.getValue().getAutori() == null ? "" : String.join(", ", v.getValue().getAutori())
        ));
        cAuthor.setPrefWidth(320);

        TableColumn<Book, Integer> cYear = new TableColumn<>("Anno");
        cYear.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getAnno()));
        cYear.setPrefWidth(90);

        cId.setMinWidth(70);
        cId.setMaxWidth(90);
        cTitle.setMinWidth(320);
        cTitle.setMaxWidth(900);
        cAuthor.setMinWidth(260);
        cAuthor.setMaxWidth(800);
        cYear.setMinWidth(80);
        cYear.setMaxWidth(120);

        cYear.setStyle("-fx-alignment: CENTER;");
        cYear.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "" : String.valueOf(item)));
                setAlignment(Pos.CENTER);
            }
        });

        FxUtil.addColumns(tbl, List.of(cId, cTitle, cAuthor, cYear));

        tbl.getSelectionModel().selectedItemProperty().addListener((ignoreObs, ignoreOld, n) -> {
            if (n != null) showDetail(owner, n);
        });

        tbl.setRowFactory(ignoredTv -> {
            TableRow<Book> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) showDetail(owner, row.getItem());
            });
            return row;
        });

        Button btnRefresh = new Button("Ricarica (F5)");
        btnRefresh.getStyleClass().add("ghost");
        btnRefresh.setOnAction(e -> refresh(owner));

        HBox actions = new HBox(10, btnRefresh);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(10, headerRow(t, actions), tbl);
        box.getStyleClass().add("card2");
        VBox.setVgrow(tbl, Priority.ALWAYS);
        return box;
    }

    // ---------------- DETAIL ----------------
    /**
     * Costruisce la card di dettaglio del libro selezionato.
     * <p>
     * Mostra:
     * </p>
     * <ul>
     *   <li>Titolo, autori e metadati (id/anno/categoria/editore)</li>
     *   <li>Aggregazioni recensioni (medie e "stelle") tramite {@link AggregationService}</li>
     *   <li>Aggregazioni consigli tramite {@link AggregationService} con pulsanti rapidi</li>
     *   <li>Azioni: aggiungi a libreria, valuta, consiglia, apri liste personali</li>
     * </ul>
     *
     * <p>
     * Le azioni che richiedono autenticazione vengono abilitate/disabilitate da {@link #refreshUserUi()}.
     * </p>
     *
     * @param owner stage proprietario
     * @return card JavaFX del dettaglio libro
     * @see #showDetail(Stage, Book)
     * @see #clearDetail()
     * @see #refreshUserUi()
     */
    private VBox buildDetailCard(Stage owner) {
        Label t = new Label("Dettaglio libro");
        t.getStyleClass().add("card-title");

        dTitle = new Label("-");
        dTitle.getStyleClass().add("title");
        dTitle.setWrapText(true);

        dAuthors = new Label("-");
        dAuthors.getStyleClass().add("muted");
        dAuthors.setWrapText(true);

        dMeta = new Label("-");
        dMeta.getStyleClass().add("chip");

        dCategory = new Label("-");
        dCategory.getStyleClass().add("chip");

        dPublisher = new Label("-");
        dPublisher.getStyleClass().add("chip");

        FlowPane chips = new FlowPane(10, 10, dMeta, dCategory, dPublisher);
        chips.setPrefWrapLength(360);
        chips.setAlignment(Pos.CENTER_LEFT);

        Label rateT = new Label("Recensioni");
        rateT.getStyleClass().add("card-title");

        dAvg = new Label("-");
        dAvg.getStyleClass().add("muted");
        dAvg.setWrapText(true);

        dStarsBox = new VBox(6);

        Label sugT = new Label("Consigliati");
        sugT.getStyleClass().add("card-title");
        dSuggestions = new VBox(8);

        Button btnAddToLibrary = new Button("Aggiungi alla mia libreria");
        btnAddToLibrary.getStyleClass().add("primary");
        btnAddToLibrary.setMaxWidth(Double.MAX_VALUE);
        btnAddToLibrary.setOnAction(e -> {
            if (selectedBook == null) return;
            if (ensureLoggedIn(owner) == null) return;
            openAddToLibraryDialog(owner, selectedBook);
        });

        btnRateThis = new Button("Valuta questo libro");
        btnRateThis.getStyleClass().add("ghost");
        btnRateThis.setMaxWidth(Double.MAX_VALUE);
        btnRateThis.setOnAction(e -> {
            if (selectedBook == null) return;
            if (ensureLoggedIn(owner) == null) return;
            openReviewEditor(owner, selectedBook);
        });

        btnSuggestThis = new Button("Consiglia libri");
        btnSuggestThis.getStyleClass().add("ghost");
        btnSuggestThis.setMaxWidth(Double.MAX_VALUE);
        btnSuggestThis.setOnAction(e -> {
            if (selectedBook == null) return;
            if (ensureLoggedIn(owner) == null) return;
            openSuggestionEditor(owner, selectedBook);
        });

        Button btnOpenReviewList = new Button("Le mie valutazioni…");
        btnOpenReviewList.getStyleClass().add("ghost");
        btnOpenReviewList.setMaxWidth(Double.MAX_VALUE);
        btnOpenReviewList.setOnAction(e -> {
            if (ensureLoggedIn(owner) == null) return;
            ReviewsWindow.open(authService, reviewService, libriRepo);
        });

        Button btnOpenSugList = new Button("I miei consigli…");
        btnOpenSugList.getStyleClass().add("ghost");
        btnOpenSugList.setMaxWidth(Double.MAX_VALUE);
        btnOpenSugList.setOnAction(e -> {
            if (ensureLoggedIn(owner) == null) return;
            SuggestionsWindow.open(authService, suggestionService, libriRepo);
        });

        VBox box = new VBox(
                10,
                t,
                new Separator(),
                dTitle, dAuthors,
                chips,
                new Separator(),
                rateT, dAvg, dStarsBox,
                new Separator(),
                sugT, dSuggestions,
                new Separator(),
                btnAddToLibrary,
                btnRateThis,
                btnSuggestThis,
                btnOpenReviewList,
                btnOpenSugList
        );
        box.getStyleClass().add("card");

        refreshUserUi();
        return box;
    }


    /**
     * Crea una riga grafica con etichetta + stelle (1..5) + valore numerico formattato.
     *
     * @param label nome dell'aspetto (es. "Stile")
     * @param value0to5 valore medio atteso in range 0..5 (valori fuori range vengono clampati per la UI)
     * @return riga JavaFX contenente testo, stelle e valore formattato
     * @see #DF1
     */
    private static HBox starsRow(String label, double value0to5) {
        int full = (int) Math.round(value0to5);
        full = Math.max(0, Math.min(5, full));

        Label name = new Label(label);
        name.getStyleClass().add("star-label");
        name.setMinWidth(110);

        HBox stars = new HBox(2);
        for (int i = 1; i <= 5; i++) {
            Label s = new Label("★");
            s.getStyleClass().add("star");
            if (i > full) s.getStyleClass().add("off");
            stars.getChildren().add(s);
        }

        Label num = new Label(" " + DF1.format(value0to5));
        num.getStyleClass().add("muted");

        HBox row = new HBox(10, name, stars, num);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }


    /**
     * Costruisce la barra di stato inferiore dell'applicazione.
     * <p>
     * Visualizza:
     * </p>
     * <ul>
     *   <li>Messaggi di stato e feedback operazioni</li>
     *   <li>Scorciatoie da tastiera disponibili</li>
     * </ul>
     *
     * @return nodo JavaFX contenente la status bar
     */
    private Node buildStatusBar() {
        lblStatus = new Label("Pronto");
        lblStatus.getStyleClass().add("muted");

        Label hint = new Label("Invio = Cerca • Doppio click = Dettagli • F5 = Ricarica");
        hint.getStyleClass().add("muted");

        HBox bar = new HBox(14, lblStatus, new Pane(), hint);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("statusbar");
        return bar;
    }

    // ---------------- Search logic ----------------
    /**
     * Esegue una ricerca in base alla modalità selezionata e aggiorna la UI.
     * <p>
     * Flusso principale:
     * </p>
     * <ol>
     *   <li>Valida Input in base alla {@link SearchMode}</li>
     *   <li>Invoca il {@link SearchService} o una richiesta diretta al server</li>
     *   <li>Applica eventuali filtri (solo librerie dell'utente)</li>
     *   <li>Applica il limite risultati lato client</li>
     *   <li>Aggiorna tabella, cache locale e pannello dettaglio</li>
     * </ol>
     *
     * @param owner stage proprietario
     * @see SearchMode
     * @see SearchService
     * @see LibraryService
     */
    private void doSearch(Stage owner) {
        SearchMode mode = cbSearchMode.getValue();
        int limit = spLimit.getValue();

        try {
            List<Book> res;

            if (mode == SearchMode.TITLE) {
                String title = safe(tfTitle.getText());
                if (title.length() < 2) throw new IllegalArgumentException("Inserisci almeno 2 caratteri nel titolo.");
                res = searchService.cercaLibroPerTitolo(title);

            } else if (mode == SearchMode.AUTHOR) {
                String author = safe(tfAuthor.getText());
                if (author.length() < 2) throw new IllegalArgumentException("Inserisci almeno 2 caratteri nell'autore.");
                res = searchService.cercaLibroPerAutore(author);

            } else { // AUTHOR_YEAR (richiesto)
                String author = safe(tfAuthor.getText());
                if (author.length() < 2) throw new IllegalArgumentException("Inserisci almeno 2 caratteri nell'autore.");
                int year = spYear.getValue();

                Request req = new Request(RequestType.SEARCH_BY_AUTHOR_YEAR, new Object[]{author, year, limit}, null);
                Response r = proxy.call(req);
                if (!r.ok) throw new RuntimeException(r.error);
                @SuppressWarnings("unchecked")
                List<Book> tmp = (r.data == null) ? List.of() : (List<Book>) r.data;
                res = tmp;
            }

            // filtro: solo libri nelle mie librerie
            if (ckOnlyMyLibraries.isSelected()) {
                String user = ensureLoggedIn(owner);
                if (user == null) return;

                Set<Integer> myBookIds = libraryService.listUserLibraries(user).stream()
                        .flatMap(l -> l.getBookIds().stream())
                        .collect(Collectors.toSet());

                res = res.stream().filter(b -> myBookIds.contains(b.getId())).collect(Collectors.toList());
            }

            // applica limite anche lato client per robustezza
            res = res.stream().limit(limit).collect(Collectors.toList());

            // aggiorna cache locale libri visti
            for (Book b : res) bookCache.put(b.getId(), b);

            data.setAll(res);
            lblStatus.setText("Risultati: " + res.size());
            clearDetail();
            if (!res.isEmpty()) tbl.getSelectionModel().select(0);

        } catch (Exception ex) {
            FxUtil.error(owner, "Ricerca non valida", ex.getMessage());
        }
    }


    /**
     * Esegue un refresh "soft" dell'applicazione.
     * <p>
     * Non ricarica dati locali, ma verifica la raggiungibilità del server
     * tramite una richiesta di ping e aggiorna la status bar.
     * </p>
     *
     * @param owner stage proprietario
     * @see Request#ping()
     */
    private void refresh(Stage owner) {
        // Non esiste più refresh dataset locale: qui facciamo un ping “soft”
        Response res = proxy.call(Request.ping());
        if (res.ok) {
            FxUtil.toast(owner.getScene(), "Server raggiungibile");
            lblStatus.setText("Server OK");
        } else {
            FxUtil.error(owner, "Errore", "Server non raggiungibile: " + res.error);
            lblStatus.setText("Server NON raggiungibile");
        }
    }

    // ---------------- Detail ----------------
    /**
     * Aggiorna il pannello dettaglio in base al libro selezionato.
     * <p>
     * Operazioni principali:
     * </p>
     * <ul>
     *   <li>Aggiorna riferimento al libro selezionato e la UI correlata</li>
     *   <li>Aggiorna la cache locale <code>bookCache</code></li>
     *   <li>Popola i campi testuali (titolo/autori/metadati)</li>
     *   <li>Richiede al server le statistiche recensioni e consigli tramite {@link AggregationService}</li>
     *   <li>Costruisce i componenti grafici per stelle e lista consigli</li>
     * </ul>
     *
     * <p>
     * I consigli vengono mostrati come pulsanti; il click tenta:
     * </p>
     * <ol>
     *   <li>Selezione del libro se presente nella tabella corrente</li>
     *   <li>Fallback su cache locale</li>
     *   <li>Altrimenti mostra un messaggio informativo</li>
     * </ol>
     *
     * @param owner stage proprietario
     * @param b libro da visualizzare
     * @see AggregationService#getReviewStats(int)
     * @see AggregationService#getSuggestionsStats(int)
     * @see #bookCache
     * @see #data
     */
    private void showDetail(Stage owner, Book b) {
        if (b == null) return;

        selectedBook = b;
        refreshUserUi();

        // aggiorna cache
        bookCache.put(b.getId(), b);

        dTitle.setText(nvl(b.getTitolo(), "-"));
        dAuthors.setText(b.getAutori() == null ? "-" : String.join(", ", b.getAutori()));

        String meta = "ID " + b.getId() + " • " + (b.getAnno() == null ? "Anno n/d" : b.getAnno());
        dMeta.setText(meta);

        dCategory.setText(nvl(b.getCategoria(), "Categoria n/d"));
        dPublisher.setText(nvl(b.getEditore(), "Editore n/d"));

        // === Aggregazioni recensioni (remote) ===
        try {
            AggregationService.ReviewStats rs = aggregationService.getReviewStats(b.getId());
            dStarsBox.getChildren().clear();

            if (rs == null || rs.count == 0) {
                dAvg.setText("Nessuna valutazione disponibile.");
                dStarsBox.getChildren().setAll(labelMuted("Nessuna valutazione disponibile."));
            } else {
                dAvg.setText("Media voto finale: " + DF1.format(rs.mediaVotoFinale) + "  |  N. valutazioni: " + rs.count);
                dStarsBox.getChildren().addAll(
                        starsRow("Stile", rs.mediaStile),
                        starsRow("Contenuto", rs.mediaContenuto),
                        starsRow("Gradevolezza", rs.mediaGradevolezza),
                        starsRow("Originalita", rs.mediaOriginalita),
                        starsRow("Edizione", rs.mediaEdizione)
                );
            }
        } catch (Exception e) {
            dAvg.setText("Errore aggregazione: " + e.getMessage());
            dStarsBox.getChildren().setAll(labelMuted("Errore aggregazione."));
        }

        // === Aggregazioni consigli (remote) ===
        try {
            AggregationService.SuggestionsStats ss = aggregationService.getSuggestionsStats(b.getId());
            dSuggestions.getChildren().clear();

            if (ss == null || ss.suggeritiCount == null || ss.suggeritiCount.isEmpty()) {
                dSuggestions.getChildren().add(labelMuted("Nessun consiglio disponibile."));
            } else {
                int shown = 0;

                for (Map.Entry<Integer, Integer> entry : ss.suggeritiCount.entrySet()) {
                    Integer id = entry.getKey();
                    int count = entry.getValue();

                    Book sb = bookCache.get(id);
                    String label = (sb == null || sb.getTitolo() == null || sb.getTitolo().isBlank())
                            ? ("Libro ID " + id + "  (" + count + ")")
                            : (sb.getTitolo() + "  (" + count + ")");

                    Button link = new Button(label);
                    link.getStyleClass().add("ghost");
                    link.setMaxWidth(Double.MAX_VALUE);

                    link.setOnAction(e -> {
                        // se presente in tabella, seleziona
                        Optional<Book> match = data.stream().filter(x -> x.getId() == id).findFirst();
                        if (match.isPresent()) {
                            tbl.getSelectionModel().select(match.get());
                            return;
                        }
                        // se in cache, mostra dettaglio
                        if (sb != null) {
                            showDetail(owner, sb);
                            return;
                        }
                        FxUtil.toast(owner.getScene(), "Libro non in cache. Esegui una ricerca per visualizzarlo (ID " + id + ").");
                    });

                    dSuggestions.getChildren().add(link);
                    shown++;
                    if (shown >= 5) break;
                }
            }
        } catch (Exception e) {
            dSuggestions.getChildren().setAll(labelMuted("Errore nel caricamento consigli: " + e.getMessage()));
        }
    }


    /**
     * Resetta il pannello dettaglio a uno stato "vuoto" (nessun libro selezionato).
     * <p>
     * Imposta i campi testuali a "-" e ripristina i messaggi placeholder
     * per recensioni e consigli. Aggiorna anche lo stato dei pulsanti tramite
     * {@link #refreshUserUi()}.
     * </p>
     *
     * @see #refreshUserUi()
     */
    private void clearDetail() {
        selectedBook = null;
        refreshUserUi();

        dTitle.setText("-");
        dAuthors.setText("-");
        dMeta.setText("-");
        dCategory.setText("-");
        dPublisher.setText("-");
        dAvg.setText("-");
        if (dStarsBox != null) dStarsBox.getChildren().setAll(labelMuted("Nessuna valutazione disponibile."));
        if (dSuggestions != null) dSuggestions.getChildren().setAll(labelMuted("Seleziona un libro per vedere i dettagli."));
    }

    // ---------------- Librerie ----------------
    /**
     * Apre un dialog che permette di aggiungere un libro a una libreria dell'utente.
     * <p>
     * Funzionalità:
     * </p>
     * <ul>
     *   <li>Carica le librerie dell'utente tramite {@link LibraryService}</li>
     *   <li>Seleziona una libreria esistente e aggiunge l'ID del libro</li>
     *   <li>Consente anche la creazione di una nuova libreria (nome minimo 5 caratteri)</li>
     * </ul>
     *
     * <p>
     * Il metodo richiede che l'utente sia autenticato (tramite {@link #ensureLoggedIn(Stage)}).
     * </p>
     *
     * @param owner stage proprietario
     * @param book libro da aggiungere
     * @see LibraryService#listUserLibraries(String)
     * @see LibraryService#saveLibrary(Library)
     * @see #ensureLoggedIn(Stage)
     */
    private void openAddToLibraryDialog(Stage owner, Book book) {
        String user = ensureLoggedIn(owner);
        if (user == null || book == null) return;

        List<Library> libs;
        try {
            libs = libraryService.listUserLibraries(user);
        } catch (Exception e) {
            FxUtil.error(owner, "Errore", "Impossibile leggere le librerie: " + e.getMessage());
            return;
        }

        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Aggiungi a libreria");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label h = new Label("Aggiungi libro alla tua libreria");
        h.getStyleClass().add("title");
        Label sub = new Label(book.getTitolo());
        sub.getStyleClass().add("subtitle");

        ComboBox<Library> cb = getLibraryComboBox();
        cb.getItems().setAll(libs);
        if (!libs.isEmpty()) cb.getSelectionModel().select(0);

        TextField tfNew = new TextField();
        tfNew.setPromptText("Oppure crea nuova libreria (min 5 caratteri)");

        Button btnCreate = new Button("Crea");
        btnCreate.getStyleClass().add("ghost");

        Button btnAdd = new Button("Aggiungi");
        btnAdd.getStyleClass().add("primary");
        btnAdd.setMaxWidth(Double.MAX_VALUE);

        btnCreate.setOnAction(e -> {
            String name = tfNew.getText() == null ? "" : tfNew.getText().trim();
            if (name.length() < 5) {
                FxUtil.error(owner, "Nome non valido", "Il nome deve avere almeno 5 caratteri.");
                return;
            }
            try {
                Library lib = new Library(user, name, new HashSet<>());
                boolean ok = libraryService.saveLibrary(lib);
                if (!ok) throw new IllegalStateException("Creazione libreria fallita.");
                cb.getItems().setAll(libraryService.listUserLibraries(user));
                cb.getSelectionModel().select(cb.getItems().stream()
                        .filter(x -> x.getNome().equals(name))
                        .findFirst().orElse(null));
                tfNew.clear();
                FxUtil.toast(owner.getScene(), "Libreria creata");
            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        btnAdd.setOnAction(e -> {
            Library sel = cb.getValue();
            if (sel == null) {
                FxUtil.error(owner, "Selezione mancante", "Seleziona una libreria.");
                return;
            }
            try {
                Set<Integer> newSet = new HashSet<>(sel.getBookIds());
                if (newSet.contains(book.getId())) {
                    FxUtil.toast(owner.getScene(), "Libro già presente in \"" + sel.getNome() + "\"");
                    return;
                }
                newSet.add(book.getId());
                Library updated = new Library(sel.getUserid(), sel.getNome(), newSet);

                boolean ok = libraryService.saveLibrary(updated);
                if (!ok) {
                    FxUtil.error(owner, "Operazione non riuscita", "Non posso aggiungere il libro. Controlla i dati.");
                    return;
                }

                FxUtil.toast(owner.getScene(), "Aggiunto a \"" + sel.getNome() + "\"");
                d.close();

            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(label("Libreria"), 0, 0);
        grid.add(cb, 1, 0);

        grid.add(label("Nuova libreria"), 0, 1);
        HBox newRow = new HBox(10, tfNew, btnCreate);
        HBox.setHgrow(tfNew, Priority.ALWAYS);
        grid.add(newRow, 1, 1);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        VBox box = new VBox(12, h, sub, new Separator(), grid, new Separator(), btnAdd);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }


    /**
     * Crea una {@link ComboBox} configurata per visualizzare oggetti {@link Library}.
     * <p>
     * La cella mostra nome libreria e numero di libri, mentre la button-cell mostra solo il nome.
     * </p>
     *
     * @return combo box pronta per essere popolata con librerie
     */
    private static ComboBox<Library> getLibraryComboBox() {
        ComboBox<Library> cb = new ComboBox<>();
        cb.setMaxWidth(Double.MAX_VALUE);

        cb.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(Library item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getNome() + "  (" + item.getBookIds().size() + " libri)");
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Library item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText("Seleziona libreria…");
                else setText(item.getNome());
            }
        });
        return cb;
    }

    // ---------------- Quick actions: Review / Suggestion ----------------
    /**
     * Apre un dialog per inserire o modificare la recensione dell'utente per un libro.
     * <p>
     * Comportamento:
     * </p>
     * <ul>
     *   <li>Richiede login (tramite {@link #ensureLoggedIn(Stage)})</li>
     *   <li>Se esiste già una recensione dell'utente per il libro, apre in modalità modifica</li>
     *   <li>Calcola il voto finale come media (arrotondata) dei 5 aspetti</li>
     *   <li>Salva tramite {@link ReviewService#inserisciValutazione(Review)} o {@link ReviewService#updateReview(Review)}</li>
     *   <li>Aggiorna il pannello dettaglio dopo il salvataggio</li>
     * </ul>
     *
     * @param owner stage proprietario
     * @param book libro da valutare
     * @see ReviewService#listByUser(String)
     * @see ReviewService#inserisciValutazione(Review)
     * @see ReviewService#updateReview(Review)
     * @see #showDetail(Stage, Book)
     */
    private void openReviewEditor(Stage owner, Book book) {
        String user = ensureLoggedIn(owner);
        if (user == null || book == null) return;

        Review found = null;
        try {
            for (Review r : reviewService.listByUser(user)) {
                if (r.getBookId() == book.getId()) {
                    found = r;
                    break;
                }
            }
        } catch (Exception e) {
            FxUtil.error(owner, "Errore", "Impossibile leggere le tue valutazioni: " + e.getMessage());
            return;
        }

        final Review existing = found;
        final boolean editing = (existing != null);

        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle(editing ? "Modifica valutazione" : "Nuova valutazione");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label h = new Label(editing ? "Aggiorna la tua valutazione" : "Valuta questo libro");
        h.getStyleClass().add("title");
        Label sub = new Label(book.getTitolo());
        sub.getStyleClass().add("subtitle");

        Spinner<Integer> sStile = spinner1to5();
        Spinner<Integer> sCont = spinner1to5();
        Spinner<Integer> sGrad = spinner1to5();
        Spinner<Integer> sOrig = spinner1to5();
        Spinner<Integer> sEdiz = spinner1to5();

        TextArea comment = new TextArea();
        comment.setPromptText("Commento (max 256 caratteri) — opzionale");
        comment.setWrapText(true);

        if (editing) {
            sStile.getValueFactory().setValue(existing.getStile());
            sCont.getValueFactory().setValue(existing.getContenuto());
            sGrad.getValueFactory().setValue(existing.getGradevolezza());
            sOrig.getValueFactory().setValue(existing.getOriginalita());
            sEdiz.getValueFactory().setValue(existing.getEdizione());
            comment.setText(existing.getCommento() == null ? "" : existing.getCommento());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int r = 0;
        grid.add(label("Stile (1-5)"), 0, r); grid.add(sStile, 1, r++);
        grid.add(label("Contenuto (1-5)"), 0, r); grid.add(sCont, 1, r++);
        grid.add(label("Gradevolezza (1-5)"), 0, r); grid.add(sGrad, 1, r++);
        grid.add(label("Originalità (1-5)"), 0, r); grid.add(sOrig, 1, r++);
        grid.add(label("Edizione (1-5)"), 0, r); grid.add(sEdiz, 1, r++);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(170);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        Label hint = labelMuted("Nota: puoi valutare un libro solo se è presente in almeno una tua libreria.");

        Button save = new Button(editing ? "Aggiorna valutazione" : "Salva valutazione");
        save.getStyleClass().add("primary");
        save.setMaxWidth(Double.MAX_VALUE);

        save.setOnAction(e -> {
            try {
                String comm = comment.getText() == null ? "" : comment.getText().trim();
                if (comm.length() > 256) throw new IllegalArgumentException("Commento troppo lungo (max 256).");

                int stile = sStile.getValue();
                int cont = sCont.getValue();
                int grad = sGrad.getValue();
                int orig = sOrig.getValue();
                int ediz = sEdiz.getValue();

                int votoFinale = (int) Math.round((stile + cont + grad + orig + ediz) / 5.0);

                Review newR = new Review(user, book.getId(), stile, cont, grad, orig, ediz, votoFinale, comm);

                boolean ok = editing ? reviewService.updateReview(newR)
                        : reviewService.inserisciValutazione(newR);

                if (!ok) {
                    FxUtil.error(owner, "Operazione non riuscita",
                            "Non posso salvare la valutazione.\n" +
                                    "Controlla che il libro sia in una tua libreria e che i dati siano validi.");
                    return;
                }

                FxUtil.toast(owner.getScene(), editing ? "Valutazione aggiornata" : "Valutazione salvata");
                showDetail(owner, book);
                d.close();

            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        VBox box = new VBox(12, h, sub, new Separator(), grid, comment, hint, new Separator(), save);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }


    /**
     * Apre un dialog per inserire un suggerimento (fino a 3 libri) relativo a un libro base.
     * <p>
     * Regole applicate:
     * </p>
     * <ul>
     *   <li>Richiede login (tramite {@link #ensureLoggedIn(Stage)})</li>
     *   <li>Propone solo libri presenti nelle librerie dell'utente (escluso il libro base)</li>
     *   <li>Evita duplicati tra le tre selezioni</li>
     *   <li>Salva tramite {@link SuggestionService#inserisciSuggerimento(Suggestion)}</li>
     * </ul>
     *
     * <p>
     * I libri selezionabili vengono ricavati principalmente da:
     * </p>
     * <ul>
     *   <li>Librerie utente (via {@link LibraryService#listUserLibraries(String)})</li>
     *   <li>Cache locale (<code>bookCache</code>) e tabella corrente (<code>data</code>) come fallback</li>
     * </ul>
     *
     * @param owner stage proprietario
     * @param book libro base per cui suggerire altri libri
     * @see LibraryService#listUserLibraries(String)
     * @see SuggestionService#inserisciSuggerimento(Suggestion)
     * @see #bookCache
     * @see #data
     */
    private void openSuggestionEditor(Stage owner, Book book) {
        String user = ensureLoggedIn(owner);
        if (user == null || book == null) return;

        List<Book> myBooks;
        try {
            Set<Integer> myIds = libraryService.listUserLibraries(user).stream()
                    .map(Library::getBookIds)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            myIds.remove(book.getId());

            LinkedHashMap<Integer, Book> map = new LinkedHashMap<>();
            for (Integer id : myIds) {
                // prima cache, poi prova a “riportare” in cache qualcosa dalla tabella corrente
                Book b = bookCache.get(id);
                if (b == null) {
                    Optional<Book> inTable = data.stream().filter(x -> x.getId() == id).findFirst();
                    if (inTable.isPresent()) {
                        b = inTable.get();
                        bookCache.put(b.getId(), b);
                    }
                }
                if (b != null) map.put(b.getId(), b);
            }

            myBooks = new ArrayList<>(map.values());
            myBooks.sort(Comparator.comparing(
                    b -> b.getTitolo() == null ? "" : b.getTitolo(),
                    String.CASE_INSENSITIVE_ORDER
            ));

        } catch (Exception e) {
            FxUtil.error(owner, "Errore", "Impossibile leggere le tue librerie: " + e.getMessage());
            return;
        }

        if (myBooks.isEmpty()) {
            FxUtil.error(owner, "Nessun libro selezionabile",
                    "Non hai libri nelle tue librerie da poter consigliare.\n" +
                            "Suggerimento: fai prima una ricerca e apri i dettagli di qualche libro, così la cache si popola.");
            return;
        }

        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Consiglia libri");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label h = new Label("Consiglia libri correlati");
        h.getStyleClass().add("title");

        Label sub = new Label("Libro base: " + (book.getTitolo() == null ? "" : book.getTitolo()));
        sub.getStyleClass().add("subtitle");
        sub.setWrapText(true);

        Label hint = labelMuted(
                """
                        Puoi consigliare fino a 3 libri.
                        Mostro solo i libri presenti nelle tue librerie.
                        Scrivi nel campo per filtrare (titolo / ID)."""
        );

        ComboBox<Book> c1 = suggestCombo(myBooks);
        ComboBox<Book> c2 = suggestCombo(myBooks);
        ComboBox<Book> c3 = suggestCombo(myBooks);

        Button save = new Button("Salva consiglio");
        save.getStyleClass().add("primary");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setDisable(true);

        Runnable refreshSave = () -> {
            boolean any = (c1.getValue() != null) || (c2.getValue() != null) || (c3.getValue() != null);
            save.setDisable(!any);
        };

        Runnable enforceNoDuplicates = () -> {
            Book b1 = c1.getValue();
            Book b2 = c2.getValue();
            Book b3 = c3.getValue();

            if (b1 != null && b2 != null && b1.getId() == b2.getId()) c2.setValue(null);
            if (b1 != null && b3 != null && b1.getId() == b3.getId()) c3.setValue(null);
            if (b2 != null && b3 != null && b2.getId() == b3.getId()) c3.setValue(null);

            refreshSave.run();
        };

        c1.valueProperty().addListener((ignoreObs, ignoreOld, ignoreN) -> enforceNoDuplicates.run());
        c2.valueProperty().addListener((ignoreObs, ignoreOld, ignoreN) -> enforceNoDuplicates.run());
        c3.valueProperty().addListener((ignoreObs, ignoreOld, ignoreN) -> enforceNoDuplicates.run());

        save.setOnAction(e -> {
            try {
                LinkedHashSet<Integer> ids = new LinkedHashSet<>();
                if (c1.getValue() != null) ids.add(c1.getValue().getId());
                if (c2.getValue() != null) ids.add(c2.getValue().getId());
                if (c3.getValue() != null) ids.add(c3.getValue().getId());

                if (ids.isEmpty()) {
                    FxUtil.error(owner, "Selezione mancante", "Seleziona almeno 1 libro da consigliare.");
                    return;
                }
                if (ids.contains(book.getId())) {
                    FxUtil.error(owner, "Non valido", "Non puoi consigliare lo stesso libro base.");
                    return;
                }
                if (ids.size() > 3) {
                    FxUtil.error(owner, "Non valido", "Massimo 3 consigli.");
                    return;
                }

                Suggestion s = new Suggestion(user, book.getId(), new ArrayList<>(ids));
                boolean ok = suggestionService.inserisciSuggerimento(s);

                if (!ok) {
                    FxUtil.error(owner, "Operazione non riuscita",
                            "Non posso salvare il consiglio.\n" +
                                    "Controlla che i libri scelti siano nelle tue librerie e che siano massimo 3.");
                    return;
                }

                FxUtil.toast(owner.getScene(), "Consiglio salvato");
                showDetail(owner, book);
                d.close();

            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(label("Libro consigliato 1"), 0, 0); grid.add(c1, 1, 0);
        grid.add(label("Libro consigliato 2"), 0, 1); grid.add(c2, 1, 1);
        grid.add(label("Libro consigliato 3"), 0, 2); grid.add(c3, 1, 2);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setMinWidth(170);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);

        VBox box = new VBox(12, h, sub, new Separator(), hint, grid, new Separator(), save);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }


    /**
     * Crea una {@link ComboBox} editabile per selezionare un libro da una lista sorgente,
     * con filtro live su titolo o ID.
     * <p>
     * La {@link ComboBox} usa una {@link FilteredList} per filtrare gli elementi in base al testo digitato.
     * </p>
     *
     * @param source lista di libri selezionabili
     * @return combo box configurata con filtro su titolo/ID
     */
    private ComboBox<Book> suggestCombo(List<Book> source) {
        ObservableList<Book> base = FXCollections.observableArrayList(source);
        FilteredList<Book> filtered = new FilteredList<>(base, ignoreB -> true);

        ComboBox<Book> cb = new ComboBox<>(filtered);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setEditable(true);
        cb.getEditor().setPromptText("Cerca per titolo o ID…");

        cb.setConverter(new StringConverter<>() {
            @Override public String toString(Book b) {
                return (b == null) ? "" : (b.getTitolo() == null ? "" : b.getTitolo());
            }
            @Override public Book fromString(String s) {
                return cb.getValue(); // solo filtro
            }
        });

        cb.setCellFactory(ignoreList -> new ListCell<>() {
            @Override protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                String title = item.getTitolo() == null ? "" : item.getTitolo();
                setText(title + "  (ID " + item.getId() + ")");
            }
        });

        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText("");
                else setText(item.getTitolo() == null ? "" : item.getTitolo());
            }
        });

        cb.getEditor().textProperty().addListener((ignoreObs, ignoreOld, txt) -> {
            String q = (txt == null) ? "" : txt.trim().toLowerCase();

            Book sel = cb.getValue();
            if (sel != null) {
                String selTxt = (sel.getTitolo() == null ? "" : sel.getTitolo()).trim().toLowerCase();
                if (q.equals(selTxt)) return;
            }

            if (q.isEmpty()) {
                filtered.setPredicate(ignoreB -> true);
            } else {
                filtered.setPredicate(b -> {
                    String title = (b.getTitolo() == null ? "" : b.getTitolo()).toLowerCase();
                    String id = String.valueOf(b.getId());
                    return title.contains(q) || id.contains(q);
                });
            }

            if (!cb.isShowing()) cb.show();
        });

        return cb;
    }


    /**
     * Crea uno {@link Spinner} intero configurato per valori da 1 a 5.
     *
     * @return spinner 1..5 (valore iniziale 3)
     */
    private static Spinner<Integer> spinner1to5() {
        Spinner<Integer> sp = new Spinner<>();
        sp.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 3));
        sp.setEditable(false);
        return sp;
    }


    // ---------------- Reserved / Auth ----------------
    /**
     * Apre l'area riservata (dialog) con accesso a funzionalità utente:
     * librerie, valutazioni, consigli e profilo account.
     * <p>
     * Richiede login (tramite {@link #ensureLoggedIn(Stage)}). Dopo la chiusura delle finestre
     * viene aggiornata la UI tramite {@link #refreshUserUi()}.
     * </p>
     *
     * @param owner stage proprietario
     * @see #ensureLoggedIn(Stage)
     * @see #refreshUserUi()
     */
    private void openReservedHome(Stage owner) {
        String user = ensureLoggedIn(owner);
        if (user == null) return;

        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Area riservata");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label title = new Label("Area Riservata");
        title.getStyleClass().add("title");
        Label sub = new Label("Gestisci librerie, valutazioni, consigli e account");
        sub.getStyleClass().add("subtitle");

        Button btnLibs = new Button("Le mie librerie");
        btnLibs.getStyleClass().add("primary");
        btnLibs.setMaxWidth(Double.MAX_VALUE);
        btnLibs.setOnAction(e -> LibrariesWindow.open(authService, libraryService, libriRepo));

        Button btnReviews = makeGhost("Le mie valutazioni", () -> ReviewsWindow.open(authService, reviewService, libriRepo));
        Button btnSug     = makeGhost("I miei consigli", () -> SuggestionsWindow.open(authService, suggestionService, libriRepo));

        Button btnAcc = makeGhost("Account", () -> {
            UserProfileWindow.open(authService);

            // se l'utente ha eliminato l'account (o fatto logout dal profilo),
            // aggiorna subito la UI e chiudi l'area riservata
            refreshUserUi();

            if (authService.getCurrentUserid() == null) {
                d.close(); // chiude l’area riservata se non sei più loggato
            }
        });

        VBox box = new VBox(10, title, sub, new Separator(),
                btnLibs,
                btnReviews,
                btnSug,
                btnAcc
        );


        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }


    /**
     * Crea un pulsante con stile "ghost" che esegue un'azione {@link Runnable}.
     *
     * @param text testo del pulsante
     * @param action azione da eseguire al click
     * @return pulsante configurato
     */
    private Button makeGhost(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("ghost");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> action.run());
        return b;
    }


    /**
     * Apre un dialog di login.
     * <p>
     * In caso di successo aggiorna la UI e chiude il dialog.
     * </p>
     *
     * @param owner stage proprietario
     * @see AuthService#login(String, String)
     * @see #refreshUserUi()
     */
    private void openLogin(Stage owner) {
        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Login");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        TextField user = new TextField();
        user.setPromptText("Username");

        PasswordManager pr = new PasswordManager("Password");

        Button btn = new Button("Accedi");
        btn.getStyleClass().add("primary");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setDefaultButton(true);
        user.setOnAction(e -> btn.fire());
        pr.getNode().lookupAll(".text-field").forEach(n -> {
            if (n instanceof TextField tf) tf.setOnAction(ev -> btn.fire());
        });


        btn.setOnAction(e -> {
            try {
                String uid = safe(user.getText());
                String pwd = pr.getText();

                boolean ok = authService.login(uid, pwd);
                if (!ok) {
                    FxUtil.error(owner, "Login fallito", "Credenziali errate.");
                    return;
                }
                refreshUserUi();
                FxUtil.toast(owner.getScene(), "Benvenuto, " + authService.getCurrentUserid());
                d.close();
            } catch (Exception ex) {
                FxUtil.error(owner, "Errore", ex.getMessage());
            }
        });

        VBox box = new VBox(10,
                label("Username"), user,
                label("Password"), pr.getNode(),
                new Separator(),
                btn
        );
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }


    /**
     * Apre un dialog di registrazione utente.
     * <p>
     * Esegue validazioni base (campi obbligatori, username, CF, email, robustezza password).
     * Dopo la registrazione tenta login e apre l'area riservata.
     * </p>
     *
     * @param owner stage proprietario
     * @see AuthService#registrazione(User)
     * @see AuthService#login(String, String)
     * @see #openReservedHome(Stage)
     */
    private void openRegister(Stage owner) {
        Dialog<Void> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("Registrazione");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        TextField nome = new TextField(); nome.setPromptText("Nome");
        TextField cognome = new TextField(); cognome.setPromptText("Cognome");
        TextField cf = new TextField(); cf.setPromptText("Codice fiscale (16 caratteri)");
        TextField email = new TextField(); email.setPromptText("email@dominio.it");
        TextField username = new TextField(); username.setPromptText("Username (5-20)");

        PasswordManager pw = new PasswordManager("Password (min 8, lettere+numeri)");
        PasswordManager pw2 = new PasswordManager("Ripeti password");

        Button btn = new Button("Crea account e accedi");
        btn.getStyleClass().add("primary");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setDefaultButton(true);

        // Enter su qualunque campo => submit
        nome.setOnAction(e -> btn.fire());
        cognome.setOnAction(e -> btn.fire());
        cf.setOnAction(e -> btn.fire());
        email.setOnAction(e -> btn.fire());
        username.setOnAction(e -> btn.fire());

        pw.getNode().lookupAll(".text-field").forEach(n -> {
            if (n instanceof TextField tf) tf.setOnAction(ev -> btn.fire());
        });
        pw2.getNode().lookupAll(".text-field").forEach(n -> {
            if (n instanceof TextField tf) tf.setOnAction(ev -> btn.fire());
        });


        btn.setOnAction(e -> {
            try {
                String n = safe(nome.getText());
                String c = safe(cognome.getText());
                String codice = safe(cf.getText());
                String em = safe(email.getText());
                String u = safe(username.getText());
                String p1 = pw.getText();
                String p2 = pw2.getText();

                if (n.isEmpty() || c.isEmpty() || codice.isEmpty() || em.isEmpty() || u.isEmpty() || p1.isEmpty() || p2.isEmpty())
                    throw new IllegalArgumentException("Tutti i campi sono obbligatori.");
                if (u.length() < 5 || u.length() > 20)
                    throw new IllegalArgumentException("Username deve essere tra 5 e 20 caratteri.");
                if (!CF_RX.matcher(codice).matches())
                    throw new IllegalArgumentException("Codice fiscale non valido (attesi 16 caratteri alfanumerici).");
                if (!EMAIL_RX.matcher(em).matches())
                    throw new IllegalArgumentException("Email non valida.");
                if (!PasswordManager.isStrongPassword(p1))
                    throw new IllegalArgumentException("Password troppo debole (min 8, almeno una lettera e un numero).");
                if (!Objects.equals(p1, p2))
                    throw new IllegalArgumentException("Le password non corrispondono.");

                boolean okReg = authService.registrazione(new User(u, p1, n, c, codice, em));
                if (!okReg) throw new IllegalStateException("Registrazione fallita: username già esistente?");

                boolean okLogin = authService.login(u, p1);
                if (!okLogin) throw new IllegalStateException("Account creato, ma login automatico fallito.");

                refreshUserUi();
                FxUtil.toast(owner.getScene(), "Account creato. Benvenuto, " + u);
                d.close();

                openReservedHome(owner);

            } catch (Exception ex) {
                FxUtil.error(owner, "Registrazione non valida", ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int r = 0;
        grid.add(label("Nome"), 0, r); grid.add(nome, 1, r++);
        grid.add(label("Cognome"), 0, r); grid.add(cognome, 1, r++);
        grid.add(label("Codice fiscale"), 0, r); grid.add(cf, 1, r++);
        grid.add(label("Email"), 0, r); grid.add(email, 1, r++);
        grid.add(label("Username"), 0, r); grid.add(username, 1, r++);
        grid.add(label("Password"), 0, r); grid.add(pw.getNode(), 1, r++);
        grid.add(label("Ripeti password"), 0, r); //noinspection UnusedAssignment
        grid.add(pw2.getNode(), 1, r++);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(160);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        VBox box = new VBox(12,
                labelMuted("Dopo la registrazione verrai loggato automaticamente ed entrerai nell’area riservata."),
                grid,
                new Separator(),
                btn
        );
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }


    /**
     * Verifica se l'utente è autenticato; in caso contrario mostra un errore e ritorna <code>null</code>.
     *
     * @param owner stage proprietario
     * @return userid dell'utente corrente se autenticato, altrimenti <code>null</code>
     */
    private String ensureLoggedIn(Stage owner) {
        if (authService.getCurrentUserid() != null) return authService.getCurrentUserid();
        FxUtil.error(owner, "Accesso richiesto", "Devi effettuare il login per accedere a questa funzione.");
        return null;
    }

    // ---------------- Helpers ----------------
    /**
     * Normalizza una stringa: se <code>null</code> ritorna stringa vuota, altrimenti fa <code>trim()</code>.
     *
     * @param s input
     * @return stringa normalizzata
     */
    private static String safe(String s) { return s == null ? "" : s.trim(); }


    /**
     * Ritorna <code>def</code> se <code>s</code> è <code>null</code> o blank, altrimenti ritorna <code>s</code>.
     *
     * @param s stringa
     * @param def valore di default
     * @return stringa non vuota
     */
    private static String nvl(String s, String def) { return (s == null || s.isBlank()) ? def : s; }


    /**
     * Crea una {@link Label} con stile "muted" usata come etichetta descrittiva nei form.
     *
     * @param text testo dell'etichetta
     * @return label stilizzata
     */
    private static Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        return l;
    }


    /**
     * Avvolge un contenuto in uno {@link ScrollPane} configurato per UI "card".
     * <p>
     * Imposta:
     * </p>
     * <ul>
     *   <li>fitToWidth/fitToHeight</li>
     *   <li>Barra orizzontale disabilitata</li>
     *   <li>Stile trasparente (evita bordi/riquadri)</li>
     * </ul>
     *
     * @param content contenuto da rendere scrollabile
     * @return scroll pane configurato
     */
    private static ScrollPane wrapScroll(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // evita bordi/riquadri brutti del ScrollPane
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        if (sp.getContent() instanceof Region r) {
            r.setMinHeight(Region.USE_PREF_SIZE);
        }
        return sp;
    }


    /**
     * Carica una prima lista di risultati per popolare la tabella all'avvio.
     * <p>
     * Effettua una ricerca con una parola comune e limita i risultati in base allo spinner limit.
     * In caso di errore non blocca l'applicazione.
     * </p>
     *
     */
    private void loadInitialResults() {
        try {
            int limit = spLimit.getValue();

            // parola comune per avere risultati subito
            String seed = "the";

            Response r = proxy.call(new Request(RequestType.SEARCH_BY_TITLE, seed, null));
            if (r == null || !r.ok) return;

            @SuppressWarnings("unchecked")
            List<Book> res = (List<Book>) r.data;

            if (res == null) return;

            res = res.stream().limit(limit).toList();

            data.setAll(res);
            res.forEach(b -> bookCache.put(b.getId(), b));

            lblStatus.setText("Risultati iniziali: " + res.size());
            clearDetail();
            if (!res.isEmpty()) tbl.getSelectionModel().select(0);

        } catch (Exception ignored) {
            // non blocca l'app se fallisce
        }
    }


    /**
     * Installa comportamenti per "commit" del valore dello {@link Spinner} quando:
     * </p>
     * <ul>
     *   <li>l'editor perde focus</li>
     *   <li>Si preme invio nell'editor</li>
     * </ul>
     * Inoltre evita cambi involontari tramite scroll quando il controllo non è focalizzato.
     *
     * @param sp spinner da configurare
     * @see #commitSpinnerEditor(Spinner, SpinnerValueFactory.IntegerSpinnerValueFactory)
     */
    private static void installSpinnerCommit(Spinner<Integer> sp) {
        if (sp == null || sp.getValueFactory() == null) return;

        TextField editor = sp.getEditor();
        SpinnerValueFactory.IntegerSpinnerValueFactory vf =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) sp.getValueFactory();

        // commit quando perdo focus
        editor.focusedProperty().addListener((obs, was, is) -> {
            if (!is) commitSpinnerEditor(sp, vf);
        });

        // commit quando premo ENTER nell'editor
        editor.setOnAction(e -> commitSpinnerEditor(sp, vf));

        // evita che la rotellina cambi valore mentre scrolli la pagina
        sp.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (!sp.isFocused() && !editor.isFocused()) e.consume();
        });
    }


    /**
     * Converte il testo dell'editor dello spinner in un intero valido e aggiorna il {@link SpinnerValueFactory}.
     * <p>
     * Se il testo non è numerico, ripristina il valore precedente valido.
     * </p>
     *
     * @param sp spinner
     * @param vf value factory dello spinner
     */
    private static void commitSpinnerEditor(Spinner<Integer> sp, SpinnerValueFactory.IntegerSpinnerValueFactory vf) {
        String txt = sp.getEditor().getText();
        if (txt == null || txt.isBlank()) return;

        try {
            int v = Integer.parseInt(txt.trim());
            v = Math.max(vf.getMin(), Math.min(vf.getMax(), v));
            vf.setValue(v);
        } catch (NumberFormatException ignored) {
            // ripristina valore precedente valido
            sp.getEditor().setText(String.valueOf(vf.getValue()));
        }
    }


    /**
     * Crea una {@link Label} con stile "muted" e testo a capo, usata per messaggi e hint.
     *
     * @param text testo da visualizzare
     * @return label stilizzata
     */
    private static Label labelMuted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }


    /**
     * Crea una riga header con un elemento a sinistra e uno a destra,
     * separati da uno spacer che cresce.
     *
     * @param left elemento sinistro (tipicamente un titolo)
     * @param right elemento destro (tipicamente pulsanti azione)
     * @return contenitore orizzontale header
     */
    private static HBox headerRow(Label left, Node right) {
        HBox row = new HBox(10, left, new Pane(), right);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }


    /**
     * Entry point Java standard che avvia l'applicazione JavaFX.
     *
     * @param args argomenti da linea di comando
     * @see Application#launch(String...)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
