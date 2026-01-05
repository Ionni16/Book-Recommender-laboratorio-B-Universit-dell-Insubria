package bookrecommender.ui;

import bookrecommender.model.Book;
import bookrecommender.model.Review;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.ReviewService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import bookrecommender.net.BRProxy;
import bookrecommender.net.Request;
import bookrecommender.net.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Finestra modale per la visualizzazione e gestione delle valutazioni
 * inserite dall'utente.
 * <p>
 * La finestra mostra in una tabella tutte le valutazioni relative
 * all'utente attualmente autenticato e permette di:
 * <ul>
 *     <li>Ricaricare l'elenco delle valutazioni;</li>
 *     <li>Eliminare una valutazione selezionata.</li>
 * </ul>
 * I dettagli dei libri (titolo) sono recuperati tramite il
 * {@link LibriRepository}, mentre i dati delle valutazioni sono gestiti
 * dal {@link ReviewService}.
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.service.ReviewService
 * @see bookrecommender.service.AuthService
 * @see bookrecommender.repo.LibriRepository
 */
public class ReviewsWindow extends Stage {

    private final AuthService authService;
    private final ReviewService reviewService;
    private final LibriRepository libriRepo;
    private final BRProxy proxy = new BRProxy("127.0.0.1", 5050);
    private final Map<Integer, String> titleCache = new HashMap<>();


    private final ObservableList<Review> reviews = FXCollections.observableArrayList();

    private TableView<Review> tbl;
    private Label lblHeader;

    /**
     * Costruisce e inizializza la finestra delle valutazioni.
     * <p>
     * Nel costruttore vengono:
     * <ul>
     *     <li>Memorizzate le dipendenze verso i servizi e il repository libri;</li>
     *     <li>Costruiti header, contenuto centrale e footer;</li>
     *     <li>Applicato il foglio di stile <code>app.css</code>;</li>
     *     <li>Caricate le valutazioni dell'utente corrente (se presente).</li>
     * </ul>
     *
     * @param authService   servizio di autenticazione per determinare
     *                      l'utente corrente
     * @param reviewService servizio per la gestione delle valutazioni
     * @param libriRepo     repository dei libri, usato per ottenere
     *                      i dettagli dei libri a partire dagli ID
     */
    public ReviewsWindow(AuthService authService, ReviewService reviewService, LibriRepository libriRepo) {
        this.authService = authService;
        this.reviewService = reviewService;
        this.libriRepo = libriRepo;

        setTitle("Le mie valutazioni");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-bg");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildFooter("Seleziona una valutazione e premi Elimina per rimuoverla."));


        Scene scene = new Scene(new StackPane(root), 980, 560);

        URL css = getClass().getResource("/bookrecommender/ui/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("CSS NON trovato: /bookrecommender/ui/app.css");
        }



        setScene(scene);
        load();
    }

    private Node buildHeader() {
        lblHeader = new Label("Valutazioni");
        lblHeader.getStyleClass().add("title");

        Label sub = new Label("Visualizza ed elimina le tue valutazioni.");
        sub.getStyleClass().add("subtitle");

        VBox box = new VBox(4, lblHeader, sub);
        box.getStyleClass().add("appbar");
        return box;
    }

    private Node buildFooter(String hintText) {
        Label hint = new Label(hintText == null ? "" : hintText);
        hint.getStyleClass().add("muted");

        Button close = new Button("Chiudi");
        close.getStyleClass().add("ghost");
        close.setOnAction(e -> close.getScene().getWindow().hide());

        HBox bar = new HBox(10, hint, new Pane(), close);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("statusbar");
        return bar;
    }


    private Node buildCenter() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card2");
        card.setPadding(new Insets(14));

        tbl = new TableView<>(reviews);
        tbl.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tbl.setPlaceholder(new Label("Nessuna valutazione disponibile."));

        TableColumn<Review, Integer> cBookId = new TableColumn<>("ID Libro");
        cBookId.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getBookId()));
        cBookId.setMaxWidth(110);

        TableColumn<Review, String> cTitle = new TableColumn<>("Titolo");
        cTitle.setCellValueFactory(v -> {
            int id = v.getValue().getBookId();

            // 1) prova repo (se hai cache locale)
            Book b = libriRepo.findById(id);
            if (b != null && b.getTitolo() != null && !b.getTitolo().isBlank()) {
                return new ReadOnlyObjectWrapper<>(b.getTitolo());
            }

            // 2) prova cache titoli da server
            String t = titleCache.get(id);
            return new ReadOnlyObjectWrapper<>(t == null ? "(n/d)" : t);
        });

        TableColumn<Review, Integer> cFinal = new TableColumn<>("Voto");
        cFinal.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getVotoFinale()));
        cFinal.setMaxWidth(110);

        TableColumn<Review, String> cComment = new TableColumn<>("Commento");
        cComment.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(
                v.getValue().getCommento() == null ? "" : v.getValue().getCommento()
        ));


        FxUtil.addColumns(tbl, List.of(cBookId, cTitle, cFinal, cComment));

        HBox actions = FxUtil.buildReloadDeleteBar(tbl, "Ricarica", this::load, "Elimina", this::deleteSelected);

        card.getChildren().addAll(new Label("Elenco"), tbl, actions);
        VBox.setVgrow(tbl, Priority.ALWAYS);

        return FxUtil.wrapCard(card);
    }


    private void load() {
        String user = authService.getCurrentUserid();
        if (user == null) {
            lblHeader.setText("Valutazioni (login richiesto)");
            reviews.clear();
            return;
        }

        lblHeader.setText("Valutazioni di: " + user);

        try {
            reviews.setAll(reviewService.listByUser(user));
            preloadTitlesFromServer();
            tbl.refresh();
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }

    private void preloadTitlesFromServer() {
    // carica solo i titoli mancanti
        Set<Integer> ids = reviews.stream()
                .map(Review::getBookId)
                .collect(Collectors.toSet());

        for (Integer id : ids) {
            if (id == null) continue;
            if (titleCache.containsKey(id)) continue;

            try {
                Response r = proxy.call(Request.getBookById(id));
                if (r != null && r.ok && r.data instanceof Book b) {
                    String t = (b.getTitolo() == null || b.getTitolo().isBlank()) ? "(n/d)" : b.getTitolo();
                    titleCache.put(id, t);
                } else {
                    titleCache.put(id, "(n/d)");
                }
            } catch (Exception e) {
                titleCache.put(id, "(n/d)");
            }
        }
    }


    private void deleteSelected() {
        Review r = tbl.getSelectionModel().getSelectedItem();
        if (r == null) return;

        Book b = libriRepo.findById(r.getBookId());
        String title = b == null ? String.valueOf(r.getBookId()) : b.getTitolo();

        if (!FxUtil.confirm(this, "Conferma", "Eliminare la valutazione per: " + title + "?"))
            return;

        try {
            boolean ok = reviewService.deleteReview(authService.getCurrentUserid(), r.getBookId());
            if (!ok) throw new IllegalStateException("Eliminazione fallita.");
            load();
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }

    /**
     * Apre la finestra delle valutazioni come dialog modale
     * e blocca l'esecuzione finché l'utente non la chiude.
     *
     * @param authService   servizio di autenticazione per individuare
     *                      l'utente corrente
     * @param reviewService servizio di gestione delle valutazioni
     * @param repo          repository dei libri, usato per visualizzare
     *                      i dettagli dei volumi associati alle valutazioni
     */
    public static void open(AuthService authService, ReviewService reviewService, LibriRepository repo) {
        ReviewsWindow w = new ReviewsWindow(authService, reviewService, repo);
        w.showAndWait();
    }
}
