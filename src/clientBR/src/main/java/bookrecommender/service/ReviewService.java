package bookrecommender.service;

import bookrecommender.model.Review;
import bookrecommender.net.BRProxy;
import bookrecommender.net.Request;
import bookrecommender.net.Response;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class ReviewService {

    private final BRProxy proxy;

    public ReviewService(Path ignoredValutazioni, Path ignoredLibrerie) {
        this.proxy = new BRProxy("127.0.0.1", 5050);
    }

    @SuppressWarnings("unchecked")
    public List<Review> listByUser(String userid) {
        Response res = proxy.call(Request.listReviewsByUser(userid));
        if (!res.ok) throw new RuntimeException(res.error);
        if (res.data == null) return Collections.emptyList();
        return (List<Review>) res.data;
    }

    public boolean inserisciValutazione(Review r) {
        Response res = proxy.call(Request.saveReview(r));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }

    public boolean updateReview(Review r) {
        Response res = proxy.call(Request.saveReview(r));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }

    public boolean deleteReview(String userid, int bookId) {
        Response res = proxy.call(Request.deleteReview(userid, bookId));
        if (!res.ok) return false;
        return Boolean.TRUE.equals(res.data) || res.data == null;
    }
}
