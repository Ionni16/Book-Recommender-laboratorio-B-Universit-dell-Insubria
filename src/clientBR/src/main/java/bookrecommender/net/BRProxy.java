package bookrecommender.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Proxy client per il protocollo di rete di Book Recommender.
 * <p>
 * Questa classe incapsula una singola chiamata sincrona verso il server:
 * apre una {@link Socket}, invia un {@link Request} tramite {@link ObjectOutputStream},
 * e legge una {@link Response} tramite {@link ObjectInputStream}.
 * </p>
 *
 * <h2>Note</h2>
 * <ul>
 *   <li>Il metodo {@link #call(Request)} crea una nuova connessione per ogni invocazione.</li>
 *   <li>In caso di errore (I/O, de serializzazione, server non raggiungibile) ritorna una
 *       {@link Response} di fallimento tramite <code>Response.fail(...)</code>.</li>
 * </ul>
 *
 * @author Richard Zefi
 * @version 1.0
 * @see Request
 * @see Response
 * @see Socket
 */
public class BRProxy {

    /** Host del server a cui connettersi. */
    private final String host;

    /** Porta TCP del server a cui connettersi. */
    private final int port;

    /**
     * Crea un nuovo proxy configurato per un server specifico.
     *
     * @param host host o indirizzo del server (es. <code>"localhost"</code>)
     * @param port porta TCP del server
     */
    public BRProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Invia una richiesta al server e ritorna la risposta.
     * <p>
     * La richiesta viene serializzata con <code>ObjectOutputStream</code> e la risposta viene
     * deserializzata con <code>ObjectInputStream</code>.
     * </p>
     *
     * @param req richiesta da inviare (non dovrebbe essere <code>null</code>)
     * @return la {@link Response} del server; se la risposta non è valida o si verifica un errore,
     *         ritorna una risposta di fallimento tramite <code>Response.fail(...)</code>
     * @see ObjectOutputStream#writeObject(Object)
     * @see ObjectInputStream#readObject()
     */
    public Response call(Request req) {
        try (Socket s = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

            out.writeObject(req);
            out.flush();

            Object obj = in.readObject();
            if (!(obj instanceof Response res)) {
                return Response.fail("Risposta non valida dal server");
            }
            return res;

        } catch (Exception e) {
            return Response.fail("Connessione fallita: " + e.getMessage());
        }
    }
}
