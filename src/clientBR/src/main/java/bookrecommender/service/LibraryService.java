package bookrecommender.service;

import bookrecommender.model.Library;
import bookrecommender.net.BRProxy;
import bookrecommender.net.Request;
import bookrecommender.net.Response;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Servizio client per la gestione delle librerie utente.
 * <p>
 * Questo servizio comunica con il server tramite {@link BRProxy} per:
 * </p>
 * <ul>
 *   <li>Elencare le librerie associate a un utente</li>
 *   <li>Salvare una libreria</li>
 *   <li>eliminare una libreria</li>
 * </ul>
 *
 * <p>
 * Il parametro <code>ignored</code> del costruttore è attualmente ignorato (stub/compatibilità).
 * </p>
 *
 * @author Matteo Ferrario
 * @version 2.0
 * @see BRProxy
 * @see Request
 * @see Response
 * @see Library
 */
public class LibraryService {

    /** Proxy di rete usato per interrogare il server. */
    private final BRProxy proxy;

    /**
     * Crea un servizio di gestione librerie.
     *
     * @param ignored percorso attualmente non utilizzato
     */
    public LibraryService(Path ignored) {
        this.proxy = new BRProxy("127.0.0.1", 5050);
    }

    /**
     * Ritorna la lista delle librerie associate a un utente.
     *
     * @param userid userid dell'utente
     * @return lista delle librerie dell'utente; se la risposta non contiene dati ritorna una lista vuota
     * @throws RuntimeException se la risposta del server non è OK (usa <code>res.error</code>)
     * @see Request#listLibrariesByUser(String)
     */
    @SuppressWarnings("unchecked")
    public List<Library> listUserLibraries(String userid) {
        Response res = proxy.call(Request.listLibrariesByUser(userid));
        if (!res.ok) throw new RuntimeException(res.error);
        if (res.data == null) return Collections.emptyList();
        return (List<Library>) res.data;
    }

    /**
     * Salva una libreria sul server.
     *
     * @param lib libreria da salvare
     * @return <code>true</code> se la richiesta va a buon fine e il server ritorna <code>true</code>
     *         oppure se <code>res.data</code> è <code>null</code>; <code>false</code> se <code>res.ok</code> è false
     * @see Request#saveLibrary(Library)
     */
    public boolean saveLibrary(Library lib) {
        Response res = proxy.call(Request.saveLibrary(lib));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }

    /**
     * Elimina una libreria di un utente sul server.
     *
     * @param userid userid dell'utente
     * @param nome nome della libreria da eliminare
     * @return <code>true</code> se la richiesta va a buon fine e il server ritorna <code>true</code>
     *         oppure se <code>res.data</code> è <code>null</code>; <code>false</code> se <code>res.ok</code> è false
     * @see Request#deleteLibrary(String, String)
     */
    public boolean deleteLibrary(String userid, String nome) {
        Response res = proxy.call(Request.deleteLibrary(userid, nome));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }
}
