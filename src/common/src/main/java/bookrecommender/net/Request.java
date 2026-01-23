package bookrecommender.net;

import java.io.Serializable;

/**
 * Rappresenta una richiesta inviata dal client al server tramite socket.
 * <p>
 * La richiesta è serializzabile e contiene:
 * <ul>
 *     <li>Il tipo di richiesta ({@link RequestType});</li>
 *     <li>Un payload opzionale con i dati necessari all'operazione;</li>
 *     <li>Un token di autenticazione (attualmente non utilizzato).</li>
 * </ul>
 *
 * <p>
 * La classe fornisce diversi metodi factory statici per creare richieste
 * tipizzate in modo sicuro e leggibile, evitando la costruzione manuale
 * del payload.
 * </p>
 *
 * <p>
 * Questa classe è condivisa tra client e server (modulo <code>common</code>).
 * </p>
 *
 * @author Richard Zefi
 * @version 1.0
 * @see RequestType
 */
@SuppressWarnings("ClassCanBeRecord")
public class Request implements Serializable {

    /**
     * Tipo della richiesta, usato dal server per instradare l'operazione.
     */
    public final RequestType type;

    /**
     * Dati associati alla richiesta.
     * <p>
     * Il tipo effettivo dipende dal {@link #type} e può essere:
     * </p>
     * <ul>
     *     <li>Un valore semplice (es. {@link String}, {@link Integer});</li>
     *     <li>Un array di oggetti;</li>
     *     <li>Un model serializzabile (es. <code>User</code>, <code>Review</code>).</li>
     * </ul>
     */
    public final Object payload;

    /**
     * Token di autenticazione associato alla richiesta.
     * <p>
     * Attualmente non utilizzato, ma previsto per estensioni future.
     * </p>
     */
    public final String token;

    /**
     * Costruisce una nuova richiesta.
     *
     * @param type    tipo della richiesta
     * @param payload dati associati alla richiesta
     * @param token   token di autenticazione (può essere {@code null})
     */
    public Request(RequestType type, Object payload, String token) {
        this.type = type;
        this.payload = payload;
        this.token = token;
    }

    /**
     * Crea una richiesta di ping per verificare la raggiungibilità del server.
     *
     * @return richiesta di tipo {@link RequestType#PING}
     */
    public static Request ping() {
        return new Request(RequestType.PING, null, null);
    }

    /**
     * Crea una richiesta di ricerca libri per titolo.
     *
     * @param titolo titolo (o parte di esso) da cercare
     * @return richiesta di tipo {@link RequestType#SEARCH_BY_TITLE}
     */
    public static Request searchByTitle(String titolo) {
        return new Request(RequestType.SEARCH_BY_TITLE, titolo, null);
    }

    /**
     * Crea una richiesta di ricerca libri per autore.
     *
     * @param autore nome dell'autore
     * @return richiesta di tipo {@link RequestType#SEARCH_BY_AUTHOR}
     */
    public static Request searchByAuthor(String autore) {
        return new Request(RequestType.SEARCH_BY_AUTHOR, autore, null);
    }


    /**
     * Crea una richiesta di logout.
     *
     * @return richiesta di tipo {@link RequestType#LOGOUT}
     */
    public static Request logout() {
        return new Request(RequestType.LOGOUT, null, null);
    }

    // ===== REVIEWS =====

    /**
     * Crea una richiesta di salvataggio o aggiornamento di una valutazione.
     *
     * @param r valutazione da salvare
     * @return richiesta di tipo {@link RequestType#SAVE_REVIEW}
     */
    public static Request saveReview(bookrecommender.model.Review r) {
        return new Request(RequestType.SAVE_REVIEW, r, null);
    }

    /**
     * Crea una richiesta per ottenere tutte le valutazioni associate a un libro.
     *
     * @param bookId identificativo del libro
     * @return richiesta di tipo {@link RequestType#GET_REVIEWS_BY_BOOK}
     */
    public static Request getReviewsByBook(int bookId) {
        return new Request(RequestType.GET_REVIEWS_BY_BOOK, bookId, null);
    }

    /**
     * Crea una richiesta per elencare le valutazioni inserite da un utente.
     *
     * @param userid identificativo dell'utente
     * @return richiesta di tipo {@link RequestType#LIST_REVIEWS_BY_USER}
     */
    public static Request listReviewsByUser(String userid) {
        return new Request(RequestType.LIST_REVIEWS_BY_USER, userid, null);
    }

