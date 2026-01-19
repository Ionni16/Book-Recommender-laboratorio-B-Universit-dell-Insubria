package bookrecommender.service;

import bookrecommender.model.User;
import bookrecommender.net.Request;
import bookrecommender.net.RequestType;
import bookrecommender.net.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Servizio client per autenticazione e gestione account.
 * <p>
 * Comunica con un server remoto tramite {@link Socket} scambiando oggetti serializzati:
 * invia {@link Request} e riceve {@link Response}.
 * Mantiene lo stato dell'utente attualmente autenticato in <code>currentUser</code>.
 * </p>
 *
 * @author Matteo Ferrario
 * @version 2.0
 * @see Request
 * @see Response
 * @see RequestType
 */
public class AuthService {

    /** Host del server a cui connettersi. */
    private final String host;

    /** Porta TCP del server a cui connettersi. */
    private final int port;

    /** Utente autenticato corrente, oppure <code>null</code> se non autenticato. */
    private User currentUser;

    /**
     * Crea un servizio di autenticazione configurato su un host/porta.
     *
     * @param host host o indirizzo del server
     * @param port porta TCP del server
     */
    public AuthService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Esegue il login sul server.
     * <p>
     * Se l'operazione va a buon fine e la risposta contiene un {@link User},
     * imposta <code>currentUser</code> e ritorna <code>true</code>.
     * </p>
     *
     * @param userid identificativo utente
     * @param password password fornita dall'utente
     * @return <code>true</code> se login ok e <code>currentUser</code> viene impostato, altrimenti <code>false</code>
     * @throws RuntimeException se il server risponde con errore o se la richiesta fallisce
     * @see RequestType#LOGIN
     */
    public boolean login(String userid, String password) {
        if (userid == null || password == null) return false;

        String uid = userid.trim();

        Response resp = send(new Request(RequestType.LOGIN, new String[]{uid, password}, null));
        if (resp == null || !resp.ok) {
            if (resp != null && resp.error != null) throw new RuntimeException(resp.error);
            throw new RuntimeException("Login fallito");
        }

        if (resp.data instanceof User u) {
            currentUser = u;
            return true;
        }

        currentUser = null;
        return false;
    }

    /**
     * Registra un utente sul server e, se la registrazione va a buon fine, tenta un login automatico.
     *
     * @param u utente da registrare
     * @return <code>true</code> se registrazione e login automatico vanno a buon fine, altrimenti <code>false</code>
     * @throws RuntimeException se il server risponde con errore o se la richiesta fallisce
     * @see RequestType#REGISTER
     */
    public boolean registrazione(User u) {
        if (u == null) return false;

        String userid = (u.getUserid() == null) ? "" : u.getUserid().trim();
        String pass = (u.getPasswordHash() == null) ? "" : u.getPasswordHash();

        User toSend = new User(
                userid,
                pass,
                u.getNome(),
                u.getCognome(),
                u.getCodiceFiscale(),
                u.getEmail()
        );

        Response resp = send(new Request(RequestType.REGISTER, toSend, null));
        if (resp == null || !resp.ok) {
            if (resp != null && resp.error != null) throw new RuntimeException(resp.error);
            throw new RuntimeException("Registrazione fallita");
        }

        return login(userid, pass);
    }

    /**
     * Esegue il logout: invia una richiesta di logout al server e resetta lo stato locale.
     *
     * @see Request#logout()
     */
    public void logout() {
        send(Request.logout());
        currentUser = null;
    }

    /**
     * Ritorna lo userid dell'utente corrente.
     *
     * @return userid dell'utente autenticato, oppure <code>null</code> se non autenticato
     */
    public String getCurrentUserid() {
        return (currentUser == null) ? null : currentUser.getUserid();
    }

    /**
     * Ritorna l'utente corrente se lo userid richiesto coincide con quello autenticato.
     *
     * @param userid userid dell'utente richiesto
     * @return {@link User} se corrisponde all'utente corrente, altrimenti <code>null</code>
     */
    public User getUser(String userid) {
        if (currentUser != null && currentUser.getUserid().equals(userid)) return currentUser;
        return null;
    }

