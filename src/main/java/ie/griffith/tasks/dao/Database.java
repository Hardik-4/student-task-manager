package ie.griffith.tasks.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Opens database connections. The app reads db.properties; the tests pass an H2 URL instead. */
public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Database fromProperties() {
        Properties p = new Properties();
        try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
            p.load(in);
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("Could not read db.properties", e);
        }
        return new Database(p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.password"));
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
