package bookrecommender.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;

/**
 * Classe di utilità per operazioni comuni dell'interfaccia JavaFX.
 * <p>
 * Fornisce metodi statici per:
 * </p>
 * <ul>
 *   <li>Mostrare dialog (info/errore/conferma)</li>
 *   <li>Mostrare un messaggio temporaneo tipo "toast" nella {@link Scene}</li>
 *   <li>Aggiungere colonne a una {@link TableView}</li>
 *   <li>Costruire barre azioni e wrapper grafici riutilizzabili</li>
 * </ul>
 *
 * @author Matteo Ferrario
 * @version 2.0
 * @see Alert
 * @see TableView
 * @see Scene
 */
public final class FxUtil {

    /**
     * Costruttore privato per impedire l'istanza della classe.
     * <p>
     * Tutte le funzionalità sono esposte tramite metodi statici.
     * </p>
     */
    private FxUtil() {}


    /**
     * Mostra una finestra di dialogo informativa con pulsante OK.
     *
     * @param owner finestra proprietaria della dialog (può essere <code>null</code>)
     * @param title titolo della finestra di dialogo
     * @param message messaggio informativo da visualizzare
     * @see Alert.AlertType#INFORMATION
     */
    public static void info(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }


    /**
     * Mostra una finestra di dialogo di errore con pulsante OK.
     *
     * @param owner finestra proprietaria della dialog (può essere <code>null</code>)
     * @param title titolo della finestra di dialogo
     * @param message messaggio di errore da visualizzare
     * @see Alert.AlertType#ERROR
     */
    public static void error(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }


    /**
     * Mostra una finestra di dialogo di conferma con pulsanti OK e Annulla.
     *
     * @param owner finestra proprietaria della dialog (può essere <code>null</code>)
     * @param title titolo della finestra di dialogo
     * @param message messaggio di conferma da visualizzare
     * @return <code>true</code> se l'utente seleziona OK; <code>false</code> in caso di Annulla o chiusura dialog
     * @see Alert.AlertType#CONFIRMATION
     */
    public static boolean confirm(Window owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }


    /**
     * Mostra un messaggio temporaneo tipo "toast" (snack-bar) in basso al centro della scena.
     * <p>
     * Presuppone che il root della {@link Scene} sia uno {@link StackPane}, così il messaggio
     * può essere sovrapposto ai contenuti esistenti.
     * Se la scena è <code>null</code> o il root non è uno {@link StackPane}, il metodo non fa nulla.
     * </p>
     *
     * @param scene scena su cui visualizzare il toast
     * @param message testo del messaggio da mostrare
     * @see PauseTransition
     * @see Duration
     */
    public static void toast(Scene scene, String message) {
        if (scene == null) return;
        if (!(scene.getRoot() instanceof StackPane stack)) return;

        Label chip = new Label(message);
        chip.getStyleClass().add("snack-bar");

        StackPane.setAlignment(chip, Pos.BOTTOM_CENTER);
        stack.getChildren().add(chip);

        PauseTransition pt = new PauseTransition(Duration.seconds(2.2));
        pt.setOnFinished(e -> stack.getChildren().remove(chip));
        pt.playFromStart();
    }


    /**
     * Aggiunge in modo comodo una lista di colonne a una tabella.
     *
     * @param table tabella target
     * @param cols colonne da aggiungere
     * @param <S> tipo di riga (model) della tabella
     * @see TableView#getColumns()
     */
    public static <S> void addColumns(TableView<S> table, List<TableColumn<S, ?>> cols) {
        table.getColumns().addAll(cols);
    }


    /**
     * Costruisce una barra azioni con due pulsanti: ricarica ed elimina.
     * <p>
     * Il pulsante "delete" viene disabilitato automaticamente quando non è selezionata alcuna riga
     * nella tabella.
     * </p>
     *
     * @param table tabella associata (usata per abilitare/disabilitare il delete in base alla selezione)
     * @param reloadText testo del pulsante di ricarica
     * @param onReload azione eseguita al click di ricarica
     * @param deleteText testo del pulsante di eliminazione
     * @param onDelete azione eseguita al click di eliminazione
     * @return contenitore {@link HBox} con i pulsanti azione
     */
    public static HBox buildReloadUpdateDeleteBar(
            TableView<?> table,
            String reloadText,
            Runnable onReload,
            String updateText,
            Runnable onUpdate,
            String deleteText,
            Runnable onDelete

    ) {
        Button btnDelete = new Button(deleteText);
        btnDelete.getStyleClass().add("danger");
        btnDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.setOnAction(e -> onDelete.run());

        Button btnReload = new Button(reloadText);
        btnReload.getStyleClass().add("ghost");
        btnReload.setOnAction(e -> onReload.run());

        Button btnUpdate = new Button(updateText);
        btnUpdate.getStyleClass().add("success");
        btnDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        btnUpdate.setOnAction(e -> onUpdate.run());

        HBox actions = new HBox(10, btnReload, btnUpdate, btnDelete);
        actions.setAlignment(Pos.CENTER_RIGHT);
        return actions;
    }


    /**
     * Avvolge una card (VBox) in un {@link BorderPane} aggiungendo padding esterno.
     *
     * @param card contenuto principale della card
     * @return wrapper con padding pronto per essere usato come root di una scena o contenuto di un dialog
     */
    public static BorderPane wrapCard(VBox card) {
        BorderPane wrap = new BorderPane(card);
        wrap.setPadding(new Insets(14));
        return wrap;
    }


    /**
     * Crea un footer riutilizzabile con messaggio di hint e pulsante di chiusura finestra.
     * <p>
     * Il pulsante "Chiudi" chiude la finestra corrente tramite:
     * <code>close.getScene().getWindow().hide()</code>.
     * </p>
     *
     * @return nodo JavaFX contenente il footer
     */
    public static Node buildFooter(String hintText) {
        Label hint = new Label(hintText == null ? "" : hintText);
        hint.getStyleClass().add("muted");

        Button close = new Button("Chiudi");
        close.getStyleClass().add("ghost");
        close.setOnAction(e -> close.getScene().getWindow().hide());

        HBox bar = new HBox(10, hint, new Pane(), close);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("status-bar");
        return bar;
    }
}