    /**
     * Aggiorna lo stato locale dell'utente corrente se lo userid coincide.
     *
     * @param u nuovo oggetto utente
     */
    public void updateUser(User u) {
        if (u != null && currentUser != null && currentUser.getUserid().equals(u.getUserid())) {
            currentUser = u;
        }
    }

    /**
     * Richiede al server il cambio password.
     *
     * @param userid userid dell'utente
     * @param newPassword nuova password
     * @return <code>true</code> se la richiesta va a buon fine, oppure il boolean ritornato dal server se presente
     * @throws RuntimeException se il server risponde con errore o se la richiesta fallisce
     * @see RequestType#CHANGE_PASSWORD
     */
    public boolean updatePassword(String userid, String newPassword) {
        if (userid == null || userid.isBlank()) return false;
        if (newPassword == null || newPassword.isBlank()) return false;

        Object[] payload = new Object[]{userid.trim(), newPassword};

        Response resp = send(new Request(RequestType.CHANGE_PASSWORD, payload, null));
        if (resp == null || !resp.ok) {
            if (resp != null && resp.error != null) throw new RuntimeException(resp.error);
            throw new RuntimeException("Cambio password fallito");
        }

        if (resp.data instanceof Boolean b) return b;

        return true;
    }

    /**
     * Richiede al server l'eliminazione dell'utente.
     * <p>
     * Se l'eliminazione va a buon fine e lo userid coincide con l'utente corrente,
     * azzera <code>currentUser</code>.
     * </p>
     *
     * @param userid userid dell'utente da eliminare
     * @return <code>true</code> se la richiesta va a buon fine, oppure il boolean ritornato dal server se presente
     * @throws RuntimeException se il server risponde con errore o se la richiesta fallisce
     * @see Request#deleteAccount(String)
     */
    public boolean deleteUser(String userid) {
        if (userid == null || userid.isBlank()) return false;

        Response resp = send(Request.deleteAccount(userid.trim()));
        if (resp == null || !resp.ok) {
            if (resp != null && resp.error != null) throw new RuntimeException(resp.error);
            throw new RuntimeException("Eliminazione account fallita");
        }

        boolean ok = (resp.data instanceof Boolean b) ? b : true;
        if (ok && currentUser != null && userid.trim().equals(currentUser.getUserid())) {
            currentUser = null;
        }
        return ok;
    }

    /**
     * Invia una richiesta al server e ritorna la risposta.
     * <p>
     * Apre una nuova connessione per ogni invocazione, invia l'oggetto {@link Request} e legge un oggetto
     * che dovrebbe essere una {@link Response}.
     * </p>
     *
     * @param req richiesta da inviare
     * @return risposta del server; in caso di errore ritorna una risposta di fallimento
     */
    private Response send(Request req) {
        try (Socket s = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

            out.writeObject(req);
            out.flush();

            Object obj = in.readObject();
            if (obj instanceof Response r) return r;
            return Response.fail("Bad response");

        } catch (Exception e) {
            return Response.fail("Errore di rete: " + e.getMessage());
        }
    }

    /**
     * Richiede al server l'aggiornamento dell'email.
     *
     * @param userid userid dell'utente
     * @param newEmail nuova email
     * @return <code>true</code> se la richiesta va a buon fine, oppure il boolean ritornato dal server se presente
     * @throws RuntimeException se il server risponde con errore o se la richiesta fallisce
     * @see RequestType#UPDATE_EMAIL
     */
    public boolean updateEmail(String userid, String newEmail) {
        if (userid == null || userid.isBlank()) return false;
        if (newEmail == null || newEmail.isBlank()) return false;

        Object[] payload = new Object[]{userid.trim(), newEmail.trim()};

        Response resp = send(new Request(RequestType.UPDATE_EMAIL, payload, null));
        if (resp == null || !resp.ok) {
            if (resp != null && resp.error != null) throw new RuntimeException(resp.error);
            throw new RuntimeException("Aggiornamento email fallito");
        }

        if (resp.data instanceof Boolean b) return b;
        return true;
    }

}
