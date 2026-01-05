package bookrecommender.service;

import bookrecommender.model.Library;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * La classe <code>LibraryService</code> gestisce le librerie personali
 * degli utenti.
 * <p>
 * I dati sono memorizzati nel file <code>Librerie.dati</code> con righe nel formato:
 * <pre>
 * userid; nomeLibreria; id1,id2,id3,...
 * </pre>
 * Dove:
 * <ul>
 *     <li><code>userid</code> è l'identificatore dell'utente;</li>
 *     <li><code>nomeLibreria</code> è il nome assegnato alla libreria;</li>
 *     <li><code>id1,id2,id3,...</code> è la lista degli ID dei libri presenti,
 *         separati da virgole.</li>
 * </ul>
 *
 * @author Hamdi Kebeli
 * @version 1.0
 * @see bookrecommender.model.Library
 * @see bookrecommender.repo.LibriRepository
 */
public class LibraryService {

    private final Path file;


    /**
     * Costruisce un nuovo servizio di gestione delle librerie personali.
     *
     * @param file      percorso del file <code>Librerie.dati</code>
     *
     */
    public LibraryService(Path file) {
        this.file = file;
        // non usato per IO ma comodo
    }

    // ============ API USATA DALLA GUI ============

    /**
     * Restituisce tutte le librerie appartenenti a un determinato utente.
     *
     * @param userid identificatore dell'utente
     * @return lista di {@link Library} associate allo <code>userid</code> indicato;
     *         lista vuota se l'utente non possiede librerie
     * @throws Exception in caso di errore di I/O durante la lettura del file
     */
    public List<Library> listUserLibraries(String userid) throws Exception {
        return loadAll().stream()
                .filter(l -> l.getUserid().equals(userid))
                .collect(Collectors.toList());
    }


    /**
     * Salva o aggiorna una libreria (<em>upsert</em>).
     * <p>
     * Se per lo stesso utente esiste già una libreria con lo stesso nome,
     * essa viene sovrascritta; altrimenti la libreria viene aggiunta come nuova.
     *
     * @param lib oggetto {@link Library} da salvare o aggiornare
     * @return sempre {@code true} al termine dell'operazione
     * @throws Exception in caso di errore di I/O durante lettura/scrittura del file
     */
    public boolean saveLibrary(Library lib) throws Exception {
        List<Library> all = loadAll();
        boolean updated = false;
        List<Library> newAll = new ArrayList<>();
        for (Library l : all) {
            if (l.getUserid().equals(lib.getUserid()) &&
                l.getNome().equals(lib.getNome())) {
                newAll.add(lib);
                updated = true;
            } else {
                newAll.add(l);
            }
        }
        if (!updated) newAll.add(lib);
        saveAll(newAll);
        return updated;
    }


    /**
     * Elimina una libreria di un utente, identificata per nome.
     *
     * @param userid identificatore dell'utente proprietario della libreria
     * @param nome   nome della libreria da eliminare
     * @return {@code true} se almeno una libreria è stata trovata e rimossa,
     *         {@code false} se non esiste alcuna libreria con quel nome
     *         per l'utente indicato
     * @throws Exception in caso di errore di I/O durante lettura/scrittura del file
     */
    public boolean deleteLibrary(String userid, String nome) throws Exception {
        List<Library> all = loadAll();
        boolean removed = all.removeIf(l ->
                l.getUserid().equals(userid) && l.getNome().equals(nome));
        if (removed) saveAll(all);
        return removed;
    }

    // ============ IO SU FILE ============

    /**
     * Carica tutte le librerie presenti nel file delle librerie.
     * <p>
     * Le righe vuote o con formato non valido vengono ignorate.
     *
     * @return lista di tutte le {@link Library} presenti nel file;
     *         lista vuota se il file non esiste
     * @throws Exception in caso di errore di I/O durante la lettura del file
     */
    private List<Library> loadAll() throws Exception {
        List<Library> list = new ArrayList<>();
        if (!Files.exists(file)) return list;

        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(";", -1);
                if (c.length < 3) continue;
                String userid = c[0];
                String nome = c[1];
                Set<Integer> ids = new LinkedHashSet<>();
                if (!c[2].isBlank()) {
                    for (String p : c[2].split(",")) {
                        try {
                            ids.add(Integer.parseInt(p.trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                list.add(new Library(userid, nome, ids));
            }
        }
        return list;
    }


    /**
     * Sovrascrive completamente il file delle librerie con la lista specificata.
     * <p>
     * Ogni libreria viene scritta su una riga nel formato:
     * <pre>
     * userid; nomeLibreria; id1,id2,id3,...
     * </pre>
     *
     * @param libs lista di {@link Library} da salvare
     * @throws Exception in caso di errore di I/O durante la scrittura del file
     */
    private void saveAll(List<Library> libs) throws Exception {
        Files.createDirectories(file.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            for (Library l : libs) {
                String ids = l.getBookIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                bw.write(l.getUserid() + ";" + l.getNome() + ";" + ids);
                bw.newLine();
            }
        }
    }
}
