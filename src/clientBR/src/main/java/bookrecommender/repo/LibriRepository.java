package bookrecommender.repo;

import bookrecommender.model.Book;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository in memoria per la gestione dei libri.
 * <p>
 * Questa classe mantiene un mini-catalogo di {@link Book} indicizzato per ID.
 * Attualmente il repository è popolato solo in memoria; il caricamento da file
 * o da server remoto è demandato a sviluppi futuri.
 * </p>
 *
 * <h2>Caratteristiche</h2>
 * <ul>
 *   <li>Thread-safe grazie all'uso di {@link ConcurrentHashMap}</li>
 *   <li>Supporta ricerche per ID, titolo e autore</li>
 *   <li>Non persiste dati su file</li>
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

}