    /**
     * Crea una richiesta per eliminare una valutazione.
     *
     * @param userid identificativo dell'utente
     * @param bookId identificativo del libro
     * @return richiesta di tipo {@link RequestType#DELETE_REVIEW}
     */
    public static Request deleteReview(String userid, int bookId) {
        return new Request(RequestType.DELETE_REVIEW, new Object[]{userid, bookId}, null);
    }

    // ===== SUGGESTIONS =====

    /**
     * Crea una richiesta per ottenere i suggerimenti associati a un libro.
     *
     * @param bookId identificativo del libro
     * @return richiesta di tipo {@link RequestType#GET_SUGGESTIONS_BY_BOOK}
     */
    public static Request getSuggestionsByBook(int bookId) {
        return new Request(RequestType.GET_SUGGESTIONS_BY_BOOK, bookId, null);
    }

    /**
     * Crea una richiesta per elencare i suggerimenti inseriti da un utente.
     *
     * @param userid identificativo dell'utente
     * @return richiesta di tipo {@link RequestType#LIST_SUGGESTIONS_BY_USER}
     */
    public static Request listSuggestionsByUser(String userid) {
        return new Request(RequestType.LIST_SUGGESTIONS_BY_USER, userid, null);
    }

    /**
     * Crea una richiesta di salvataggio di un suggerimento.
     *
     * @param s suggerimento da salvare
     * @return richiesta di tipo {@link RequestType#SAVE_SUGGESTION}
     */
    public static Request saveSuggestion(bookrecommender.model.Suggestion s) {
        return new Request(RequestType.SAVE_SUGGESTION, s, null);
    }

    /**
     * Crea una richiesta per eliminare un suggerimento associato a un libro.
     *
     * @param userid identificativo dell'utente
     * @param bookId identificativo del libro
     * @return richiesta di tipo {@link RequestType#DELETE_SUGGESTION}
     */
    public static Request deleteSuggestion(String userid, int bookId) {
        return new Request(RequestType.DELETE_SUGGESTION, new Object[]{userid, bookId}, null);
    }

    // ===== LIBRARIES =====

    /**
     * Crea una richiesta per elencare le librerie di un utente.
     *
     * @param userid identificativo dell'utente
     * @return richiesta di tipo {@link RequestType#LIST_LIBRARIES_BY_USER}
     */
    public static Request listLibrariesByUser(String userid) {
        return new Request(RequestType.LIST_LIBRARIES_BY_USER, userid, null);
    }

    /**
     * Crea una richiesta di salvataggio o aggiornamento di una libreria.
     *
     * @param lib libreria da salvare
     * @return richiesta di tipo {@link RequestType#SAVE_LIBRARY}
     */
    public static Request saveLibrary(bookrecommender.model.Library lib) {
        return new Request(RequestType.SAVE_LIBRARY, lib, null);
    }

    /**
     * Crea una richiesta per eliminare una libreria.
     *
     * @param userid identificativo dell'utente
     * @param nome   nome della libreria
     * @return richiesta di tipo {@link RequestType#DELETE_LIBRARY}
     */
    public static Request deleteLibrary(String userid, String nome) {
        return new Request(RequestType.DELETE_LIBRARY, new Object[]{userid, nome}, null);
    }


    /**
     * Crea una richiesta per rinominare una libreria.
     *
     * @param userid  identificativo utente proprietario
     * @param oldName nome attuale libreria
     * @param newName nuovo nome libreria
     * @return richiesta di tipo {@link RequestType#RENAME_LIBRARY}
     */
    public static Request renameLibrary(String userid, String oldName, String newName) {
        return new Request(RequestType.RENAME_LIBRARY, new Object[]{userid, oldName, newName}, null);
    }



    /**
     * Crea una richiesta per ottenere i dettagli di un libro dato il suo ID.
     *
     * @param bookId identificativo del libro
     * @return richiesta di tipo {@link RequestType#GET_BOOK_BY_ID}
     */
    public static Request getBookById(int bookId) {
        return new Request(RequestType.GET_BOOK_BY_ID, new Object[]{bookId}, null);
    }

    /**
     * Crea una richiesta per eliminare definitivamente un account utente.
     *
     * @param userid identificativo dell'utente
     * @return richiesta di tipo {@link RequestType#DELETE_ACCOUNT}
     */
    public static Request deleteAccount(String userid) {
        return new Request(RequestType.DELETE_ACCOUNT, userid, null);
    }
}
