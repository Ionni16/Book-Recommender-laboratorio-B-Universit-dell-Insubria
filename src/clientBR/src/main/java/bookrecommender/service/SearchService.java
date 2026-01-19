package bookrecommender.service;

import bookrecommender.model.Book;
import bookrecommender.net.BRProxy;
import bookrecommender.net.Request;
import bookrecommender.net.Response;
import bookrecommender.repo.LibriRepository;

import java.util.Collections;
import java.util.List;

/**
 * Servizio client per la ricerca dei libri.
 * <p>
 * Questo servizio delega al server remoto le operazioni di ricerca
 * (per titolo e per autore) tramite {@link BRProxy}.
 * </p>
 *
 * <p>
 * Il {@link LibriRepository} passato al costruttore viene ignorato ed è presente
 * esclusivamente per compatibilità con una precedente versione dell'interfaccia
 * grafica (LabA).
 * </p>
 *
 * @author Matteo Ferrario
 * @version 2.0
 * @see BRProxy
 * @see Request
 * @see Response
 * @see Book
 */
public class SearchService {

    /** Proxy di rete usato per comunicare con il server. */
    private final BRProxy proxy;

    /**
     * Crea un servizio di ricerca libri.
     *
     * @param ignored repository dei libri, attualmente ignorato
     */
    public SearchService(LibriRepository ignored) {
        // La GUI del LabA passa ancora LibriRepository: lo ignoriamo (per compatibilità).
        this.proxy = new BRProxy("127.0.0.1", 5050);
    }

    /**
     * Cerca libri il cui titolo contiene una stringa specificata.
     *
     * @param titolo titolo o parte del titolo da cercare
     * @return lista dei libri trovati; se la risposta non contiene dati ritorna una lista vuota
     * @throws RuntimeException se la risposta del server non è OK
     * @see Request#searchByTitle(String)
     */
    @SuppressWarnings("unchecked")
    public List<Book> cercaLibroPerTitolo(String titolo) {
        Response res = proxy.call(Request.searchByTitle(titolo));
        if (!res.ok) {
            throw new RuntimeException(res.error);
        }
        if (res.data == null) return Collections.emptyList();
        return (List<Book>) res.data;
    }

    /**
     * Cerca libri il cui autore contiene una stringa specificata.
     *
     * @param autore nome o parte del nome dell'autore da cercare
     * @return lista dei libri trovati; se la risposta non contiene dati ritorna una lista vuota
     * @throws RuntimeException se la risposta del server non è OK
     * @see Request#searchByAuthor(String)
     */
    @SuppressWarnings("unchecked")
    public List<Book> cercaLibroPerAutore(String autore) {
        Response res = proxy.call(Request.searchByAuthor(autore));
        if (!res.ok) {
            throw new RuntimeException(res.error);
        }
        if (res.data == null) return Collections.emptyList();
        return (List<Book>) res.data;
    }
}
