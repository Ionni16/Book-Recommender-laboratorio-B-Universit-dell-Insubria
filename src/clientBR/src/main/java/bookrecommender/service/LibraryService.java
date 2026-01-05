package bookrecommender.service;

import bookrecommender.model.Library;
import bookrecommender.net.BRProxy;
import bookrecommender.net.Request;
import bookrecommender.net.Response;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class LibraryService {

    private final BRProxy proxy;

    public LibraryService(Path ignored) {
        this.proxy = new BRProxy("127.0.0.1", 5050);
    }

    @SuppressWarnings("unchecked")
    public List<Library> listUserLibraries(String userid) {
        Response res = proxy.call(Request.listLibrariesByUser(userid));
        if (!res.ok) throw new RuntimeException(res.error);
        if (res.data == null) return Collections.emptyList();
        return (List<Library>) res.data;
    }

    public boolean saveLibrary(Library lib) {
        Response res = proxy.call(Request.saveLibrary(lib));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }

    public boolean deleteLibrary(String userid, String nome) {
        Response res = proxy.call(Request.deleteLibrary(userid, nome));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }
}
