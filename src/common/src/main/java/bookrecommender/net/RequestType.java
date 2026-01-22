package bookrecommender.net;

/**
 * Elenco dei tipi di richiesta supportati dal protocollo client/server.
 * <p>
 * Ogni valore di questo enum identifica un'operazione specifica che il server
 * deve gestire. Il {@link RequestType} viene usato insieme alla classe
 * {@link Request} per instradare correttamente le richieste ricevute.
 * </p>
 *
 * <p>
 * La enum è condivisa tra client e server (modulo <code>common</code>) e
 * rappresenta il "contratto" del protocollo applicativo.
 * </p>
 *
 * @author Richard Zefi
 * @version 1.0
 * @see Request
 */
public enum RequestType {

    /**
     * Richiesta di ping.
     * <p>
     * Usata per verificare la raggiungibilità del server.
     * </p>
     */
    PING,

    // ===== SEARCH =====

    /**
     * Ricerca libri per titolo.
     * <p>
     * Il payload contiene una {@link String} con il titolo o parte di esso.
     * </p>
     */
    SEARCH_BY_TITLE,

    /**
     * Ricerca libri per autore.
     * <p>
     * Il payload contiene una {@link String} con il nome dell'autore.
     * </p>
     */
    SEARCH_BY_AUTHOR,

    /**
     * Ricerca libri per autore e anno di pubblicazione.
     * <p>
     * Il payload contiene un array <code>Object[]</code> con:
     * </p>
     * <ul>
     *     <li>Autore ({@link String});</li>
     *     <li>Anno ({@link Integer});</li>
     *     <li>Limite massimo risultati ({@link Integer}).</li>
     * </ul>
     */
    SEARCH_BY_AUTHOR_YEAR,

    /**
     * Recupera i dettagli completi di un libro dato il suo ID.
     * <p>
     * Il payload contiene l'identificativo del libro ({@link Integer}).
     * </p>
     */
    GET_BOOK_BY_ID,

    // ===== AUTH =====

    /**
     * Richiesta di login utente.
     * <p>
     * Il payload contiene un array <code>String[]</code> con userid e password.
     * </p>
     */
    LOGIN,

    /**
     * Richiesta di registrazione di un nuovo utente.
     * <p>
     * Il payload contiene un oggetto {@code User}.
     * </p>
     */
    REGISTER,

    /**
     * Richiesta di logout dell'utente corrente.
     */
    LOGOUT,

    /**
     * Richiesta di cambio password.
     * <p>
     * Il payload contiene un array <code>Object[]</code> con userid e nuova password.
     * </p>
     */
    CHANGE_PASSWORD,

    /**
     * Richiesta di aggiornamento dell'indirizzo email.
     * <p>
     * Il payload contiene un array <code>Object[]</code> con userid e nuova email.
     * </p>
     */
    UPDATE_EMAIL,

    /**
     * Richiesta di eliminazione definitiva dell'account utente.
     * <p>
     * Il payload contiene il userid ({@link String}).
     * </p>
     */
    DELETE_ACCOUNT,

    // ===== REVIEWS =====

    /**
     * Salvataggio o aggiornamento di una valutazione.
     * <p>
     * Il payload contiene un oggetto {@code Review}.
     * </p>
     */
    SAVE_REVIEW,

    /**
     * Recupera tutte le valutazioni associate a un libro.
     * <p>
     * Il payload contiene l'ID del libro ({@link Integer}).
     * </p>
     */
    GET_REVIEWS_BY_BOOK,

    /**
     * Elenca tutte le valutazioni inserite da un utente.
     * <p>
     * Il payload contiene il userid ({@link String}).
     * </p>
     */
    LIST_REVIEWS_BY_USER,

    /**
     * Elimina una valutazione associata a un libro.
     * <p>
     * Il payload contiene un array <code>Object[]</code> con userid e bookId.
     * </p>
     */
    DELETE_REVIEW,

    // ===== SUGGESTIONS =====

    /**
     * Recupera i suggerimenti associati a un libro.
     * <p>
     * Il payload contiene l'ID del libro ({@link Integer}).
     * </p>
     */
    GET_SUGGESTIONS_BY_BOOK,

    /**
     * Elenca i suggerimenti inseriti da un utente.
     * <p>
     * Il payload contiene il userid ({@link String}).
     * </p>
     */
    LIST_SUGGESTIONS_BY_USER,

    /**
     * Salva un suggerimento completo.
     * <p>
     * Il payload contiene un oggetto {@code Suggestion} che include:
     * </p>
     * <ul>
     *     <li>userid;</li>
     *     <li>bookId;</li>
     *     <li>Lista di libri suggeriti (massimo 3).</li>
     * </ul>
     */
    SAVE_SUGGESTION,

    /**
     * Elimina i suggerimenti associati a una coppia (userid, bookId).
     * <p>
     * Il payload contiene un array <code>Object[]</code> con userid e bookId.
     * </p>
     */
    DELETE_SUGGESTION,

    // ===== LIBRARIES =====

    /**
     * Elenca tutte le librerie di un utente.
     * <p>
     * Il payload contiene il userid ({@link String}).
     * </p>
     */
    LIST_LIBRARIES_BY_USER,

    /**
     * Salva o aggiorna una libreria.
     * <p>
     * Il payload contiene un oggetto {@code Library} che include:
     * </p>
     * <ul>
     *     <li>userid;</li>
     *     <li>Nome della libreria;</li>
     *     <li>Insieme di ID dei libri contenuti.</li>
     * </ul>
     */
    SAVE_LIBRARY,

    /**
     * Elimina una libreria.
     * <p>
     * Il payload contiene un array <code>Object[]</code> con userid e nome libreria.
     * </p>
     */
    DELETE_LIBRARY
}
