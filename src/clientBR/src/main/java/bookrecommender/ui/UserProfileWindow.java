package bookrecommender.ui;

import bookrecommender.model.User;
import bookrecommender.service.AuthService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Finestra modale per la gestione del profilo utente.
 * <p>
 * Permette all'utente autenticato di:
 * <ul>
 *     <li>Visualizzare i propri dati anagrafici (nome, cognome, codice fiscale, username);</li>
 *     <li>Aggiornare l'indirizzo email di contatto;</li>
 *     <li>Modificare la password con verifica di conferma;</li>
 *     <li>Eliminare definitivamente il proprio account.</li>
 * </ul>
 * I dati vengono letti e aggiornati tramite il {@link AuthService}.
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see bookrecommender.service.AuthService
 */
public class UserProfileWindow extends Stage {

    private final AuthService authService;

    /** Espressione regolare minimale per la validazione della sintassi dell'email. */
    private static final Pattern EMAIL_RX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /**
     * Costruisce e inizializza la finestra del profilo utente.
     * <p>
     * Nel costruttore vengono:
     * <ul>
     *     <li>Memorizzato il servizio di autenticazione;</li>
     *     <li>Configurati titolo e modalità modale della finestra;</li>
     *     <li>Costruiti header, contenuto centrale e footer;</li>
     *     <li>Applicato il foglio di stile <code>app.css</code>.</li>
     * </ul>
     *
     * @param authService servizio di autenticazione usato per recuperare
     *                    e modificare i dati dell'utente corrente
     */
    public UserProfileWindow(AuthService authService) {
        this.authService = authService;

        setTitle("Account");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-bg");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(FxUtil.buildFooter(""));

        Scene scene = new Scene(new StackPane(root), 820, 560);

        URL css = getClass().getResource("/bookrecommender/ui/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("CSS NON trovato: /bookrecommender/ui/app.css");
        }

        setScene(scene);
    }


    /**
     * Costruisce l'header grafico della finestra profilo.
     * <p>
     * Contiene titolo e sottotitolo e applica lo stile CSS dell'appbar.
     * </p>
     *
     * @return nodo JavaFX per la sezione header
     */
    private Node buildHeader() {
        Label h = new Label("Account");
        h.getStyleClass().add("title");

        Label sub = new Label("Gestisci email, password e sicurezza dell’account");
        sub.getStyleClass().add("subtitle");

        VBox box = new VBox(4, h, sub);
        box.getStyleClass().add("appbar");
        return box;
    }


