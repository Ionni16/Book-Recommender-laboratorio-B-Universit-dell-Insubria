package bookrecommender.service;

import bookrecommender.model.Review;
import bookrecommender.model.Suggestion;
import bookrecommender.repo.ConsigliRepository;
import bookrecommender.repo.ValutazioniRepository;

import java.util.*;

/**
 * La classe AggregationService fornisce metodi per calcolare
 * statistiche aggregate sulle valutazioni e sui suggerimenti dei libri.
 *
 * Versione DB-only: legge i dati tramite repository JDBC (PostgreSQL),
 * NON più da file.
 */
public class AggregationService {

    private final ValutazioniRepository valRepo;
    private final ConsigliRepository consRepo;

    /**
     * Costruisce un nuovo servizio di aggregazione usando i repository DB.
     *
     * @param valRepo  repository valutazioni (DB)
     * @param consRepo repository consigli (DB)
     */
    public AggregationService(ValutazioniRepository valRepo, ConsigliRepository consRepo) {
        this.valRepo = valRepo;
        this.consRepo = consRepo;
    }

    /**
     * Struttura dati che rappresenta le statistiche aggregate sulle valutazioni
     * di un singolo libro.
     */
    public static class ReviewStats {
        public int count;

        // votoFinale -> count
        public final Map<Integer, Integer> distribuzioneVoti = new TreeMap<>();

        public double mediaStile, mediaContenuto, mediaGradevolezza, mediaOriginalita, mediaEdizione;
        public double mediaVotoFinale;
    }

    /**
     * Statistiche aggregate sui suggerimenti: suggerito_id -> numero utenti che lo suggeriscono.
     */
    public static class SuggestionsStats {
        public Map<Integer, Integer> suggeritiCount = new LinkedHashMap<>();
    }

    /**
     * Calcola le statistiche aggregate sulle valutazioni per un determinato libro.
     *
     * @param bookId id libro
     * @return ReviewStats con medie e distribuzione
     */
    public ReviewStats getReviewStats(int bookId) {
        List<Review> reviews = valRepo.findByBookId(bookId);

        ReviewStats s = new ReviewStats();
        s.count = reviews.size();
        if (s.count == 0) return s;

        int sumS = 0, sumC = 0, sumG = 0, sumO = 0, sumE = 0, sumVF = 0;

        for (Review r : reviews) {
            s.distribuzioneVoti.merge(r.getVotoFinale(), 1, Integer::sum);

            sumS += r.getStile();
            sumC += r.getContenuto();
            sumG += r.getGradevolezza();
            sumO += r.getOriginalita();
            sumE += r.getEdizione();
            sumVF += r.getVotoFinale();
        }

        s.mediaStile        = sumS / (double) s.count;
        s.mediaContenuto    = sumC / (double) s.count;
        s.mediaGradevolezza = sumG / (double) s.count;
        s.mediaOriginalita  = sumO / (double) s.count;
        s.mediaEdizione     = sumE / (double) s.count;
        s.mediaVotoFinale   = sumVF / (double) s.count;

        return s;
    }

    /**
     * Calcola le statistiche aggregate sui suggerimenti per un determinato libro base.
     *
     * @param bookId id libro base
     * @return SuggestionsStats con conteggi suggerimenti ordinati (desc)
     */
    public SuggestionsStats getSuggestionsStats(int bookId) {
        List<Suggestion> sugs = consRepo.findByBookId(bookId);

        SuggestionsStats s = new SuggestionsStats();

        for (Suggestion sg : sugs) {
            for (Integer sug : sg.getSuggeriti()) {
                s.suggeritiCount.merge(sug, 1, Integer::sum);
            }
        }

        // Ordina per conteggio decrescente
        s.suggeritiCount = s.suggeritiCount.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);

        return s;
    }
}
