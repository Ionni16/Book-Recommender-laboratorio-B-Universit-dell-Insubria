package bookrecommender.net;

import java.io.Serializable;

/**
 * Rappresenta la risposta del server a una {@link Request}.
 * <p>
 * La classe {@code Response} è parte del protocollo di comunicazione
 * client/server ed è condivisa tra client e server (modulo <code>common</code>).
 * Ogni richiesta inviata dal client riceve sempre una {@code Response}.
 * </p>
 *
 * <p>
 * Una risposta può essere:
 * </p>
 * <ul>
 *     <li><b>Positiva</b> ({@code ok == true}): contiene un risultato valido in {@link #data};</li>
 *     <li><b>Negativa</b> ({@code ok == false}): contiene un messaggio di errore in {@link #error}.</li>
 * </ul>
 *
 * <p>
 * Il campo {@link #data} è tipizzato come {@link Object} per permettere
 * al protocollo di trasportare risultati eterogenei
 * (liste, singoli oggetti, valori primitivi boxed, ecc.).
 * </p>
 *
 * <b>Convenzione:</b>
    <ul>
        <li>se {@code ok == true} → {@code error == null}</li>
        <li>se {@code ok == false} → {@code data == null}</li>
    </ul>

 *
 * @author Richard Zefi
 * @version 1.0
 * @see Request
 * @see RequestType
 */
@SuppressWarnings("ClassCanBeRecord")
public class Response implements Serializable {


    public final boolean ok;
    public final Object data;
    public final String error;

    /**
     * Costruttore privato.
     * <p>
     * Le istanze di {@code Response} devono essere create esclusivamente
     * tramite i metodi factory {@link #ok(Object)} e {@link #fail(String)}.
     * </p>
     *
     * @param ok    esito dell'operazione
     * @param data  dato restituito (solo se {@code ok == true})
     * @param error messaggio di errore (solo se {@code ok == false})
     */
    private Response(boolean ok, Object data, String error) {
        this.ok = ok;
        this.data = data;
        this.error = error;
    }

    /**
     * Crea una risposta di successo.
     *
     * @param data dato da restituire al client
     * @return una {@code Response} con {@code ok == true}
     */
    public static Response ok(Object data) {
        return new Response(true, data, null);
    }

    /**
     * Crea una risposta di errore.
     *
     * @param error messaggio di errore descrittivo
     * @return una {@code Response} con {@code ok == false}
     */
    public static Response fail(String error) {
        return new Response(false, null, error);
    }
}
