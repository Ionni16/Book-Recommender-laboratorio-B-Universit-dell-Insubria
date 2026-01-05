package bookrecommender.service;

import bookrecommender.model.Review;
import bookrecommender.util.Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * La classe <code>ReviewService</code> gestisce le valutazioni dei libri
 * effettuate dagli utenti.
 * <p>
 * I dati sono memorizzati nel file <code>ValutazioniLibri.dati</code> con
 * righe nel formato:
 * <pre>
 * userId; bookId; stile; contenuto; gradev; originalita; edizione; votoFinale; commento
 * </pre>
 * Inoltre viene utilizzato il file <code>Librerie.dati</code> per verificare
 * che l'utente abbia inserito il libro in almeno una delle proprie librerie
 * prima di poterlo valutare.
 *
 * @author Hamdi Kebeli
 * @version 1.0
 * @see bookrecommender.model.Review
 */
public class ReviewService {

    private final Path fileValutazioni;
    private final Path fileLibrerie;

    /**
     * Costruisce un nuovo servizio per la gestione delle valutazioni.
     *
     * @param fileValutazioni percorso del file <code>ValutazioniLibri.dati</code>
     * @param fileLibrerie    percorso del file <code>Librerie.dati</code>
     *                        usato per verificare il possesso del libro
     */
    public ReviewService(Path fileValutazioni, Path fileLibrerie) {
        this.fileValutazioni = fileValutazioni;
        this.fileLibrerie = fileLibrerie;
    }

    // ============ API PRINCIPALE (già esistente) ============

    /**
     * Inserisce una nuova valutazione per un libro.
     * <p>
     * Requisiti:
     * <ul>
     *     <li>Il libro deve essere presente in almeno una libreria
     *         dell'utente;</li>
     *     <li>Non deve esistere già una valutazione per la coppia
     *         <code>(userid, bookId)</code>.</li>
     * </ul>
     *
     * @param r oggetto {@link Review} che rappresenta la valutazione da inserire
     * @return {@code true} se la valutazione è stata inserita correttamente,
     *         {@code false} se i requisiti non sono rispettati
     * @throws Exception in caso di errore di I/O durante lettura/scrittura dei file
     */
    public boolean inserisciValutazione(Review r) throws Exception {
        if (!Utilities.utenteHaLibroInLibreria(r.getUserid(), r.getBookId(), fileLibrerie)) {
            return false;
        }

        List<Review> all = loadAll();
        boolean esisteGia = all.stream()
                .anyMatch(x -> x.getUserid().equals(r.getUserid())
                        && x.getBookId() == r.getBookId());
        if (esisteGia) return false;

        all.add(r);
        saveAll(all);
        return true;
    }

    // ============ METODI AGGIUNTI PER LA GUI ============

    /**
     * Restituisce tutte le valutazioni effettuate da un determinato utente.
     *
     * @param userid identificatore dell'utente
     * @return lista di {@link Review} associate a quello <code>userid</code>;
     *         lista vuota se l'utente non ha ancora effettuato valutazioni
     * @throws Exception in caso di errore di I/O durante la lettura del file
     */
    public List<Review> listByUser(String userid) throws Exception {
        return loadAll().stream()
                .filter(r -> r.getUserid().equals(userid))
                .collect(Collectors.toList());
    }

    /**
     * Aggiorna una valutazione esistente.
     * <p>
     * L'identificazione della valutazione avviene tramite la coppia
     * <code>(userid, bookId)</code>; questi campi devono quindi rimanere invariati
     * nell'oggetto passato in ingresso.
     *
     * @param updated oggetto {@link Review} contenente i nuovi valori
     * @return {@code true} se la valutazione da aggiornare è stata trovata e
     *         sostituita, {@code false} se non esiste alcuna valutazione
     *         per quella coppia <code>(userid, bookId)</code>
     * @throws Exception in caso di errore di I/O durante lettura/scrittura del file
     */
    public boolean updateReview(Review updated) throws Exception {
        List<Review> all = loadAll();
        boolean found = false;
        List<Review> nuovo = new ArrayList<>();
        for (Review r : all) {
            if (r.getUserid().equals(updated.getUserid())
                    && r.getBookId() == updated.getBookId()) {
                nuovo.add(updated);
                found = true;
            } else {
                nuovo.add(r);
            }
        }
        if (!found) return false;
        saveAll(nuovo);
        return true;
    }

    /**
     * Elimina la valutazione associata a un certo utente e a un certo libro.
     *
     * @param userid identificatore dell'utente
     * @param bookId identificatore del libro
     * @return {@code true} se la valutazione è stata trovata e rimossa,
     *         {@code false} se non esisteva alcuna valutazione per quei parametri
     * @throws Exception in caso di errore di I/O durante lettura/scrittura del file
     */
    public boolean deleteReview(String userid, int bookId) throws Exception {
        List<Review> all = loadAll();
        boolean removed = all.removeIf(r ->
                r.getUserid().equals(userid) && r.getBookId() == bookId);
        if (removed) saveAll(all);
        return removed;
    }

    // ============ IO SU FILE ============

    /**
     * Carica tutte le valutazioni dal file delle valutazioni.
     * <p>
     * Le righe vuote o con formato non valido vengono ignorate.
     *
     * @return lista di tutte le {@link Review} presenti nel file;
     *         lista vuota se il file non esiste
     * @throws Exception in caso di errore di I/O durante la lettura del file
     */
    private List<Review> loadAll() throws Exception {
        List<Review> list = new ArrayList<>();
        if (!Files.exists(fileValutazioni)) return list;

        try (BufferedReader br = Files.newBufferedReader(fileValutazioni)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(";", -1);
                if (c.length < 9) continue;
                String userid = c[0];
                int bookId = parseIntSafe(c[1]);
                int stile = parseIntSafe(c[2]);
                int contenuto = parseIntSafe(c[3]);
                int gradev = parseIntSafe(c[4]);
                int orig = parseIntSafe(c[5]);
                int ediz = parseIntSafe(c[6]);
                int votoFinale = parseIntSafe(c[7]);
                String commento = c[8];
                Review r = new Review(userid, bookId, stile, contenuto,
                        gradev, orig, ediz, votoFinale, commento);
                list.add(r);
            }
        }
        return list;
    }

    /**
     * Sovrascrive completamente il file delle valutazioni con la lista specificata.
     * <p>
     * Ogni valutazione viene scritta su una riga nel formato:
     * <pre>
     * userId; bookId; stile; contenuto; gradev; originalita; edizione; votoFinale; commento
     * </pre>
     *
     * @param list lista di {@link Review} da salvare
     * @throws Exception in caso di errore di I/O durante la scrittura del file
     */
    private void saveAll(List<Review> list) throws Exception {
        Files.createDirectories(fileValutazioni.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(fileValutazioni)) {
            for (Review r : list) {
                bw.write(r.getUserid() + ";" +
                        r.getBookId() + ";" +
                        r.getStile() + ";" +
                        r.getContenuto() + ";" +
                        r.getGradevolezza() + ";" +
                        r.getOriginalita() + ";" +
                        r.getEdizione() + ";" +
                        r.getVotoFinale() + ";" +
                        (r.getCommento() == null ? "" : r.getCommento()));
                bw.newLine();
            }
        }
    }

    /**
     * Effettua il parse sicuro di una stringa in intero.
     * <p>
     * In caso di errore (stringa nulla, vuota o non numerica) viene restituito
     * il valore <code>0</code>.
     *
     * @param s stringa da convertire in intero
     * @return valore intero corrispondente oppure <code>0</code> se il parse fallisce
     */
    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }
}
