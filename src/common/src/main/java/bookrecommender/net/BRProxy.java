package bookrecommender.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Proxy client per il protocollo di rete di Book Recommender.
 *
 * VERSIONE OTTIMIZZATA:
 * - connessione persistente (niente socket nuova ad ogni call)
 * - timeout di connect/read
 * - riconnessione automatica in caso di errore
 */
@SuppressWarnings("ClassCanBeRecord")
public class BRProxy {

    private final String host;
    private final int port;

    // timeout “ragionevoli” (modificabili)
    private final int connectTimeoutMs = 1500;
    private final int readTimeoutMs = 2500;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public BRProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private void ensureConnected() throws Exception {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return;

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
        socket.setSoTimeout(readTimeoutMs);

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    public synchronized Response call(Request req) {
        try {
            ensureConnected();

            out.writeObject(req);
            out.flush();

            Object obj = in.readObject();
            if (!(obj instanceof Response res)) {
                return Response.fail("Risposta non valida dal server");
            }
            return res;

        } catch (Exception e) {
            // forza riconnessione al prossimo giro
            close();
            return Response.fail("Connessione fallita: " + e.getMessage());
        }
    }

    public synchronized void close() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        in = null;
        out = null;
        socket = null;
    }
}
