package bookrecommender.service;

import bookrecommender.model.Suggestion;
import bookrecommender.net.BRProxy;
import bookrecommender.net.Request;
import bookrecommender.net.Response;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class SuggestionService {

    private final BRProxy proxy;

    public SuggestionService(Path ignoredConsigli, Path ignoredLibrerie) {
        this.proxy = new BRProxy("127.0.0.1", 5050);
    }

    public boolean inserisciSuggerimento(Suggestion s) {
        Response res = proxy.call(Request.saveSuggestion(s));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }

    @SuppressWarnings("unchecked")
    public List<Suggestion> listByUser(String userid) {
        Response res = proxy.call(Request.listSuggestionsByUser(userid));
        if (!res.ok) throw new RuntimeException(res.error);
        if (res.data == null) return Collections.emptyList();
        return (List<Suggestion>) res.data;
    }

    public boolean deleteSuggestion(String userid, int bookId) {
        Response res = proxy.call(Request.deleteSuggestion(userid, bookId));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }
}
