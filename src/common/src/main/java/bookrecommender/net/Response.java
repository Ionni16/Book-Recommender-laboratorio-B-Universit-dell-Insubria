package bookrecommender.net;

import java.io.Serializable;

public class Response implements Serializable {
    public final boolean ok;
    public final Object data;
    public final String error;

    private Response(boolean ok, Object data, String error) {
        this.ok = ok;
        this.data = data;
        this.error = error;
    }

    public static Response ok(Object data) { return new Response(true, data, null); }
    public static Response fail(String error) { return new Response(false, null, error); }
}
