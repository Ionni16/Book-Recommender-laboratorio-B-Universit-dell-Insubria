package bookrecommender.util;

import bookrecommender.model.Library;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe di utilità per operazioni di supporto sui dati delle librerie.
 * <p>
 * Fornisce metodi statici per:
 * <ul>
 *     <li>Caricare le librerie degli utenti da file;</li>
 *     <li>Verificare se un determinato libro è presente
 *     in almeno una libreria di un utente.</li>
 * </ul>
 * <p>
 * La classe lavora direttamente sui file di persistenza
 * (es. {@code Librerie.dati}) ed è pensata come supporto
 * ai servizi applicativi lato server.
 *
 * @author Richard Zefi
 * @version 1.0
 * @see bookrecommender.model.Library
 */
public class Utilities {

    /**
     * Verifica se un utente ha un libro nella sua libreria.
     *
     * @param userid      identificatore dell'utente
     * @param bookId      identificatore del libro
     * @param fileLibrerie percorso del file delle librerie
     * @return true se l'utente ha il libro in almeno una libreria, false altrimenti
     * @throws Exception in caso di errore di I/O
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean utenteHaLibroInLibreria(String userid, int bookId, Path fileLibrerie) throws Exception {
        List<Library> allLibraries = loadAllLibraries(fileLibrerie);
        return allLibraries.stream()
                .filter(lib -> lib.getUserid().equals(userid))
                .anyMatch(lib -> lib.getBookIds().contains(bookId));
    }

    /**
     * Carica tutte le librerie dal file.
     *
     * @param file percorso del file Librerie.dati
     * @return lista di tutte le librerie
     * @throws Exception in caso di errore di I/O
     */
    private static List<Library> loadAllLibraries(Path file) throws Exception {
        try (BufferedReader br = Files.newBufferedReader(file)) {
            return br.lines()
                    .map(Utilities::parseLibrary)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Parsa una riga del file librerie in un oggetto Library.
     *
     * @param line riga del file
     * @return Library o null se parsing fallisce
     */
    private static Library parseLibrary(String line) {
        String[] parts = line.split(";");
        if (parts.length < 3) return null;
        String userid = parts[0].trim();
        String nome = parts[1].trim();
        Set<Integer> bookIds = Arrays.stream(parts[2].split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new Library(userid, nome, bookIds);
    }
}