package bookrecommender.service;

import bookrecommender.model.Suggestion;
import bookrecommender.util.Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * La classe <code>SuggestionService</code> gestisce i suggerimenti di libri
 * correlati inseriti dagli utenti.
 * <p>
 * I dati sono memorizzati nel file <code>ConsigliLibri.dati</code> con righe
 * nel formato:
 * <pre>
 * userid; bookId; idSug1,idSug2,...
 * </pre>
 * Dove:
 * <ul>
 *     <li><code>userid</code> è l'identificatore dell'utente;</li>
 *     <li><code>bookId</code> è l'identificatore del libro "base" a cui si
 *         riferiscono i suggerimenti;</li>
 *     <li><code>idSug1,idSug2,...</code> è la lista di ID dei libri suggeriti,
 *         separati da virgole.</li>
 * </ul>
 * Il file <code>Librerie.dati</code> viene utilizzato per verificare che
 * l'utente possieda realmente i libri che intende suggerire.
 *
 * @author ...
 * @version 1.0
 * @see bookrecommender.model.Suggestion
 */
public class SuggestionService {

    /** Percorso del file che contiene i suggerimenti salvati. */
    private final Path fileConsigli;

    /** Percorso del file che contiene le librerie degli utenti. */
    private final Path fileLibrerie;

    /**
     * Costruisce un nuovo servizio per la gestione dei suggerimenti di libri.
     *
     * @param fileConsigli percorso del file <code>ConsigliLibri.dati</code>
     * @param fileLibrerie percorso del file <code>Librerie.dati</code>
     *                     utilizzato per i controlli sul possesso dei libri
     */
    public SuggestionService(Path fileConsigli,  Path fileLibrerie) {
        this.fileConsigli = fileConsigli;
        this.fileLibrerie = fileLibrerie;
    }

    // ============ API PRINCIPALE ============

    /**
     * Inserisce un nuovo suggerimento di libri correlati per un certo libro base.
     * <p>
     * Requisiti (come da specifiche del progetto):
     * <ul>
     *     <li>Possono essere suggeriti al massimo 3 libri;</li>
     *     <li>Tutti i libri suggeriti devono essere diversi tra loro;</li>
     *     <li>Tutti i libri suggeriti devono essere diversi dal libro base;</li>
     *     <li>Tutti i libri suggeriti devono essere presenti in almeno una
     *         libreria dell'utente;</li>
     *     <li>L'utente può inserire suggerimenti per una certa coppia
     *         <code>(userid, bookId)</code> una sola volta.</li>
     * </ul>
     *
     * @param s oggetto {@link Suggestion} contenente utente, libro base e lista dei libri suggeriti
     * @return {@code true} se il suggerimento rispetta tutti i vincoli ed è stato salvato,
     *         {@code false} in caso contrario (vincoli violati o suggerimento già presente)
     * @throws Exception in caso di errore di I/O durante lettura o scrittura dei file
     */
    public boolean inserisciSuggerimento(Suggestion s) throws Exception {
        List<Integer> ids = new ArrayList<>(s.getSuggeriti());

        // filtra duplicati e self
        ids = ids.stream().distinct()
                .filter(id -> id != s.getBookId())
                .collect(Collectors.toList());
        if (ids.isEmpty() || ids.size() > 3) return false;

        // controlla che ogni libro suggerito sia in almeno una libreria dell'utente
        for (int id : ids) {
            if (!Utilities.utenteHaLibroInLibreria(s.getUserid(), id, fileLibrerie)) {
                return false;
            }
        }

        // l'utente può suggerire per quel libro UNA SOLA VOLTA
        List<Suggestion> all = loadAll();
        boolean esisteGia = all.stream()
                .anyMatch(old -> old.getUserid().equals(s.getUserid())
                        && old.getBookId() == s.getBookId());
        if (esisteGia) {
            return false;
        }

        all.add(new Suggestion(s.getUserid(), s.getBookId(), ids));
        saveAll(all);
        return true;
    }


    // ============ METODI PER LA GUI ============

    /**
     * Restituisce tutti i suggerimenti inseriti da un determinato utente.
     *
     * @param userid identificatore dell'utente
     * @return lista di {@link Suggestion} associati a quello <code>userid</code>;
     *         lista vuota se l'utente non ha suggerito alcun libro
     * @throws Exception in caso di errore di I/O durante la lettura del file
     */
    public List<Suggestion> listByUser(String userid) throws Exception {
        return loadAll().stream()
                .filter(s -> s.getUserid().equals(userid))
                .collect(Collectors.toList());
    }

    /**
     * Elimina il suggerimento di un utente per un certo libro base.
     * <p>
     * L'identificazione del suggerimento avviene tramite la coppia
     * <code>(userid, bookId)</code>.
     *
     * @param userid identificatore dell'utente
     * @param bookId identificatore del libro base a cui sono associati i suggerimenti
     * @return {@code true} se il suggerimento è stato trovato e rimosso,
     *         {@code false} se non esisteva alcun suggerimento per quei parametri
     * @throws Exception in caso di errore di I/O durante lettura o scrittura del file
     */
    public boolean deleteSuggestion(String userid, int bookId) throws Exception {
        List<Suggestion> all = loadAll();
        boolean removed = all.removeIf(s ->
                s.getUserid().equals(userid) && s.getBookId() == bookId);
        if (removed) saveAll(all);
        return removed;
    }

    // ============ IO SU FILE ============

    /**
     * Carica tutte le righe di suggerimenti dal file dei consigli.
     * <p>
     * Le righe vuote o con formato non valido vengono ignorate.
     *
     * @return lista di tutte le {@link Suggestion} presenti nel file;
     *         lista vuota se il file non esiste
     * @throws Exception in caso di errore di I/O durante la lettura del file
     */
    private List<Suggestion> loadAll() throws Exception {
        List<Suggestion> list = new ArrayList<>();
        if (!Files.exists(fileConsigli)) return list;

        try (BufferedReader br = Files.newBufferedReader(fileConsigli)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(";", -1);
                if (c.length < 3) continue;
                String userid = c[0];
                int bookId = parseIntSafe(c[1]);
                List<Integer> ids = new ArrayList<>();
                if (!c[2].isBlank()) {
                    for (String p : c[2].split(",")) {
                        try {
                            ids.add(Integer.parseInt(p.trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                list.add(new Suggestion(userid, bookId, ids));
            }
        }
        return list;
    }

    /**
     * Sovrascrive completamente il file dei suggerimenti con la lista specificata.
     * <p>
     * Ogni suggerimento viene scritto su una riga nel formato:
     * <pre>
     * userid; bookId; idSug1,idSug2,...
     * </pre>
     *
     * @param list lista di {@link Suggestion} da salvare
     * @throws Exception in caso di errore di I/O durante la scrittura del file
     */
    private void saveAll(List<Suggestion> list) throws Exception {
        Files.createDirectories(fileConsigli.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(fileConsigli)) {
            for (Suggestion s : list) {
                String ids = s.getSuggeriti().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                bw.write(s.getUserid() + ";" + s.getBookId() + ";" + ids);
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
