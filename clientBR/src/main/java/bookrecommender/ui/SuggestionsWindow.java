package bookrecommender.ui;
import bookrecommender.model.Book;
import bookrecommender.model.Suggestion;
import bookrecommender.net.Request;
import bookrecommender.net.Response;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.SuggestionService;
import bookrecommender.net.BRProxy;

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

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Finestra modale per la visualizzazione e gestione dei consigli (suggerimenti) inseriti dall'utente.
 * <p>
 * La finestra mostra una tabella contenente:
 * </p>
 * <ul>
 *   <li>Libro base: ID e titolo;</li>
 *   <li>Libri suggeriti: massimo 3 titoli, visualizzati sempre per nome (mai per ID).</li>
 * </ul>
 *
 * <p>
 * La risoluzione dei titoli avviene in modo robusto tramite:
 * </p>
 * <ol>
 *   <li>Cache in memoria (<code>titleCache</code>);</li>
 *   <li>Repository client {@link LibriRepository} (se popolato come cache locale);</li>
 *   <li>Chiamata remota al server (<code>GET_BOOK_BY_ID</code>).</li>
 * </ol>
 *
 * @author Ionut Puiu
 * @version 1.0
 * @see SuggestionService
 * @see AuthService
 * @see LibriRepository
 * @see BRProxy
 */
public class SuggestionsWindow extends Stage {

    private final AuthService authService;
    private final SuggestionService suggestionService;
    private final LibriRepository libriRepo;

    private final BRProxy proxy = new BRProxy("127.0.0.1", 5050);
    private final Map<Integer, String> titleCache = new HashMap<>();

    private final ObservableList<Suggestion> suggestions = FXCollections.observableArrayList();

    private TableView<Suggestion> tbl;
    private Label lblHeader;


