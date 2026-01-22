package bookrecommender.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Un oggetto della classe <code>User</code> rappresenta un utente
 * registrato nel sistema di raccomandazione.
 * <p>
 * Per ogni utente vengono memorizzati un identificatore univoco,
 * l'hash della password e alcuni dati anagrafici e di contatto.
 *
 * @author Ionut Puiu
 * @version 1.0
 * @see bookrecommender.model.Library
 * @see bookrecommender.model.Review
 * @see bookrecommender.model.Suggestion
 */
@SuppressWarnings("ClassCanBeRecord")
public class User implements Serializable {


    // --------------- ATTRIBUTI ---------------
    private final String userid;
    private final String passwordHash; // SHA-256 
    private final String nome;
    private final String cognome;
    private final String codiceFiscale;
    private final String email;


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce un nuovo utente con i dati specificati.
     *
     * @param userid        identificatore univoco dell'utente
     * @param passwordHash  hash della password (SHA-256)
     * @param nome          nome dell'utente
     * @param cognome       cognome dell'utente
     * @param codiceFiscale codice fiscale dell'utente
     * @param email         indirizzo e-mail dell'utente
     */
    public User(String userid, String passwordHash, String nome, String cognome, String codiceFiscale, String email) {
        this.userid = userid;
        this.passwordHash = passwordHash;
        this.nome = nome;
        this.cognome = cognome;
        this.codiceFiscale = codiceFiscale;
        this.email = email;
    }


    // --------------- GETTERS ---------------
    /**
     * Restituisce l'identificatore univoco dell'utente.
     *
     * @return l'identificatore dell'utente
     */
    public String getUserid() { return userid;}


    /**
     * Restituisce l'hash della password dell'utente.
     *
     * @return l'hash della password (SHA-256)
     */
    public String getPasswordHash() { return passwordHash;}


    /**
     * Restituisce il nome dell'utente.
     *
     * @return il nome dell'utente
     */
    public String getNome() { return nome;}


    /**
     * Restituisce il cognome dell'utente.
     *
     * @return il cognome dell'utente
     */
    public String getCognome() { return cognome;}


    /**
     * Restituisce il codice fiscale dell'utente.
     *
     * @return il codice fiscale
     */
    public String getCodiceFiscale() { return codiceFiscale;}


    /**
     * Restituisce l'indirizzo e-mail dell'utente.
     *
     * @return l'indirizzo e-mail
     */
    public String getEmail() { return email;}


    // --------------- METODI PUBBLICI ---------------
    /**
     * Confronta questo utente con un altro oggetto per verificarne
     * l'uguaglianza. Due utenti sono considerati uguali se hanno lo stesso
     * valore di <code>userid</code>.
     *
     * @param o oggetto con cui confrontare questo utente
     * @return <code>true</code> se l'oggetto specificato è un
     *         <code>User</code> con lo stesso identificatore,
     *         <code>false</code> altrimenti
     */
    @Override public boolean equals(Object o) { return (o instanceof User) && Objects.equals(userid, ((User)o).userid);}


    /**
     * Restituisce il valore di hash dell'utente, calcolato a partire
     * dal suo identificatore <code>userid</code>.
     *
     * @return il valore di hash dell'utente
     */
    @Override public int hashCode() { return Objects.hash(userid);}
}
