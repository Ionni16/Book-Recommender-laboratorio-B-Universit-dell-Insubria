package bookrecommender.service;

import bookrecommender.model.Suggestion;
import bookrecommender.net.Request;
import bookrecommender.net.Response;
import bookrecommender.net.BRProxy;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Servizio client per la gestione dei suggerimenti.
 * <p>
 * Questo servizio comunica con il server remoto tramite {@link BRProxy} per:
 * </p>
 * <ul>
 *   <li>Inserire un suggerimento</li>
 *   <li>Elencare i suggerimenti associati a un utente</li>
 *   <li>Eliminare un suggerimento</li>
 * </ul>
 *
 * <p>
 * I parametri {@link Path} passati al costruttore sono attualmente ignorati
 * e presenti solo per compatibilità con una precedente architettura basata su file.
 * </p>
 *
 * @author Matteo Ferrario
 * @version 2.0
 * @see BRProxy
 * @see Request
 * @see Response
 * @see Suggestion
 */
public class SuggestionService {

    /** Proxy di rete usato per comunicare con il server. */
    private final BRProxy proxy;

    /**
     * Crea un servizio per la gestione dei suggerimenti.
     *
     * @param ignoredConsigli percorso dei suggerimenti (attualmente ignorato)
     * @param ignoredLibrerie percorso delle librerie (attualmente ignorato)
     */
    public SuggestionService(Path ignoredConsigli, Path ignoredLibrerie) {
        this.proxy = new BRProxy("127.0.0.1", 5050);
    }

    /**
     * Inserisce un nuovo suggerimento sul server.
     *
     * @param s suggerimento da inserire
     * @return <code>true</code> se la richiesta va a buon fine e il server ritorna <code>true</code>
     *         oppure se <code>res.data</code> è <code>null</code>;
     *         <code>false</code> se <code>res.ok</code> è false
     * @see Request#saveSuggestion(Suggestion)
     */
    public boolean inserisciSuggerimento(Suggestion s) {
        Response res = proxy.call(Request.saveSuggestion(s));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }

    /**
     * Ritorna tutti i suggerimenti associati a un utente.
     *
     * @param userid userid dell'utente
     * @return lista dei suggerimenti dell'utente; se la risposta non contiene dati ritorna una lista vuota
     * @throws RuntimeException se la risposta del server non è OK
     * @see Request#listSuggestionsByUser(String)
     */
    @SuppressWarnings("unchecked")
    public List<Suggestion> listByUser(String userid) {
        Response res = proxy.call(Request.listSuggestionsByUser(userid));
        if (!res.ok) throw new RuntimeException(res.error);
        if (res.data == null) return Collections.emptyList();
        return (List<Suggestion>) res.data;
    }

    /**
     * Elimina un suggerimento associato a un utente e a un libro.
     *
     * @param userid userid dell'utente
     * @param bookId identificativo del libro suggerito
     * @return <code>true</code> se la richiesta va a buon fine e il server ritorna <code>true</code>
     *         oppure se <code>res.data</code> è <code>null</code>;
     *         <code>false</code> se <code>res.ok</code> è false
     * @see Request#deleteSuggestion(String, int)
     */
    public boolean deleteSuggestion(String userid, int bookId) {
        Response res = proxy.call(Request.deleteSuggestion(userid, bookId));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }
}
