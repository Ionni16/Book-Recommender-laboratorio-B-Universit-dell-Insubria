package bookrecommender.service;

import bookrecommender.model.Book;
import bookrecommender.net.BRProxy;
import bookrecommender.net.Request;
import bookrecommender.net.Response;
import bookrecommender.repo.LibriRepository;

import java.util.Collections;
import java.util.List;

public class SearchService {

    private final BRProxy proxy;

    public SearchService(LibriRepository ignored) {
        // La GUI del LabA passa ancora LibriRepository: lo ignoriamo (per compatibilità).
        this.proxy = new BRProxy("127.0.0.1", 5050);
    }

    @SuppressWarnings("unchecked")
    public List<Book> cercaLibroPerTitolo(String titolo) {
        Response res = proxy.call(Request.searchByTitle(titolo));
        if (!res.ok) {
            // se vuoi: mostrare alert in UI; per ora lanciamo runtime
            throw new RuntimeException(res.error);
        }
        if (res.data == null) return Collections.emptyList();
        return (List<Book>) res.data;
    }

    @SuppressWarnings("unchecked")
    public List<Book> cercaLibroPerAutore(String autore) {
        Response res = proxy.call(Request.searchByAuthor(autore));
        if (!res.ok) {
            throw new RuntimeException(res.error);
        }
        if (res.data == null) return Collections.emptyList();
        return (List<Book>) res.data;
    }
}