    /**
     * Costruisce e inizializza la finestra dei consigli.
     * <p>
     * Nel costruttore vengono:
     * </p>
     * <ul>
     *   <li>Memorizzate le dipendenze verso servizi e repository;</li>
     *   <li>Configurate title e modalità modale;</li>
     *   <li>Costruiti header, contenuto centrale e footer;</li>
     *   <li>Applicato il foglio di stile <code>app.css</code>;</li>
     *   <li>Caricati i consigli dell'utente corrente (se presente).</li>
     * </ul>
     *
     * @param authService servizio di autenticazione per individuare l'utente corrente
     * @param suggestionService servizio di gestione dei consigli
     * @param libriRepo repository dei libri, usato per risolvere i dettagli dai relativi ID
     */
    public SuggestionsWindow(AuthService authService, SuggestionService suggestionService, LibriRepository libriRepo) {
        this.authService = authService;
        this.suggestionService = suggestionService;
        this.libriRepo = libriRepo;

        setTitle("I miei consigli");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-bg");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(FxUtil.buildFooter("Seleziona un consiglio e premi Elimina per rimuoverlo."));

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


    /**
     * Costruisce l'header grafico della finestra.
     *
     * @return nodo JavaFX dell'header
     * @see #load()
     */
    private Node buildHeader() {
        lblHeader = new Label("Consigli");
        lblHeader.getStyleClass().add("title");

        Label sub = new Label("Visualizza ed elimina i consigli associati ai tuoi libri.");
        sub.getStyleClass().add("subtitle");

        VBox box = new VBox(4, lblHeader, sub);
        box.getStyleClass().add("appbar");
        return box;
    }


    /**
     * Costruisce il contenuto centrale della finestra.
     * <p>
     * Crea la tabella consigli e la barra azioni (ricarica/elimina).
     * </p>
     *
     * @return nodo JavaFX centrale
     * @see FxUtil#buildReloadUpdateDeleteBar(TableView, String, Runnable, String, Runnable, String, Runnable)
     * @see FxUtil#wrapCard(VBox)
     */
    private Node buildCenter() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card2");
        card.setPadding(new Insets(14));

        tbl = new TableView<>(suggestions);
        tbl.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tbl.setPlaceholder(new Label("Nessun consiglio disponibile."));

        TableColumn<Suggestion, Integer> cBookId = new TableColumn<>("ID Libro");
        cBookId.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getBookId()));
        cBookId.setPrefWidth(120);
        cBookId.setMaxWidth(140);

        TableColumn<Suggestion, String> cBookTitle = new TableColumn<>("Titolo libro");
        cBookTitle.setCellValueFactory(v -> {
            int id = v.getValue().getBookId();
            return new ReadOnlyObjectWrapper<>(resolveTitle(id));
        });
        cBookTitle.setPrefWidth(320);

        TableColumn<Suggestion, String> cSug = new TableColumn<>("Suggeriti (max 3)");
        cSug.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(formatSuggestedTitles(v.getValue())));
        cSug.setPrefWidth(460);

        FxUtil.addColumns(tbl, List.of(cBookId, cBookTitle, cSug));

        HBox actions = FxUtil.buildReloadUpdateDeleteBar(
                tbl,
                "Ricarica",
                this::load,
                "modifica",
                this::updateSelected,
                "Elimina",
                this::deleteSelected

        );

        card.getChildren().addAll(new Label("Elenco"), tbl, actions);
        VBox.setVgrow(tbl, Priority.ALWAYS);

        return FxUtil.wrapCard(card);
    }


    /**
     * Ricarica i consigli dell'utente corrente e aggiorna la tabella.
     * <p>
     * Se nessun utente è autenticato:
     * </p>
     * <ul>
     *   <li>Svuota l'elenco consigli;</li>
     *   <li>Svuota la cache titoli;</li>
     *   <li>Aggiorna l'header indicando che serve login.</li>
     * </ul>
     *
     * <p>
     * Dopo il caricamento pre-carica i titoli necessari tramite {@link #preloadTitlesFromServer()}.
     * </p>
     *
     * @see AuthService#getCurrentUserid()
     * @see SuggestionService#listByUser(String)
     * @see #preloadTitlesFromServer()
     */
    private void load() {
        String user = authService.getCurrentUserid();
        if (user == null) {
            lblHeader.setText("Consigli (login richiesto)");
            suggestions.clear();
            titleCache.clear();
            return;
        }

        lblHeader.setText("Consigli di: " + user);

        try {
            suggestions.setAll(suggestionService.listByUser(user));
            preloadTitlesFromServer();
            tbl.refresh();
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }


    /**
     * Elimina il consiglio selezionato dopo conferma utente.
     * <p>
     * Se l'operazione va a buon fine ricarica l'elenco tramite {@link #load()}.
     * </p>
     *
     * @see SuggestionService#deleteSuggestion(String, int)
     * @see FxUtil#confirm(javafx.stage.Window, String, String)
     * @see #load()
     */
    private void deleteSelected() {
        Suggestion s = tbl.getSelectionModel().getSelectedItem();
        if (s == null) return;

        String title = resolveTitle(s.getBookId());

        if (!FxUtil.confirm(this, "Conferma", "Eliminare il consiglio associato a: " + title + "?")) return;

        try {
            boolean ok = suggestionService.deleteSuggestion(authService.getCurrentUserid(), s.getBookId());
            if (!ok) throw new IllegalStateException("Eliminazione fallita.");
            load();
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }


    /**
     * Modifica il consiglio selezionato aprendo un editor per impostare fino a 3 ID libro suggeriti.
     * <p>
     * La modifica avviene reinviando al server l'intero {@link Suggestion} tramite
     * {@link SuggestionService#inserisciSuggerimento(Suggestion)} (che lato server sostituisce i suggerimenti).
     * </p>
     *
     * <p>
     * Applica vincoli lato client coerenti con il server:
     * </p>
     * <ul>
     *   <li>Almeno 1 suggerimento;</li>
     *   <li>Massimo 3 suggerimenti;</li>
     *   <li>ID unici;</li>
     *   <li>Nessun suggerito uguale al libro base.</li>
     * </ul>
     *
     * @see SuggestionService#inserisciSuggerimento(Suggestion)
     * @see FxUtil#error(javafx.stage.Window, String, String)
     * @see FxUtil#toast(javafx.scene.Scene, String)
     * @see #load()
     */
    private void updateSelected() {
        Suggestion s = tbl.getSelectionModel().getSelectedItem();
        if (s == null) return;

        String user = authService.getCurrentUserid();
        if (user == null) {
            FxUtil.error(this, "Errore", "Devi essere loggato per modificare un consiglio.");
            return;
        }

        int bookId = s.getBookId();
        String baseTitle = resolveTitle(bookId);

        List<Integer> current = s.getSuggeriti();
        String v1 = (current.size() > 0 && current.get(0) != null) ? String.valueOf(current.get(0)) : "";
        String v2 = (current.size() > 1 && current.get(1) != null) ? String.valueOf(current.get(1)) : "";
        String v3 = (current.size() > 2 && current.get(2) != null) ? String.valueOf(current.get(2)) : "";

        Dialog<Void> d = new Dialog<>();
        d.initOwner(this);
        d.setTitle("Modifica consiglio");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label h = new Label("Libri consigliati (max 3)");
        h.getStyleClass().add("title");
        Label sub = new Label(baseTitle);
        sub.getStyleClass().add("subtitle");

        TextField t1 = new TextField(v1);
        TextField t2 = new TextField(v2);
        TextField t3 = new TextField(v3);
        t1.setPromptText("ID libro suggerito #1");
        t2.setPromptText("ID libro suggerito #2");
        t3.setPromptText("ID libro suggerito #3");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;
        grid.add(new Label("Suggerito 1"), 0, row); grid.add(t1, 1, row++);
        grid.add(new Label("Suggerito 2"), 0, row); grid.add(t2, 1, row++);
        grid.add(new Label("Suggerito 3"), 0, row); grid.add(t3, 1, row++);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(170);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        Button save = new Button("Salva modifiche");
        save.getStyleClass().add("primary");
        save.setMaxWidth(Double.MAX_VALUE);

        save.setOnAction(e -> {
            try {
                java.util.LinkedHashSet<Integer> set = new java.util.LinkedHashSet<>();

                for (TextField tf : List.of(t1, t2, t3)) {
                    String raw = tf.getText() == null ? "" : tf.getText().trim();
                    if (raw.isBlank()) continue;

                    int id;
                    try {
                        id = Integer.parseInt(raw);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("ID non valido: " + raw);
                    }

                    if (id == bookId) continue; // come server: niente self-reference
                    set.add(id);
                    if (set.size() >= 3) break;
                }

                if (set.isEmpty()) throw new IllegalArgumentException("Devi inserire almeno 1 libro da consigliare.");
                if (set.size() > 3) throw new IllegalArgumentException("Massimo 3 consigli.");

                Suggestion updated = new Suggestion(user, bookId, new java.util.ArrayList<>(set));

                boolean ok = suggestionService.inserisciSuggerimento(updated);
                if (!ok) throw new IllegalStateException("Aggiornamento fallito.");

                FxUtil.toast(getScene(), "Consiglio aggiornato");
                load();
                d.close();

            } catch (Exception ex) {
                FxUtil.error(this, "Errore", ex.getMessage());
            }
        });

        VBox box = new VBox(12, h, sub, new Separator(), grid, new Separator(), save);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        d.getDialogPane().setContent(box);
        d.showAndWait();
    }


    /**
     * Risolve il titolo di un libro dato il suo ID, usando una strategia a fallback:
     * <ol>
     *   <li>Cache in memoria (<code>titleCache</code>);</li>
     *   <li>Cache locale tramite {@link LibriRepository#findById(Integer)};</li>
     *   <li>Richiesta remota al server tramite <code>GET_BOOK_BY_ID</code>.</li>
     * </ol>
     * In caso di fallimento ritorna <code>(n/d)</code> e aggiorna comunque la cache.
     *
     * @param bookId identificativo del libro
     * @return titolo risolto oppure <code>(n/d)</code>
     * @see Request#getBookById(int)
     * @see BRProxy#call(bookrecommender.net.Request)
     * @see LibriRepository#findById(Integer)
     */
    private String resolveTitle(int bookId) {
        String cached = titleCache.get(bookId);
        if (cached != null) return cached;

        // prova repo locale (se è una cache popolata)
        try {
            Book bLocal = libriRepo.findById(bookId);
            if (bLocal != null && bLocal.getTitolo() != null && !bLocal.getTitolo().isBlank()) {
                String t = bLocal.getTitolo().trim();
                titleCache.put(bookId, t);
                return t;
            }
        } catch (Exception ignore) {}

        // prova server
        try {
            Response r = proxy.call(Request.getBookById(bookId));
            if (r != null && r.ok && r.data instanceof Book b) {
                String t = (b.getTitolo() == null || b.getTitolo().isBlank()) ? "(n/d)" : b.getTitolo().trim();
                titleCache.put(bookId, t);
                return t;
            }
        } catch (Exception ignore) {}

        titleCache.put(bookId, "(n/d)");
        return "(n/d)";
    }


    /**
     * Pre-carica i titoli necessari per rendere la tabella immediata.
     * <p>
     * Raccoglie tutti gli ID coinvolti:
     * </p>
     * <ul>
     *   <li>ID libro base di ogni consiglio;</li>
     *   <li>ID dei libri suggeriti (se presenti, massimo 3 per consiglio).</li>
     * </ul>
     *
     * <p>
     * La cache viene popolata usando {@link #resolveTitle(int)}.
     * </p>
     *
     * @see #resolveTitle(int)
     */
    private void preloadTitlesFromServer() {
        // raccoglie tutti gli id: libro base + suggeriti
        Set<Integer> ids = suggestions.stream()
                .flatMap(s -> {
                    List<Integer> all = new ArrayList<>();
                    all.add(s.getBookId());
                    if (s.getSuggeriti() != null) all.addAll(s.getSuggeriti());
                    return all.stream();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Integer id : ids) {
            if (id == null) continue;
            if (titleCache.containsKey(id)) continue;

            // usa resolveTitle che fa già tutti i fallback e riempie la cache
            resolveTitle(id);
        }
    }


    /**
     * Formatta i titoli dei libri suggeriti (max 3) associati a un consiglio.
     * <p>
     * Mostra sempre titoli (mai ID). Se un titolo non è risolvibile viene usato <code>(n/d)</code>.
     * Se la lista suggeriti è vuota o nulla, ritorna <code>-</code>.
     * </p>
     *
     * @param s consiglio da formattare
     * @return stringa con titoli separati da <code> • </code> oppure <code>-</code>
     * @see #resolveTitle(int)
     */
    private String formatSuggestedTitles(Suggestion s) {
        List<Integer> ids = s.getSuggeriti();
        if (ids == null || ids.isEmpty()) return "-";

        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .map(this::resolveTitle)  // mai ID
                .collect(Collectors.joining(" • "));
    }


    /**
     * Apre la finestra dei consigli come dialog modale e blocca l'esecuzione finché l'utente non la chiude.
     *
     * @param authService servizio di autenticazione per individuare l'utente corrente
     * @param suggestionService servizio di gestione dei consigli
     * @param repo repository dei libri, usato per risolvere i dettagli dei volumi
     */
    public static void open(AuthService authService, SuggestionService suggestionService, LibriRepository repo) {
        SuggestionsWindow w = new SuggestionsWindow(authService, suggestionService, repo);
        w.showAndWait();
    }
}
