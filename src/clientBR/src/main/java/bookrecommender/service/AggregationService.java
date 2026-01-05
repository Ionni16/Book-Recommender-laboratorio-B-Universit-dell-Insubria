package bookrecommender.service;

import bookrecommender.model.Review;
import bookrecommender.model.Suggestion;
import bookrecommender.net.BRProxy;
import bookrecommender.net.Request;
import bookrecommender.net.Response;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class AggregationService {

    private final BRProxy proxy;

    public AggregationService(Path ignoredValutazioni, Path ignoredConsigli) {
        this.proxy = new BRProxy("127.0.0.1", 5050);
    }

    public static class ReviewStats {
        public int count;
        public final Map<Integer,Integer> distribuzioneVoti = new TreeMap<>();
        public double mediaStile, mediaContenuto, mediaGradevolezza, mediaOriginalita, mediaEdizione;
        public double mediaVotoFinale;
    }

    public static class SuggestionsStats {
        public Map<Integer,Integer> suggeritiCount = new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public ReviewStats getReviewStats(int bookId) {
        Response res = proxy.call(Request.getReviewsByBook(bookId));
        if (!res.ok) throw new RuntimeException(res.error);
        List<Review> reviews = (res.data == null) ? List.of() : (List<Review>) res.data;

        ReviewStats s = new ReviewStats();
        s.count = reviews.size();
        if (s.count == 0) return s;

        int sumS=0, sumC=0, sumG=0, sumO=0, sumE=0, sumVF=0;
        for (Review r : reviews) {
            s.distribuzioneVoti.merge(r.getVotoFinale(), 1, Integer::sum);
            sumS += r.getStile();
            sumC += r.getContenuto();
            sumG += r.getGradevolezza();
            sumO += r.getOriginalita();
            sumE += r.getEdizione();
            sumVF += r.getVotoFinale();
        }
        s.mediaStile        = sumS / (double)s.count;
        s.mediaContenuto    = sumC / (double)s.count;
        s.mediaGradevolezza = sumG / (double)s.count;
        s.mediaOriginalita  = sumO / (double)s.count;
        s.mediaEdizione     = sumE / (double)s.count;
        s.mediaVotoFinale   = sumVF / (double)s.count;
        return s;
    }

    @SuppressWarnings("unchecked")
    public SuggestionsStats getSuggestionsStats(int bookId) {
        Response res = proxy.call(Request.getSuggestionsByBook(bookId));
        if (!res.ok) throw new RuntimeException(res.error);
        List<Suggestion> sugs = (res.data == null) ? List.of() : (List<Suggestion>) res.data;

        SuggestionsStats s = new SuggestionsStats();
        for (Suggestion sg : sugs) {
            for (Integer sug : sg.getSuggeriti()) {
                s.suggeritiCount.merge(sug, 1, Integer::sum);
            }
        }

        s.suggeritiCount = s.suggeritiCount.entrySet().stream()
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x,y) -> x,
                        LinkedHashMap::new
                ));
        return s;
    }
}
