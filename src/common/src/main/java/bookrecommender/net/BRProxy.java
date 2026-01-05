package bookrecommender.net;

import java.io.*;
import java.net.Socket;

public class BRProxy {

    private final String host;
    private final int port;

    public BRProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

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