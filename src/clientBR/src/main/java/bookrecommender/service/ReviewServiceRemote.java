package bookrecommender.service;


import bookrecommender.model.Review;
import bookrecommender.net.Request;
import bookrecommender.net.RequestType;
import bookrecommender.net.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ReviewServiceRemote {

    private final String host;
    private final int port;

    public ReviewServiceRemote(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Lista recensioni dell'utente */
    @SuppressWarnings("unchecked")
    public List<Review> listByUser(String userid) {
        Response resp = send(new Request(RequestType.LIST_REVIEWS_BY_USER, userid, null));
        if (resp == null || !resp.ok) throw new RuntimeException(resp == null ? "Nessuna risposta" : resp.error);
        return (List<Review>) resp.data;
    }

    /** Salva (insert/update) recensione */
    public boolean inserisciValutazione(Review r) {
        Response resp = send(new Request(RequestType.SAVE_REVIEW, r, null));
        if (resp == null || !resp.ok) throw new RuntimeException(resp == null ? "Nessuna risposta" : resp.error);
        return (resp.data instanceof Boolean b) ? b : true;
    }

    /** Alias usato dalla UI quando modifica */
    public boolean updateReview(Review r) {
        return inserisciValutazione(r);
    }

    /** Elimina recensione */
    public boolean deleteReview(String userid, int bookId) {
        Object[] payload = new Object[]{ userid, bookId };
        Response resp = send(new Request(RequestType.DELETE_REVIEW, payload, null));
        if (resp == null || !resp.ok) throw new RuntimeException(resp == null ? "Nessuna risposta" : resp.error);
        return (resp.data instanceof Boolean b) ? b : true;
    }

    // ---- rete ----
    private Response send(Request req) {
        try (Socket s = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

            out.writeObject(req);
            out.flush();

            Object obj = in.readObject();
            return (obj instanceof Response r) ? r : Response.fail("Bad response");

        } catch (Exception e) {
            return Response.fail("Errore di rete: " + e.getMessage());
        }
    }
}
