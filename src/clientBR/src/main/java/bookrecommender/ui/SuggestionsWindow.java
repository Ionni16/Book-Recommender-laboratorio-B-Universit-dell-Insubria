package bookrecommender.ui;

import bookrecommender.model.Book;
import bookrecommender.model.Suggestion;
import bookrecommender.net.BRProxy;
import bookrecommender.net.Request;
import bookrecommender.net.Response;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.SuggestionService;
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
 * Finestra modale per la visualizzazione e gestione dei consigli
 * (suggerimenti di libri correlati) inseriti dall'utente.
 *
 * Mostra:
 * - libro base (ID + titolo)
 * - suggeriti (max 3) per TITOLO (mai ID)
 *
 * Richiede GET_BOOK_BY_ID lato server per risolvere i titoli quando il repo client è vuoto/cache.
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
        root.setBottom(buildFooter("Seleziona un consiglio e premi Elimina per rimuoverlo."));

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
        lblHeader = new Label("Consigli");
        lblHeader.getStyleClass().add("title");

        Label sub = new Label("Visualizza ed elimina i consigli associati ai tuoi libri.");
        sub.getStyleClass().add("subtitle");

        VBox box = new VBox(4, lblHeader, sub);
        box.getStyleClass().add("appbar");
        return box;
    }

    private static Node buildFooter(String hintText) {
        Label hint = new Label(hintText == null ? "" : hintText);
        hint.getStyleClass().add("muted");

        Button closeBtn = new Button("Chiudi");
        closeBtn.getStyleClass().add("ghost");
        closeBtn.setOnAction(e -> closeBtn.getScene().getWindow().hide());

        HBox bar = new HBox(10, hint, new Pane(), closeBtn);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("statusbar");
        return bar;
    }

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

        HBox actions = FxUtil.buildReloadDeleteBar(
                tbl,
                "Ricarica",
                this::load,
                "Elimina",
                this::deleteSelected
        );

        card.getChildren().addAll(new Label("Elenco"), tbl, actions);
        VBox.setVgrow(tbl, Priority.ALWAYS);

        return FxUtil.wrapCard(card);
    }

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
     * Risolve un titolo in modo robusto:
     * 1) cache in-memory (titleCache)
     * 2) repo client (se ha cache locale)
     * 3) server GET_BOOK_BY_ID
     * fallback: "(n/d)"
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
     * Precarica titoli per libro base e suggeriti (max 3) per rendere la tabella immediata.
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
     * Mostra SEMPRE titoli (mai ID).
     * Se un titolo non è risolvibile => "(n/d)".
     */
    private String formatSuggestedTitles(Suggestion s) {
        List<Integer> ids = s.getSuggeriti();
        if (ids == null || ids.isEmpty()) return "-";

        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .map(id -> resolveTitle(id))  // mai ID
                .collect(Collectors.joining(" • "));
    }

    public static void open(AuthService authService, SuggestionService suggestionService, LibriRepository repo) {
        SuggestionsWindow w = new SuggestionsWindow(authService, suggestionService, repo);
        w.showAndWait();
    }
}
