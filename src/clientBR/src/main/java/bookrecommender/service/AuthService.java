package bookrecommender.service;

import bookrecommender.model.User;
import bookrecommender.net.Request;
import bookrecommender.net.RequestType;
import bookrecommender.net.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class AuthService {

    private final String host;
    private final int port;

    private User currentUser;

    public AuthService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Ritorna true se login ok e imposta currentUser */
    public boolean login(String userid, String password) {
        if (userid == null || password == null) return false;

        // ✅ INVIA PASSWORD IN CHIARO: il server calcola hash e confronta
        String uid = userid.trim();
        String pw  = password; // NON hashare qui

        Response resp = send(new Request(RequestType.LOGIN, new String[]{uid, pw}, null));
        if (resp == null || !resp.ok) {
            // se il server manda un messaggio, usalo
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

    /** Registra l'utente e, se ok, prova login automatico */
    public boolean registrazione(User u) {
        if (u == null) return false;

        String userid = (u.getUserid() == null) ? "" : u.getUserid().trim();
        String pass   = (u.getPasswordHash() == null) ? "" : u.getPasswordHash(); // qui la UI mette la password

        // ✅ INVIA PASSWORD IN CHIARO dentro passwordHash: il server la normalizza in SHA-256 e salva
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

        // login automatico con password in chiaro
        return login(userid, pass);
    }
    

    public void logout() {
        send(Request.logout());
        currentUser = null;
    }

    public String getCurrentUserid() {
        return (currentUser == null) ? null : currentUser.getUserid();
    }

    public User getUser(String userid) {
        if (currentUser != null && currentUser.getUserid().equals(userid)) return currentUser;
        return null;
    }

    public boolean updateUser(User u) {
        if (u != null && currentUser != null && currentUser.getUserid().equals(u.getUserid())) {
            currentUser = u;
        }
        return true;
    }

    public boolean updatePassword(String userid, String newPassword) {
        if (userid == null || userid.isBlank()) return false;
        if (newPassword == null || newPassword.isBlank()) return false;

        // inviamo password in chiaro: il server la hasha come fa già per login/register
        Object[] payload = new Object[]{ userid.trim(), newPassword };

        Response resp = send(new Request(RequestType.CHANGE_PASSWORD, payload, null));
        if (resp == null || !resp.ok) {
            if (resp != null && resp.error != null) throw new RuntimeException(resp.error);
            throw new RuntimeException("Cambio password fallito");
        }

        // se il server ritorna true/false
        if (resp.data instanceof Boolean b) return b;

        return true;
    }


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


    // ------------------ rete ------------------
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

    public boolean updateEmail(String userid, String newEmail) {
        if (userid == null || userid.isBlank()) return false;
        if (newEmail == null || newEmail.isBlank()) return false;

        Object[] payload = new Object[]{ userid.trim(), newEmail.trim() };

        Response resp = send(new Request(RequestType.UPDATE_EMAIL, payload, null));
        if (resp == null || !resp.ok) {
            if (resp != null && resp.error != null) throw new RuntimeException(resp.error);
            throw new RuntimeException("Aggiornamento email fallito");
        }

        if (resp.data instanceof Boolean b) return b;
        return true;
    }

}