    /**
     * Costruisce il contenuto centrale della finestra profilo.
     * <p>
     * Struttura principale:
     * </p>
     * <ul>
     *   <li>Card con dati personali (sola lettura)</li>
     *   <li>Card email: validazione e salvataggio immediato</li>
     *   <li>Card password: cambio password con conferma</li>
     *   <li>"Zona pericolosa": eliminazione account con conferma</li>
     * </ul>
     *
     * <p>
     * Il contenuto è inserito in uno {@link ScrollPane} per supportare finestre piccole.
     * Viene inoltre forzata la trasparenza della viewport via {@link Platform#runLater(Runnable)}
     * per compatibilità con alcune versioni JavaFX che ignorano lo stile CSS.
     * </p>
     *
     * @return nodo JavaFX centrale (scrollable) della finestra
     * @throws RuntimeException se operazioni remote falliscono e vengono propagate (dipende dai metodi di servizio)
     * @see AuthService#getCurrentUserid()
     * @see AuthService#getUser(String)
     * @see AuthService#updateEmail(String, String)
     * @see AuthService#updatePassword(String, String)
     * @see AuthService#deleteUser(String)
     * @see FxUtil#toast(Scene, String)
     * @see FxUtil#error(javafx.stage.Window, String, String)
     * @see FxUtil#confirm(javafx.stage.Window, String, String)
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private Node buildCenter() {
        String userId = authService.getCurrentUserid();
        if (userId == null) {
            VBox v = new VBox(10, new Label("Login richiesto."));
            v.getStyleClass().add("card");
            v.setPadding(new Insets(14));
            FxUtil.wrapCard(v);
        }

        User u;
        try {
            u = authService.getUser(userId);
        } catch (Exception e) {
            FxUtil.error(this, "Errore", "Impossibile leggere i dati utente: " + e.getMessage());
            u = null;
        }

        VBox page = new VBox(14);
        page.setPadding(new Insets(14));
        page.setFillWidth(true);

        // ---- Card: dati personali ----
        VBox info = new VBox(8);
        info.getStyleClass().add("card2");
        info.setPadding(new Insets(14));

        Label t1 = new Label("Dati personali");
        t1.getStyleClass().add("card-title");

        Label nome = muted("Nome: " + (u == null ? "-" : safe(u.getNome())));
        Label cognome = muted("Cognome: " + (u == null ? "-" : safe(u.getCognome())));
        Label cf = muted("Codice Fiscale: " + (u == null ? "-" : safe(u.getCodiceFiscale())));
        Label user = muted("Username: " + (u == null ? "-" : safe(u.getUserid())));

        info.getChildren().addAll(t1, nome, cognome, cf, user);

        // ---- Card: email ----
        VBox emailCard = new VBox(10);
        emailCard.getStyleClass().add("card");
        emailCard.setPadding(new Insets(14));

        Label t2 = new Label("Email");
        t2.getStyleClass().add("card-title");
        Label t2sub = muted("Aggiorna la tua email di contatto. Verrà salvata subito.");

        TextField email = new TextField(u == null ? "" : safe(u.getEmail()));
        email.setPromptText("email@dominio.it");

        Button saveEmail = new Button("Aggiorna Email");
        saveEmail.getStyleClass().add("primary");
        saveEmail.setOnAction(e -> {
            try {
                String em = safe(email.getText());
                if (!EMAIL_RX.matcher(em).matches())
                    throw new IllegalArgumentException("Email non valida.");

                boolean ok = authService.updateEmail(userId, em);
                if (!ok) throw new IllegalStateException("Aggiornamento fallito.");

                // (opzionale ma consigliato) aggiorna anche l'utente in memoria:
                User current = authService.getUser(userId);
                if (current != null) {
                    User updated = new User(
                            current.getUserid(),
                            current.getPasswordHash(),
                            current.getNome(),
                            current.getCognome(),
                            current.getCodiceFiscale(),
                            em
                    );
                    authService.updateUser(updated); // solo per UI, ora ha senso
                }

                FxUtil.toast(getScene(), "Email aggiornata");

            } catch (Exception ex) {
                ex.printStackTrace();
                FxUtil.error(this, "Errore", ex.getMessage());
            }
        });


        HBox emailActions = new HBox(saveEmail);
        emailActions.setAlignment(Pos.CENTER_RIGHT);

        emailCard.getChildren().addAll(t2, t2sub, email, emailActions);

        // ---- Card: password ----
        VBox pwCard = new VBox(10);
        pwCard.getStyleClass().add("card");
        pwCard.setPadding(new Insets(14));

        Label t3 = new Label("Password");
        t3.getStyleClass().add("card-title");
        Label t3sub = muted("Scegli una password forte (min 8 caratteri).");

        PasswordManager pw1 = new PasswordManager("Nuova password");
        PasswordManager pw2 = new PasswordManager("Ripeti nuova password");

        Button savePw = new Button("Cambia Password");
        savePw.getStyleClass().add("primary");
        savePw.setOnAction(e -> {
            try {
                String a = pw1.getText();
                String b = pw2.getText();
                if (a == null || a.isBlank() || b == null || b.isBlank())
                    throw new IllegalArgumentException("Inserisci entrambe le password.");
                if (!Objects.equals(a, b))
                    throw new IllegalArgumentException("Le password non corrispondono.");
                if (a.length() < 8)
                    throw new IllegalArgumentException("Password troppo corta (min 8).");

                boolean ok = authService.updatePassword(userId, a);
                if (!ok) throw new IllegalStateException("Password non aggiornata.");

                pw1.clear();
                pw2.clear();
                FxUtil.toast(getScene(), "Password aggiornata");
            } catch (Exception ex) {
                FxUtil.error(this, "Errore", ex.getMessage());
            }
        });

        HBox pwActions = new HBox(savePw);
        pwActions.setAlignment(Pos.CENTER_RIGHT);

        pwCard.getChildren().addAll(t3, t3sub, pw1.getNode(), pw2.getNode(), pwActions);

        // ---- Card: danger zone ----
        VBox danger = new VBox(10);
        danger.getStyleClass().add("card2");
        danger.setPadding(new Insets(14));

        Label t4 = new Label("Zona pericolosa");
        t4.getStyleClass().add("card-title");
        Label t4sub = muted("Elimina definitivamente l’account. Operazione irreversibile.");

        Button deleteAcc = new Button("Elimina account");
        deleteAcc.getStyleClass().add("danger");
        deleteAcc.setOnAction(e -> {
            if (!FxUtil.confirm(this, "Conferma", "Eliminare definitivamente l'account? Operazione irreversibile."))
                return;
            try {
                boolean ok = authService.deleteUser(userId);
                if (!ok) throw new IllegalStateException("Eliminazione fallita.");
                authService.logout();
                FxUtil.info(this, "Account eliminato", "L’account è stato eliminato e sei stato disconnesso.");
                close();
            } catch (Exception ex) {
                FxUtil.error(this, "Errore", ex.getMessage());
            }
        });

        HBox dangerActions = new HBox(deleteAcc);
        dangerActions.setAlignment(Pos.CENTER_RIGHT);

        danger.getChildren().addAll(t4, t4sub, dangerActions);

        page.getChildren().addAll(info, emailCard, pwCard, danger);

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // classe CSS che rende trasparente anche la viewport
        sp.getStyleClass().add("scroll-clean");

        // fallback: forza viewport trasparente via codice (alcune versioni JavaFX ignorano il CSS)
        Platform.runLater(() -> {
            Node viewport = sp.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-color: transparent;");
            }
        });

        return sp;
    }


    /**
     * Crea una {@link Label} con stile "muted" e wrap attivo, usata per testi secondari.
     *
     * @param text testo da visualizzare
     * @return label stilizzata
     */
    private static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }


    /**
     * Normalizza una stringa: se <code>null</code> ritorna stringa vuota,
     * altrimenti ritorna <code>trim()</code>.
     *
     * @param s stringa in input
     * @return stringa normalizzata
     */
    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }


    /**
     * Apre la finestra di gestione profilo come dialog modale e
     * blocca l'esecuzione finché l'utente non la chiude.
     *
     * @param authService servizio di autenticazione usato per accedere
     *                    ai dati dell'utente corrente
     */
    public static void open(AuthService authService) {
        UserProfileWindow w = new UserProfileWindow(authService);
        w.showAndWait();
    }
}
