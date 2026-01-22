package bookrecommender.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Un oggetto della classe <code>Suggestion</code> rappresenta un insieme
 * di suggerimenti di lettura forniti da un utente a partire da un libro
 * di riferimento.
 * <p>
 * Per ogni libro recensito un utente può indicare fino a tre altri libri
 * che ritiene collegati o consigliati.
 *
 * @author Ionut Puiu
 * @version 1.0
 * @see bookrecommender.model.User
 * @see bookrecommender.model.Book
 */
@SuppressWarnings("ClassCanBeRecord")
public class Suggestion implements Serializable {


    // --------------- ATTRIBUTI ---------------
    private final String userid;
    private final int bookId;          // libro di riferimento
    private final List<Integer> suggeriti; // fino a 3 idLibro suggeriti


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce un nuovo insieme di suggerimenti per il libro e
     * l'utente specificati.
     * <p>
     * Se il parametro <code>suggeriti</code> è <code>null</code>,
     * viene inizializzata una lista vuota.
     *
     * @param userid    identificatore dell'utente che ha espresso il suggerimento
     * @param bookId    identificatore del libro di riferimento
     * @param suggeriti lista degli identificatori dei libri suggeriti;
     *                  se <code>null</code> viene creata una lista vuota
     */
    public Suggestion(String userid, int bookId, List<Integer> suggeriti) {
        this.userid = userid;
        this.bookId = bookId;
        this.suggeriti = suggeriti == null ? new ArrayList<>() : new ArrayList<>(suggeriti);
    }


    // --------------- GETTERS ---------------
    /**
     * Restituisce l'identificatore dell'utente che ha espresso il suggerimento.
     *
     * @return l'identificatore dell'utente
     */
    public String getUserid() { return userid; }


    /**
     * Restituisce l'identificatore del libro di riferimento.
     *
     * @return l'identificatore del libro di riferimento
     */
    public int getBookId() { return bookId; }


    /**
     * Restituisce una copia della lista degli identificatori dei libri
     * suggeriti dall'utente.
     *
     * @return una nuova lista contenente gli id dei libri suggeriti
     */
    public List<Integer> getSuggeriti() { return new ArrayList<>(suggeriti); }
}
