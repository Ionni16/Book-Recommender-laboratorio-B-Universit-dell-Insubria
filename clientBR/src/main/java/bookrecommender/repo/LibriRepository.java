package bookrecommender.repo;

import bookrecommender.model.Book;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository in memoria per la gestione dei libri (cache client-side).
 * <p>
 * Questa classe mantiene un mini-catalogo di {@link Book} indicizzato per ID.
 * Non effettua operazioni di I/O: il popolamento dei dati è demandato a livelli
 * superiori (es. Servizi che interrogano il server) che poi inseriscono i libri
 * in questa cache tramite {@link #put(Book)} o {@link #putAll(Iterable)}.
 * </p>
 *
 * <h2>Caratteristiche</h2>
 * <ul>
 *   <li>Thread-safe grazie all'uso di {@link ConcurrentHashMap}</li>
 *   <li>Lookup veloce per ID</li>
 *   <li>Nessuna persistenza su file</li>
 * </ul>
 *
 * @author Matteo Ferrario
 * @version 2.0
 * @see Book
 */
public class LibriRepository {


    /**
     * Percorso ignorato, mantenuto solo per compatibilità con l'architettura.
     */
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final Path ignoredPath;


    /**
     * Mappa dei libri indicizzati per ID.
     */
    @SuppressWarnings("CollectionNeverUpdated")
    private final Map<Integer, Book> byId = new ConcurrentHashMap<>();


    /**
     * Crea un nuovo repository dei libri.
     *
     * @param ignoredPath percorso attualmente non utilizzato
     */
    public LibriRepository(Path ignoredPath) {
        this.ignoredPath = ignoredPath;
    }


    /**
     * Cerca un libro per ID.
     *
     * @param id identificativo del libro
     * @return il libro corrispondente, oppure <code>null</code> se non trovato
     */
    public Book findById(Integer id) {
        if (id == null) return null;
        return byId.get(id);
    }

    /**
     * Inserisce (o sovrascrive) un libro nella cache in memoria.
     * <p>
     * Se esiste già un libro con lo stesso ID, verrà sostituito.
     * </p>
     *
     * @param book libro da inserire; se {@code null} l'operazione viene ignorata
     */
    public void put(Book book) {
        if (book != null) {
            byId.put(book.getId(), book);
        }
    }


    /**
     * Inserisce tutti i libri forniti nella cache in memoria.
     * <p>
     * Gli elementi {@code null} presenti nell'iterabile vengono ignorati.
     * </p>
     *
     * @param books collezione/iterabile di libri da inserire; non deve essere {@code null}
     */
    public void putAll(Iterable<Book> books) {
        for (Book b : books) {
            put(b);
        }
    }
}
