package bookrecommender.ui;


import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

/**
 * Componente UI riutilizzabile per l'inserimento di password con funzionalità
 * di visibilità (mostra/nascondi).
 * <p>
 * Internamente mantiene due controlli sincronizzati:
 * <ul>
 *   <li>{@link PasswordField} per la modalità "nascosta"</li>
 *   <li>{@link TextField} per la modalità "visibile"</li>
 * </ul>
 * Il testo è condiviso tramite binding bidirezionale. Il toggle avviene
 * mostrando/nascondendo i nodi e aggiornando anche {@code managed} per evitare
 * che il layout riservi spazio al controllo non attivo.
 *
 * @author Matteo Ferrario
 */
public class PasswordManager {
    public final PasswordField pf = new PasswordField();
    public final TextField tf = new TextField();
    public final HBox root;


    /**
     * Costruisce il componente impostando il prompt su entrambi i campi e
     * configurando il pulsante di toggle.
     *
     * @param prompt testo da visualizzare come placeholder nei campi
     */
    public PasswordManager(String prompt) {
        pf.setPromptText(prompt);
        tf.setPromptText(prompt);

        tf.textProperty().bindBidirectional(pf.textProperty());

        tf.setVisible(false);
        tf.setManaged(false);

        Button eye = new Button("👁");
        eye.getStyleClass().add("ghost");
        eye.getStyleClass().add("icon");
        eye.setFocusTraversable(false);

        eye.setOnAction(e -> {
            boolean showing = tf.isVisible();
            if (showing) {
                tf.setVisible(false);
                tf.setManaged(false);
                pf.setVisible(true);
                pf.setManaged(true);
            } else {
                pf.setVisible(false);
                pf.setManaged(false);
                tf.setVisible(true);
                tf.setManaged(true);
            }
        });

        StackPane stack = new StackPane(pf, tf);
        HBox.setHgrow(stack, Priority.ALWAYS);

        root = new HBox(10, stack, eye);
        root.setAlignment(Pos.CENTER_LEFT);
    }


    /**
     * Restituisce il nodo radice da inserire nel layout.
     *
     * @return nodo grafico del componente
     */
    Node getNode() { return root; }


    /**
     * Restituisce il testo corrente inserito dall'utente.
     * <p>
     * Il valore è identico in modalità visibile e nascosta perché i due campi
     * sono sincronizzati.
     *
     * @return testo inserito
     */
    String getText() { return pf.getText(); }


    /**
     * Svuota il contenuto del campo password (e quindi anche del campo visibile).
     */
    void clear() { pf.clear(); }


    /**
     * Analizza la stringa inserita per verificare la robustezza della password
     * secondo dei minimi criteri di sicurezza
     *
     * @param pw password inserita.
     * @return {@code true} se rispetta le condizioni,
     *         {@code false} se non rispettati.
     */
    public static boolean isStrongPassword(String pw) {
        if (pw == null || pw.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : pw.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

}