package bookrecommender.model;

import java.io.Serializable;

/**
 * Un oggetto della classe <code>Review</code> rappresenta la valutazione
 * assegnata da un utente a un libro secondo <strong>cinque criteri</strong>:
 * stile, contenuto, gradevolezza, originalità ed edizione.
 * <p>
 * Per ciascun criterio viene registrato un voto intero compreso tra 1 e 5.
 * Dal valore medio dei cinque criteri viene ricavato il voto finale
 * arrotondato all'intero più vicino.
 *
 * @author Ionut Puiu
 * @version 1.0
 * @see bookrecommender.model.User
 * @see bookrecommender.model.Book
 */
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    // --------------- ATTRIBUTI ---------------
    private final String userid;
    private final int bookId;
    private final int stile, contenuto, gradevolezza, originalita, edizione; // 1..5
    private final int votoFinale; // round(media 5 criteri)
    private final String commento; // ≤256


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce una nuova recensione per il libro e l'utente specificati.
     *
     * @param userid        identificatore dell'utente che ha espresso la recensione
     * @param bookId        identificatore del libro recensito
     * @param stile         voto assegnato allo stile (1..5)
     * @param contenuto     voto assegnato al contenuto (1..5)
     * @param gradevolezza  voto assegnato alla gradevolezza (1..5)
     * @param originalita   voto assegnato all'originalità (1..5)
     * @param edizione      voto assegnato all'edizione (1..5)
     * @param votoFinale    voto complessivo assegnato alla recensione
     * @param commento      commento testuale associato alla recensione
     */
    public Review(String userid, int bookId, int stile, int contenuto, int gradevolezza, int originalita, int edizione, int votoFinale, String commento) {
        this.userid = userid;
        this.bookId = bookId;
        this.stile = stile;
        this.contenuto = contenuto;
        this.gradevolezza = gradevolezza;
        this.originalita = originalita;
        this.edizione = edizione;
        this.votoFinale = votoFinale;
        this.commento = commento;
    }


    // --------------- GETTERS ---------------
    /**
     * Restituisce l'identificatore dell'utente che ha espresso la recensione.
     *
     * @return l'identificatore dell'utente
     */
    public String getUserid() { return userid; }


    /**
     * Restituisce l'identificatore del libro recensito.
     *
     * @return l'identificatore del libro
     */
    public int getBookId() { return bookId;}


    /**
     * Restituisce il voto assegnato allo stile.
     *
     * @return il voto sullo stile (1..5)
     */
    public int getStile() { return stile;}


    /**
     * Restituisce il voto assegnato al contenuto.
     *
     * @return il voto sul contenuto (1..5)
     */
    public int getContenuto() { return contenuto;}


    /**
     * Restituisce il voto assegnato alla gradevolezza.
     *
     * @return il voto sulla gradevolezza (1..5)
     */
    public int getGradevolezza() { return gradevolezza;}


    /**
     * Restituisce il voto assegnato all'originalità.
     *
     * @return il voto sull'originalità (1..5)
     */
    public int getOriginalita() { return originalita;}


    /**
     * Restituisce il voto assegnato all'edizione.
     *
     * @return il voto sull'edizione (1..5)
     */
    public int getEdizione() { return edizione;}


    /**
     * Restituisce il voto finale complessivo della recensione.
     *
     * @return il voto complessivo
     */
    public int getVotoFinale() { return votoFinale;}


    /**
     * Restituisce il commento testuale associato alla recensione.
     *
     * @return il commento della recensione
     */
    public String getCommento() { return commento;}


    // --------------- METODO PUBBLICO ---------------
    /**
     * Calcola il voto finale a partire dai voti dei cinque criteri,
     * come media aritmetica arrotondata all'intero più vicino.
     *
     * @param stile        voto assegnato allo stile (1..5)
     * @param contenuto    voto assegnato al contenuto (1..5)
     * @param gradevolezza voto assegnato alla gradevolezza (1..5)
     * @param originalita  voto assegnato all'originalità (1..5)
     * @param edizione     voto assegnato all'edizione (1..5)
     * @return il voto finale ottenuto arrotondando la media dei cinque criteri
     */
    public static int calcolaVotoFinale(int stile, int contenuto, int gradevolezza, int originalita, int edizione) {
        double media = (stile + contenuto + gradevolezza + originalita + edizione) / 5.0;
        return (int)Math.round(media);
    }
}
