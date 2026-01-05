package bookrecommender.service;

import bookrecommender.model.Book;
import bookrecommender.repo.LibriRepository;

import java.util.List;

public class SearchService {

    private final LibriRepository libriRepo;

    public SearchService(LibriRepository libriRepo) {
        this.libriRepo = libriRepo;
    }

    public List<Book> cercaLibroPerTitolo(String q, int limit) {
        return libriRepo.searchByTitleContains(q, limit);
    }

    public List<Book> cercaLibroPerAutore(String a, int limit) {
        return libriRepo.searchByAuthorContains(a, limit);
    }

    /**
     * Implementazione per SEARCH_BY_AUTHOR_YEAR
     */
    public List<Book> cercaLibroPerAutoreEAnno(String autore, int anno, int limit) {
        if (autore == null || autore.isBlank()) return List.of();
        return libriRepo.searchByAuthorAndYear(autore, anno, limit);
    }

    // Se vuoi tenere la versione "default limit=50" (comoda):
    public List<Book> cercaLibroPerAutoreEAnno(String autore, int anno) {
        return cercaLibroPerAutoreEAnno(autore, anno, 50);
    }
}
