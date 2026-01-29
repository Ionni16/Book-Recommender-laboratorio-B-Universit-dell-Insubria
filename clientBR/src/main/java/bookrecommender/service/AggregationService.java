package bookrecommender.service;

import bookrecommender.model.Review;
import bookrecommender.model.Suggestion;
import bookrecommender.net.Request;
import bookrecommender.net.Response;
import bookrecommender.net.BRProxy;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Servizio di aggregazione statistiche per recensioni e suggerimenti.
 * <p>
 * Interroga il server tramite {@link BRProxy} e calcola:
 * </p>
 * <ul>
 *   <li>Statistiche sulle recensioni di un libro (conteggio, distribuzione voti, medie)</li>
 *   <li>Statistiche sui suggerimenti (frequenza dei libri suggeriti, ordinata per occorrenze)</li>
 * </ul>
 *
 * <p>
 * I percorsi passati al costruttore sono attualmente ignorati: il servizio lavora tramite rete
 * (chiamate <code>proxy.call(...)</code>) e non legge file locali.
 * </p>
 *
 * @author Matteo Ferrario
 * @version 1.0
 * @see BRProxy
 * @see Request
 * @see Response
 */
public class AggregationService {

    /** Proxy di rete usato per interrogare il server. */
    private final BRProxy proxy;

    public AggregationService(BRProxy proxy) {
        this.proxy = proxy;
    }


    /**
     * Crea un nuovo servizio di aggregazione.
     * <p>
     * I parametri <code>ignoredValutazioni</code> e <code>ignoredConsigli</code> sono presenti per compatibilità
     * con un'architettura precedente/file-based, ma non vengono utilizzati in questa implementazione.
     * </p>
     *
     * @param ignoredValutazioni percorso (ignorato) delle valutazioni/recensioni
     * @param ignoredConsigli percorso (ignorato) dei suggerimenti/consigli
     */
    public AggregationService(Path ignoredValutazioni, Path ignoredConsigli) {
        this(new BRProxy("127.0.0.1", 5050));
    }

    /**
     * Statistiche aggregate sulle recensioni di un libro.
     * <ul>
     *   <li><code>count</code>: numero di recensioni</li>
     *   <li><code>distribuzioneVoti</code>: mappa voto finale → occorrenze</li>
     *   <li>Medie dei singoli aspetti e del voto finale</li>
     * </ul>
     */
    public static class ReviewStats {
        /** Numero di recensioni. */
        public int count;

        /** Distribuzione dei voti finali: <code>votoFinale -&gt; occorrenze</code>. */
        public final Map<Integer, Integer> distribuzioneVoti = new TreeMap<>();

        /** Medie dei singoli aspetti. */
        public double mediaStile, mediaContenuto, mediaGradevolezza, mediaOriginalita, mediaEdizione;

        /** Media del voto finale. */
        public double mediaVotoFinale;
    }

    /**
     * Statistiche aggregate sui suggerimenti di un libro.
     * <p>
     * <code>suggeritiCount</code> Contiene la frequenza con cui ciascun libro viene suggerito,
     * ordinata in modo decrescente per occorrenze.
     * </p>
     */
    public static class SuggestionsStats {
        /** Frequenza dei libri suggeriti: <code>bookIdSuggerito -&gt; occorrenze</code>. */
        public Map<Integer, Integer> suggeritiCount = new LinkedHashMap<>();
    }

    /**
     * Calcola statistiche sulle recensioni associate a un libro.
     * <p>
     * Recupera le recensioni con <code>Request.getReviewsByBook(bookId)</code> e calcola:
     * conteggio, distribuzione dei voti finali e medie degli aspetti.
     * </p>
     *
     * @param bookId id del libro
     * @return statistiche aggregate sulle recensioni; se non ci sono recensioni, tutte le medie restano 0
     * @throws RuntimeException se la risposta del server non è OK
     * @see Request#getReviewsByBook(int)
     */
    @SuppressWarnings("unchecked")
    public ReviewStats getReviewStats(int bookId) {
        Response res = proxy.call(Request.getReviewsByBook(bookId));
        if (!res.ok) throw new RuntimeException(res.error);
        List<Review> reviews = (res.data == null) ? List.of() : (List<Review>) res.data;

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
        s.mediaStile = sumS / (double) s.count;
        s.mediaContenuto = sumC / (double) s.count;
        s.mediaGradevolezza = sumG / (double) s.count;
        s.mediaOriginalita = sumO / (double) s.count;
        s.mediaEdizione = sumE / (double) s.count;
        s.mediaVotoFinale = sumVF / (double) s.count;
        return s;
    }

    public ReviewStats computeReviewStats(List<Review> reviews) {
        ReviewStats s = new ReviewStats();
        s.count = (reviews == null) ? 0 : reviews.size();
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
        s.mediaStile = sumS / (double) s.count;
        s.mediaContenuto = sumC / (double) s.count;
        s.mediaGradevolezza = sumG / (double) s.count;
        s.mediaOriginalita = sumO / (double) s.count;
        s.mediaEdizione = sumE / (double) s.count;
        s.mediaVotoFinale = sumVF / (double) s.count;
        return s;
    }


    /**
     * Calcola statistiche sui suggerimenti associati a un libro.
     * <p>
     * Recupera i suggerimenti con <code>Request.getSuggestionsByBook(bookId)</code>, conta le occorrenze dei libri
     * suggeriti e ordina il risultato in modo decrescente per frequenza.
     * </p>
     *
     * @param bookId id del libro
     * @return statistiche aggregate sui suggerimenti (mappa ordinata per occorrenze decrescenti)
     * @throws RuntimeException se la risposta del server non è OK
     * @see Request#getSuggestionsByBook(int)
     */
    @SuppressWarnings("unchecked")
    public SuggestionsStats getSuggestionsStats(int bookId) {
        Response res = proxy.call(Request.getSuggestionsByBook(bookId));
        if (!res.ok) throw new RuntimeException(res.error);
        List<Suggestion> suggestions = (res.data == null) ? List.of() : (List<Suggestion>) res.data;

        SuggestionsStats s = new SuggestionsStats();
        for (Suggestion sg : suggestions) {
            for (Integer sug : sg.getSuggeriti()) {
                s.suggeritiCount.merge(sug, 1, Integer::sum);
            }
        }

        s.suggeritiCount = s.suggeritiCount.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> x,
                        LinkedHashMap::new
                ));
        return s;
    }
}
