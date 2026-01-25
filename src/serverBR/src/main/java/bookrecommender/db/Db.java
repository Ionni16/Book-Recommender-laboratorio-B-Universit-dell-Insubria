package bookrecommender.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe di utilità per la gestione della connessione al database.
 * <p>
 * Incapsula i parametri di connessione (URL, utente, password) e fornisce
 * un metodo per ottenere una nuova {@link Connection} JDBC verso il database
 * PostgreSQL.
 * </p>
 *
 * <p>
 * La classe NON gestisce:
 * <ul>
 *   <li>Connection pooling</li>
 *   <li>Chiusura automatica delle connessioni</li>
 *   <li>Retry o logging</li>
 * </ul>
 * Queste responsabilità sono demandate ai livelli superiori dell’applicazione.
 * </p>
 *
 * @author Ionuț Puiu
 */
@SuppressWarnings("ClassCanBeRecord")
public class Db {

    /** JDBC URL del database (es. jdbc:postgresql://localhost:5432/bookrecommender) */
    private final String url;

    /** Username per l’accesso al database */
    private final String user;

    /** Password per l’accesso al database */
    private final String password;

    /**
     * Costruisce un nuovo oggetto {@code Db} con i parametri di connessione.
     *
     * @param url      URL JDBC del database
     * @param user     nome utente del database
     * @param password password del database
     */
    public Db(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Restituisce una nuova connessione al database.
     *
     * <p>
     * Ogni chiamata crea una nuova {@link Connection} tramite
     * {@link DriverManager#getConnection(String, String, String)}.
     * </p>
     *
     * @return una nuova connessione JDBC al database
     * @throws SQLException se la connessione fallisce
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
