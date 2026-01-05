package bookrecommender.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Un oggetto della classe <code>Book</code> rappresenta un libro
 * presente nel catalogo del sistema di raccomandazione.
 * Contiene identificatore, titolo, autori, anno di pubblicazione,
 * editore e categoria del libro.
 *
 * @author Ionut Puiu
 * @version 1.0
 */
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    // --------------- ATTRIBUTI ---------------
    private final int id;
    private final String titolo;
    private final List<String> autori;
    private final Integer anno;
    private final String editore;
    private final String categoria;


    // --------------- COSTRUTTORE ---------------
    /**
     * Costruisce un nuovo oggetto <code>Book</code> con i dati specificati.
     * <p>
     * Se il parametro <code>titolo</code> è <code>null</code>, viene
     * sostituito con la stringa vuota. Se il parametro
     * <code>autori</code> è <code>null</code>, viene inizializzata
     * una lista vuota.
     *
     * @param id        identificatore univoco del libro
     * @param titolo    titolo del libro; se <code>null</code> viene
     *                  sostituito con la stringa vuota
     * @param autori    lista degli autori del libro; se <code>null</code>
     *                  viene sostituita con una lista vuota
     * @param anno      anno di pubblicazione del libro, oppure
     *                  <code>null</code> se sconosciuto
     * @param editore   nome dell'editore del libro
     * @param categoria categoria o genere del libro
     */
    public Book(int id, String titolo, List<String> autori, Integer anno, String editore, String categoria) {
        this.id = id;
        this.titolo = titolo == null ? "" : titolo;
        this.autori = autori == null ? new ArrayList<>() : new ArrayList<>(autori);
        this.anno = anno;
        this.editore = editore;
        this.categoria = categoria;
    }


    // --------------- GETTERS ---------------
    /**
     * Restituisce l'identificativo <strong>univoco</strong> del libro.
     *
     * @return l'identificatore del libro
     */
    public int getId() { return id; }


    /**
     * Restituisce il titolo del libro.
     *
     * @return il titolo del libro (mai <code>null</code>)
     */
    public String getTitolo() { return titolo; }


    /**
     * Restituisce una vista non modificabile della lista degli autori.
     * <p>
     * Se in fase di costruzione è stato passato un valore <code>null</code>
     * per la lista di autori, la lista restituita è vuota.
     *
     * @return lista non modificabile degli autori del libro
     */
    public List<String> getAutori() { return Collections.unmodifiableList(autori); }


    /**
     * Restituisce l'anno di pubblicazione del libro.
     *
     * @return l'anno di pubblicazione, oppure <code>null</code> se non noto
     */
    public Integer getAnno() { return anno; }


    /**
     * Restituisce il nome dell'editore del libro.
     *
     * @return il nome dell'editore
     */
    public String getEditore() { return editore; }


    /**
     * Restituisce la categoria o il genere del libro.
     *
     * @return la categoria del libro
     */
    public String getCategoria() { return categoria; }


    // --------------- HELPERS ---------------
    /**
     * Confronta questo libro con un altro oggetto per verificarne
     * l'uguaglianza. Due libri sono considerati uguali se hanno lo stesso
     * valore di <code>id</code>.
     *
     * @param o oggetto con cui confrontare questo libro
     * @return <code>true</code> se l'oggetto specificato è un
     *         <code>Book</code> con lo stesso identificatore,
     *         <code>false</code> altrimenti
     */
    @Override public boolean equals(Object o) { return (o instanceof Book) && ((Book)o).id == id; }


    /**
     * Restituisce il valore di hash del libro, calcolato a partire
     * dal suo identificatore <code>id</code>.
     *
     * @return il valore di hash del libro
     */
    @Override public int hashCode() { return Objects.hash(id); }
}
