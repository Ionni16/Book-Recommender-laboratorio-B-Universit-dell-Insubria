package bookrecommender.service;

import bookrecommender.model.Review;
import bookrecommender.net.Request;
import bookrecommender.net.Response;
import bookrecommender.net.BRProxy;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Servizio client per la gestione delle recensioni.
 * <p>
 * Questo servizio comunica con il server tramite {@link BRProxy} per:
 * </p>
 * <ul>
 *   <li>Ottenere le recensioni di un utente</li>
 *   <li>Inserire una nuova recensione</li>
 *   <li>Aggiornare una recensione esistente</li>
 *   <li>Eliminare una recensione</li>
 * </ul>
 *
 * <p>
 * I percorsi passati al costruttore sono attualmente ignorati e presenti
 * solo per compatibilità con una precedente architettura basata su file.
 * </p>
 *
 * @author Matteo Ferrario
 * @version 2.0
 * @see BRProxy
 * @see Request
 * @see Response
 * @see Review
 */
public class ReviewService {

    /** Proxy di rete usato per comunicare con il server. */
    private final BRProxy proxy;

    /**
     * Crea un nuovo servizio per la gestione delle recensioni.
     *
     * @param ignoredValutazioni percorso delle valutazioni (attualmente ignorato)
     * @param ignoredLibrerie percorso delle librerie (attualmente ignorato)
     */
    public ReviewService(Path ignoredValutazioni, Path ignoredLibrerie) {
        this.proxy = new BRProxy("127.0.0.1", 5050);
    }

    /**
     * Ritorna tutte le recensioni associate a un utente.
     *
     * @param userid userid dell'utente
     * @return lista delle recensioni dell'utente; se non ci sono dati ritorna una lista vuota
     * @throws RuntimeException se la risposta del server non è OK
     * @see Request#listReviewsByUser(String)
     */
    @SuppressWarnings("unchecked")
    public List<Review> listByUser(String userid) {
        Response res = proxy.call(Request.listReviewsByUser(userid));
        if (!res.ok) throw new RuntimeException(res.error);
        if (res.data == null) return Collections.emptyList();
        return (List<Review>) res.data;
    }


    /**
     * Ritorna tutte le recensioni associate a un libro (di tutti gli utenti).
     *
     * @param bookId id del libro
     * @return lista recensioni del libro; se non ci sono dati ritorna una lista vuota
     * @throws RuntimeException se la risposta del server non è OK
     * @see Request#getReviewsByBook(int)
     */
    @SuppressWarnings("unchecked")
    public List<Review> listByBook(int bookId) {
        Response res = proxy.call(Request.getReviewsByBook(bookId));
        if (!res.ok) throw new RuntimeException(res.error);
        if (res.data == null) return Collections.emptyList();
        return (List<Review>) res.data;
    }


    /**
     * Inserisce una nuova recensione sul server.
     *
     * @param r recensione da inserire
     * @return <code>true</code> se la richiesta va a buon fine e il server ritorna <code>true</code>
     *         oppure se <code>res.data</code> è <code>null</code>; <code>false</code> se <code>res.ok</code> è false
     * @see Request#saveReview(Review)
     */
    public boolean inserisciValutazione(Review r) {
        Response res = proxy.call(Request.saveReview(r));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }

    /**
     * Aggiorna una recensione esistente sul server.
     *
     * @param r recensione aggiornata
     * @return <code>true</code> se la richiesta va a buon fine e il server ritorna <code>true</code>
     *         oppure se <code>res.data</code> è <code>null</code>; <code>false</code> se <code>res.ok</code> è false
     * @see Request#saveReview(Review)
     */
    public boolean updateReview(Review r) {
        Response res = proxy.call(Request.saveReview(r));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }

    /**
     * Elimina una recensione associata a un utente e a un libro.
     *
     * @param userid userid dell'utente
     * @param bookId identificativo del libro
     * @return <code>true</code> se la richiesta va a buon fine e il server ritorna <code>true</code>
     *         oppure se <code>res.data</code> è <code>null</code>; <code>false</code> se <code>res.ok</code> è false
     * @see Request#deleteReview(String, int)
     */
    public boolean deleteReview(String userid, int bookId) {
        Response res = proxy.call(Request.deleteReview(userid, bookId));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }
}
