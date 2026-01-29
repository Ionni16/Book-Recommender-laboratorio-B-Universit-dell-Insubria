package bookrecommender.service;

import bookrecommender.model.Book;
import bookrecommender.repo.LibriRepository;

import java.util.List;

/**
 * Servizio applicativo per la ricerca dei libri.
 *
 * <p>
 * La classe fornisce un livello di astrazione sopra {@link LibriRepository},
 * delegando le operazioni di ricerca e applicando controlli minimi sugli input.
 * Supporta ricerche per titolo, autore e combinazione autore/anno, con possibilità
 * di limitare il numero di risultati restituiti.
 * </p>
 *
 * <ul>
 *   <li>Ricerca libri per titolo con confronto case-insensitive</li>
 *   <li>Ricerca libri per autore</li>
 *   <li>Ricerca libri per autore e anno di pubblicazione</li>
 * </ul>
 *
 * @author Richard Zefi
 * @version 1.0
 * @see bookrecommender.repo.LibriRepository
 * @see bookrecommender.model.Book
 */
@SuppressWarnings("ClassCanBeRecord")
public class SearchService {

    private final LibriRepository libriRepo;

    /**
     * Costruisce il servizio di ricerca inizializzandolo con il repository dei libri.
     *
     * <p>
     * Il repository viene utilizzato per eseguire le query di ricerca sul database.
     * </p>
     *
     * @param libriRepo repository per l’accesso ai dati dei libri
     */
    public SearchService(LibriRepository libriRepo) {
        this.libriRepo = libriRepo;
    }

    /**
     * Ricerca libri il cui titolo contiene una determinata substring.
     *
     * <p>
     * La ricerca è delegata al repository e restituisce una lista di {@link Book}
     * con autori aggregati, limitata dal parametro {@code limit}.
     * </p>
     *
     * @param q substring da ricercare nel titolo
     * @param limit numero massimo di risultati da restituire
     * @return lista dei libri trovati
     */
    public List<Book> cercaLibroPerTitolo(String q, int limit) {
        return libriRepo.searchByTitleContains(q, limit);
    }

    /**
     * Ricerca libri che hanno almeno un autore contenente una determinata substring.
     *
     * <p>
     * La ricerca è delegata al repository e restituisce una lista di {@link Book}
     * con autori aggregati, limitata dal parametro {@code limit}.
     * </p>
     *
     * @param a substring da ricercare nel nome autore
     * @param limit numero massimo di risultati da restituire
     * @return lista dei libri trovati
     */
    public List<Book> cercaLibroPerAutore(String a, int limit) {
        return libriRepo.searchByAuthorContains(a, limit);
    }

    /**
     * Ricerca libri per autore e anno di pubblicazione.
     *
     * <p>
     * Se l’autore è nullo o vuoto, il metodo restituisce una lista vuota.
     * La ricerca è delegata al repository e i risultati sono limitati
     * dal parametro {@code limit}.
     * </p>
     *
     * @param autore substring da ricercare nel nome autore
     * @param anno anno di pubblicazione
     * @param limit numero massimo di risultati da restituire
     * @return lista dei libri trovati per autore e anno
     */
    public List<Book> cercaLibroPerAutoreEAnno(String autore, int anno, int limit) {
        if (autore == null || autore.isBlank()) return List.of();
        return libriRepo.searchByAuthorAndYear(autore, anno, limit);
    }
    
}
