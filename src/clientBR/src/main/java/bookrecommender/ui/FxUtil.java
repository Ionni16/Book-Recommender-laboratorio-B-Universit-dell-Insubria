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
 * Fornisce metodi statici per mostrare finestre di dialogo
 * (informazioni, errori, conferme) e un semplice messaggio
 * di tipo "toast" nella scena corrente.
 *
 * @author Matteo Ferrario
 * @version 1.0
 */
public final class FxUtil {

    /**
     * Costruttore privato per impedire l'istanza della classe.
     * <p>
     * Tutte le funzionalità sono esposte tramite metodi statici.
     */
    private FxUtil() {}

    /**
     * Mostra una finestra di dialogo informativa con pulsante OK.
     *
     * @param owner   finestra proprietaria della dialog (può essere
     *                <code>null</code> se non si vuole impostare un owner)
     * @param title   titolo della finestra di dialogo
     * @param message messaggio informativo da visualizzare
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
     * @param owner   finestra proprietaria della dialog (può essere
     *                <code>null</code> se non si vuole impostare un owner)
     * @param title   titolo della finestra di dialogo
     * @param message messaggio di errore da visualizzare
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
     * @param owner   finestra proprietaria della dialog (può essere
     *                <code>null</code> se non si vuole impostare un owner)
     * @param title   titolo della finestra di dialogo
     * @param message messaggio di conferma da visualizzare
     * @return {@code true} se l'utente ha selezionato OK,
     *         {@code false} in caso contrario (Annulla o chiusura della dialog)
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
     * Mostra un semplice messaggio di tipo "toast" in basso al centro della scena.
     * <p>
     * Il metodo presuppone che il <code>root</code> della scena sia uno
     * {@link StackPane}, in modo da poter sovrapporre il messaggio rispetto
     * ai contenuti esistenti.
     *
     * @param scene   scena su cui visualizzare il toast; se <code>null</code>
     *                o se il <code>root</code> non è uno {@link StackPane},
     *                il metodo non esegue alcuna azione
     * @param message testo del messaggio da mostrare
     */
    public static void toast(Scene scene, String message) {
        if (scene == null) return;
        if (!(scene.getRoot() instanceof StackPane stack)) return;

        Label chip = new Label(message);
        chip.getStyleClass().add("snackbar");

        StackPane.setAlignment(chip, Pos.BOTTOM_CENTER);
        stack.getChildren().add(chip);

        PauseTransition pt = new PauseTransition(Duration.seconds(2.2));
        pt.setOnFinished(e -> stack.getChildren().remove(chip));
        pt.playFromStart();
    }

    public static <S> void addColumns(TableView<S> table, List<TableColumn<S, ?>> cols) {
        table.getColumns().addAll(cols);
    }

    public static HBox buildReloadDeleteBar(
            TableView<?> table,
            String reloadText,
            Runnable onReload,
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

        HBox actions = new HBox(10, btnReload, btnDelete);
        actions.setAlignment(Pos.CENTER_RIGHT);
        return actions;
    }

    public static BorderPane wrapCard(VBox card) {
        BorderPane wrap = new BorderPane(card);
        wrap.setPadding(new Insets(14));
        return wrap;
    }


    public static Node buildFooter() {
        Label hint = new Label("Suggerimento: usa l’icona 👁 per vedere la password mentre scrivi.");
        hint.getStyleClass().add("muted");

        Button close = new Button("Chiudi");
        close.getStyleClass().add("ghost");
        close.setOnAction(e -> close.getScene().getWindow().hide());

        HBox bar = new HBox(10, hint, new Pane(), close);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.getStyleClass().add("statusbar");
        return bar;
    }


}
