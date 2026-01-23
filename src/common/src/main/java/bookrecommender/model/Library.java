package bookrecommender.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Un oggetto della classe <code>Library</code> rappresenta una libreria
 * personale di un utente, identificata dal nome scelto dall'utente stesso.
 * La libreria contiene l'insieme degli identificatori di libri posseduti.
 * <p>
 * Ogni libreria è associata a un singolo utente tramite il campo
 * <code>userid</code>.
 *
 * @author Ionut Puiu
 * @version 1.0
 * @see bookrecommender.model.User
 * @see bookrecommender.model.Book
 */
@SuppressWarnings("ClassCanBeRecord")
public class Library implements Serializable {



    // --------------- ATTRIBUTI ---------------
    private final String userid;
    private final String nome;      // nome libreria scelto dall'utente
    private final Set<Integer> bookIds; // idLibro presenti nella libreria

    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce una nuova libreria per l'utente specificato.
     * <p>
     * Se il parametro <code>bookIds</code> è <code>null</code> viene
     * inizializzato un insieme vuoto.
     *
     * @param userid  identificatore dell'utente proprietario
     * @param nome    nome della libreria scelto dall'utente
     * @param bookIds insieme degli identificatori dei libri già presenti
     *                nella libreria; se <code>null</code> viene creato
     *                un insieme vuoto
     */
    public Library(String userid, String nome, Set<Integer> bookIds) {
        this.userid = userid;
        this.nome = nome;
        this.bookIds = bookIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(bookIds);
    }


    // --------------- GETTERS ---------------
    /**
     * Restituisce l'identificatore dell'utente proprietario della libreria.
     *
     * @return l'identificatore dell'utente
     */
    public String getUserid() { return userid; }


    /**
     * Restituisce il nome della libreria.
     *
     * @return il nome della libreria
     */
    public String getNome() { return nome; }


    /**
     * Restituisce il valore di hash della libreria, calcolato a partire
     * dall'identificatore dell'utente e dal nome della libreria.
     *
     * @return il valore di hash della libreria
     */
    @Override
    public int hashCode() { return Objects.hash(userid, nome); }


    // --------------- SETTERS ---------------
    /**
     * Restituisce una copia dell'insieme degli identificatori dei libri
     * presenti nella libreria.
     *
     * @return una nuova istanza di <code>Set</code> contenente gli id dei libri
     */
    public Set<Integer> getBookIds() { return new LinkedHashSet<>(bookIds); }


    /**
     * Confronta questa libreria con un altro oggetto per verificarne
     * l'uguaglianza. Due librerie sono considerate uguali se appartengono
     * allo stesso utente (<code>userid</code>) e hanno lo stesso nome.
     *
     * @param o oggetto con cui confrontare questa libreria
     * @return <code>true</code> se l'oggetto specificato è una
     *         <code>Library</code> con lo stesso <code>userid</code> e
     *         lo stesso <code>nome</code>, <code>false</code> altrimenti
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Library other)) return false;
        return Objects.equals(userid, other.userid) &&
               Objects.equals(nome, other.nome);
    }
}
