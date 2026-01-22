package bookrecommender.ui;

import bookrecommender.model.Book;
import bookrecommender.model.Library;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AuthService;
import bookrecommender.service.LibraryService;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
 * Finestra di gestione delle librerie personali dell'utente.
 * <p>
 * Questa finestra modale permette di:
 * <ul>
 *     <li>Visualizzare tutte le librerie dell'utente autenticato;</li>
 *     <li>Creare nuove librerie;</li>
 *     <li>Rinominare ed eliminare librerie esistenti;</li>
 *     <li>Visualizzare i libri contenuti nella libreria selezionata;</li>
 *     <li>Rimuovere singoli libri da una libreria.</li>
 * </ul>
 * I dati sono forniti dai servizi {@link AuthService} e
 * {@link LibraryService} e dal {@link LibriRepository} per risolvere i
 * dettagli dei libri a partire dagli identificatori.
 *
 * @author Ionut Puiu
 * @version 2.0
 * @see bookrecommender.service.LibraryService
 * @see bookrecommender.service.AuthService
 * @see bookrecommender.repo.LibriRepository
 */
public class LibrariesWindow extends Stage {

    private final AuthService authService;
    private final LibraryService libraryService;
    private final LibriRepository libriRepo;

    private final ObservableList<Library> libsData = FXCollections.observableArrayList();
    private final ObservableList<Book> booksData = FXCollections.observableArrayList();

    private TableView<Library> tblLibs;
    private TableView<Book> tblBooks;

    private Label lblHeader;

    /**
     * Costruisce e inizializza la finestra di gestione librerie.
     * <p>
     * Nel costruttore vengono:
     * <ul>
     *     <li>Memorizzate le dipendenze verso i servizi e il repository libri;</li>
     *     <li>Costruita la struttura grafica (header, centro, footer);</li>
     *     <li>Applicato il foglio di stile <code>app.css</code>;</li>
     *     <li>Caricate le librerie dell'utente attualmente autenticato.</li>
     * </ul>
     *
     * @param authService    servizio di autenticazione per recuperare l'utente corrente
     * @param libraryService servizio per l'accesso e la modifica delle librerie
     * @param libriRepo      repository libri usato per risolvere i dettagli dei volumi
     */
    public LibrariesWindow(AuthService authService, LibraryService libraryService, LibriRepository libriRepo) {
        this.authService = authService;
        this.libraryService = libraryService;
        this.libriRepo = libriRepo;

        setTitle("Gestione Librerie");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-bg");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(FxUtil.buildFooter(""));

        Scene scene = new Scene(new StackPane(root), 980, 560);
        URL css = getClass().getResource("/bookrecommender/ui/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("CSS NON trovato: /bookrecommender/ui/app.css");
        }



        setScene(scene);
        loadLibraries();
    }


    /**
     * Costruisce l'header grafico della finestra, con titolo dinamico e sottotitolo descrittivo.
     *
     * @return nodo JavaFX per la sezione header
     * @see #loadLibraries()
     */
    private Node buildHeader() {
        lblHeader = new Label("Le tue librerie");
        lblHeader.getStyleClass().add("title");

        Label sub = new Label("Rinomina, elimina, aggiungi/rimuovi libri");
        sub.getStyleClass().add("subtitle");

        VBox box = new VBox(4, lblHeader, sub);
        box.getStyleClass().add("appbar");
        return box;
    }


