package bookrecommender.repo;

import bookrecommender.model.Book;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LibriRepository {

    @SuppressWarnings("unused")
    private final Path ignoredPath;

    // mini catalogo in memoria (vuoto). Più avanti lo riempiremo dal server.
    private final Map<Integer, Book> byId = new ConcurrentHashMap<>();

    public LibriRepository(Path ignoredPath) {
        this.ignoredPath = ignoredPath;
    }

    public void load() {
        // stub: per ora NON carica da file.
        // Quando faremo il server, qui chiederemo al server un catalogo o faremo le ricerche remote.
    }

    public int size() {
        return byId.size();
    }

    public List<Book> all() {
        return new ArrayList<>(byId.values());
    }

    public Book findById(Integer id) {
        if (id == null) return null;
        return byId.get(id);
    }

    // ricerca “stub” (ora lavora sul catalogo in memoria)
    public List<Book> searchByTitle(String title) {
        if (title == null) return List.of();
        String t = title.toLowerCase();
        List<Book> out = new ArrayList<>();
        for (Book b : byId.values()) {
            if (b.getTitolo() != null && b.getTitolo().toLowerCase().contains(t)) out.add(b);
        }
        return out;
    }

    public List<Book> searchByAuthor(String author) {
        if (author == null) return List.of();
        String a = author.toLowerCase();
        List<Book> out = new ArrayList<>();
        for (Book b : byId.values()) {
            for (String au : b.getAutori()) {
                if (au != null && au.toLowerCase().contains(a)) {
                    out.add(b);
                    break;
                }
            }
        }
        return out;
    }
}