    /**
     * Costruisce il contenuto centrale della finestra.
     * <p>
     * Layout:
     * </p>
     * <ul>
     *   <li>Pannello sinistro: tabella librerie + azioni (crea/rinomina/elimina)</li>
     *   <li>Pannello destro: tabella libri della libreria selezionata + azione rimozione libro</li>
     * </ul>
     *
     * <p>
     * La selezione di una libreria nella tabella sinistra aggiorna automaticamente
     * la lista libri tramite {@link #loadBooksOf(Library)}.
     * </p>
     *
     * @return nodo JavaFX centrale
     * @see #createNewLibrary()
     * @see #renameLibrary()
     * @see #deleteLibrary()
     * @see #removeBookFromLibrary()
     */
    private Node buildCenter() {
        VBox left = new VBox(10);
        left.getStyleClass().add("card");
        left.setPadding(new Insets(14));

        Label lt = new Label("Librerie");
        lt.getStyleClass().add("card-title");

        tblLibs = new TableView<>(libsData);
        tblLibs.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tblLibs.setPlaceholder(new Label("Nessuna libreria trovata."));

        TableColumn<Library, String> cName = new TableColumn<>("Nome");
        cName.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getNome()));

        TableColumn<Library, Number> cCount = new TableColumn<>("Libri");
        cCount.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getBookIds().size()));
        cCount.setMaxWidth(110);

        tblLibs.getColumns().add(cName);
        tblLibs.getColumns().add(cCount);

        tblLibs.getSelectionModel().selectedItemProperty().addListener((ignoredObs, ignoredOld, newLib) -> {
            if (newLib != null) loadBooksOf(newLib);
        });

        Button btnNew = new Button("Crea libreria…");
        btnNew.getStyleClass().add("primary");
        btnNew.setOnAction(e -> createNewLibrary());

        Button btnRename = new Button("Rinomina…");
        btnRename.setOnAction(e -> renameLibrary());
        btnRename.disableProperty().bind(tblLibs.getSelectionModel().selectedItemProperty().isNull());

        Button btnDelete = new Button("Elimina");
        btnDelete.getStyleClass().add("danger");
        btnDelete.setOnAction(e -> deleteLibrary());
        btnDelete.disableProperty().bind(tblLibs.getSelectionModel().selectedItemProperty().isNull());

        HBox libActions = new HBox(10, btnNew, btnRename, btnDelete);
        libActions.setAlignment(Pos.CENTER_RIGHT);

        left.getChildren().addAll(lt, tblLibs, libActions);
        VBox.setVgrow(tblLibs, Priority.ALWAYS);

        VBox right = new VBox(10);
        right.getStyleClass().add("card2");
        right.setPadding(new Insets(14));

        Label rt = new Label("Libri nella libreria selezionata");
        rt.getStyleClass().add("card-title");

        tblBooks = new TableView<>(booksData);
        tblBooks.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tblBooks.setPlaceholder(new Label("Seleziona una libreria."));

        TableColumn<Book, Integer> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getId()));
        cId.setMaxWidth(90);

        TableColumn<Book, String> cTitle = new TableColumn<>("Titolo");
        cTitle.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getTitolo()));

        tblBooks.getColumns().add(cId);
        tblBooks.getColumns().add(cTitle);

        Button btnRemove = new Button("Rimuovi libro");
        btnRemove.getStyleClass().add("danger");
        btnRemove.setOnAction(e -> removeBookFromLibrary());
        btnRemove.disableProperty().bind(
                tblBooks.getSelectionModel().selectedItemProperty().isNull()
                        .or(tblLibs.getSelectionModel().selectedItemProperty().isNull())
        );

        HBox bookActions = new HBox(10, btnRemove);
        bookActions.setAlignment(Pos.CENTER_RIGHT);

        right.getChildren().addAll(rt, tblBooks, bookActions);
        VBox.setVgrow(tblBooks, Priority.ALWAYS);

        HBox center = new HBox(14, left, right);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(360);

        BorderPane wrap = new BorderPane(center);
        wrap.setPadding(new Insets(14));
        return wrap;
    }


    /**
     * Carica dal servizio remoto tutte le librerie dell'utente corrente e aggiorna la tabella.
     * <p>
     * Se nessun utente è autenticato, svuota le liste e aggiorna header indicando che serve login.
     * </p>
     *
     * @see AuthService#getCurrentUserid()
     * @see LibraryService#listUserLibraries(String)
     */
    private void loadLibraries() {
        String user = authService.getCurrentUserid();
        if (user == null) {
            libsData.clear();
            booksData.clear();
            lblHeader.setText("Le tue librerie (login richiesto)");
            return;
        }

        lblHeader.setText("Librerie di: " + user);

        try {
            List<Library> libs = libraryService.listUserLibraries(user);
            libsData.setAll(libs);
            if (!libs.isEmpty()) tblLibs.getSelectionModel().select(0);
        } catch (Exception e) {
            FxUtil.error(this, "Errore", "Errore nel caricamento delle librerie:\n" + e.getMessage());
        }
    }


    /**
     * Popola la tabella dei libri a partire dagli ID presenti nella libreria selezionata.
     * <p>
     * Risolve ogni ID tramite {@link LibriRepository#findById(Integer)} e scarta eventuali risultati null.
     * </p>
     *
     * @param lib libreria selezionata
     * @see LibriRepository#findById(Integer)
     */
    private void loadBooksOf(Library lib) {
        booksData.clear();
        if (lib == null) return;

        List<Book> bs = lib.getBookIds().stream()
                .map(libriRepo::findById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        booksData.setAll(bs);
    }


    /**
     * Apre un dialog per creare una nuova libreria e la salva tramite {@link LibraryService}.
     * <p>
     * Validazione: nome minimo 5 caratteri.
     * Dopo il salvataggio ricarica l'elenco librerie.
     * </p>
     *
     * @see LibraryService#saveLibrary(Library)
     * @see #loadLibraries()
     */
    private void createNewLibrary() {
        String user = authService.getCurrentUserid();
        if (user == null) return;

        TextInputDialog d = new TextInputDialog();
        d.initOwner(this);
        d.setTitle("Nuova libreria");
        d.setHeaderText(null);
        d.setContentText("Nome libreria (min 5 caratteri):");

        Optional<String> r = d.showAndWait();
        if (r.isEmpty()) return;

        String name = r.get().trim();
        if (name.length() < 5) {
            FxUtil.error(this, "Nome non valido", "Il nome deve avere almeno 5 caratteri.");
            return;
        }

        try {
            Library lib = new Library(user, name, new HashSet<>());
            boolean ok = libraryService.saveLibrary(lib);
            if (!ok) throw new IllegalStateException("Salvataggio fallito.");
            loadLibraries();
            FxUtil.info(this, "Ok", "Libreria creata.");
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }


    /**
     * Rinomina la libreria selezionata.
     * <p>
     * Chiede un nuovo nome, confronta con quello attuale e, se diverso,
     * salva una nuova istanza di {@link Library} tramite {@link LibraryService}.
     * </p>
     *
     * @see LibraryService#saveLibrary(Library)
     * @see #checkLibrary()
     * @see #loadLibraries()
     */
    private void renameLibrary() {
        Library sel = tblLibs.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        TextInputDialog d = new TextInputDialog(sel.getNome());
        d.initOwner(this);
        d.setTitle("Rinomina libreria");
        d.setHeaderText(null);
        d.setContentText("Nuovo nome (min 5 caratteri):");


        if (checkLibrary().equals(sel.getNome())) {
            FxUtil.info(this, "Nessuna modifica", "Il nome è identico: nessuna modifica salvata.");
            return;
        }

        try {
            Library updated = new Library(sel.getUserid(), checkLibrary(), sel.getBookIds());
            boolean ok = libraryService.saveLibrary(updated);
            if (!ok) throw new IllegalStateException("Salvataggio fallito.");
            loadLibraries();
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }


    /**
     * Elimina la libreria selezionata dopo conferma utente.
     * <p>
     * Dopo l'eliminazione ricarica l'elenco librerie.
     * </p>
     *
     * @see FxUtil#confirm(javafx.stage.Window, String, String)
     * @see LibraryService#deleteLibrary(String, String)
     * @see #loadLibraries()
     */
    private void deleteLibrary() {
        Library sel = tblLibs.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        if (!FxUtil.confirm(this, "Conferma", "Eliminare definitivamente la libreria \"" + sel.getNome() + "\"?"))
            return;

        try {
            boolean ok = libraryService.deleteLibrary(sel.getUserid(), sel.getNome());
            if (!ok) throw new IllegalStateException("Eliminazione fallita.");
            loadLibraries();
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }


    /**
     * Rimuove il libro selezionato dalla libreria selezionata.
     * <p>
     * Opera creando un nuovo insieme di ID e salvando la libreria aggiornata tramite {@link LibraryService}.
     * Dopo l'aggiornamento ricarica l'elenco librerie (e quindi anche la tabella libri).
     * </p>
     *
     * @see FxUtil#confirm(javafx.stage.Window, String, String)
     * @see LibraryService#saveLibrary(Library)
     * @see #loadLibraries()
     */
    private void removeBookFromLibrary() {
        Library lib = tblLibs.getSelectionModel().getSelectedItem();
        Book b = tblBooks.getSelectionModel().getSelectedItem();
        if (lib == null || b == null) return;

        if (!FxUtil.confirm(this, "Conferma", "Rimuovere \"" + b.getTitolo() + "\" dalla libreria \"" + lib.getNome() + "\"?"))
            return;

        try {
            Set<Integer> newSet = new HashSet<>(lib.getBookIds());
            newSet.remove(b.getId());
            Library updated = new Library(lib.getUserid(), lib.getNome(), newSet);

            boolean ok = libraryService.saveLibrary(updated);
            if (!ok) throw new IllegalStateException("Aggiornamento fallito.");

            loadLibraries();
        } catch (Exception e) {
            FxUtil.error(this, "Errore", e.getMessage());
        }
    }


    /**
     * Apre la finestra di gestione librerie come dialog modale
     * e blocca l'esecuzione fino alla sua chiusura.
     *
     * @param authService servizio di autenticazione per determinare
     * l'utente corrente
     * @param libraryService servizio di gestione delle librerie
     * @param repo repository dei libri, usato per risolvere gli ID
     * contenuti nelle librerie
     */
    public static void open(AuthService authService, LibraryService libraryService, LibriRepository repo) {
        LibrariesWindow w = new LibrariesWindow(authService, libraryService, repo);
        w.showAndWait();
    }


    /**
     * Mostra un dialog di input per ottenere un nome libreria e applica una validazione minima.
     * <p>
     * Ritorna la stringa inserita (trim) oppure stringa vuota se l'utente annulla.
     * In caso di lunghezza insufficiente mostra un errore.
     * </p>
     *
     * @return nome libreria inserito (trim) o stringa vuota
     * @see TextInputDialog
     */
    private String checkLibrary(){
        TextInputDialog d = new TextInputDialog();
        d.initOwner(this);
        d.setTitle("Nuova libreria");
        d.setHeaderText(null);
        d.setContentText("Nome libreria (min 5 caratteri):");
        Optional<String> r = d.showAndWait();
        String name = "";

        if (r.isPresent()) name = r.get().trim();
        if (name.length() < 6) {
            FxUtil.error(this, "Nome non valido", "Il nome deve avere almeno 5 caratteri.");
        }
        return name;
    }
}
