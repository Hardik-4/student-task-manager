package ie.griffith.tasks;

import ie.griffith.tasks.dao.Database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

/** Creates a fresh H2 in-memory database (MySQL mode) using db/schema.sql, including its sample tasks. */
final class TestDb {
    private TestDb() { }

    static Database create() {
        try {
            Database db = new Database("jdbc:h2:mem:t" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
            String sql = Files.readString(Path.of("db/schema.sql")).replaceAll("--[^\\n]*", ""); // drop comments
            try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
                for (String statement : sql.split(";")) {
                    if (!statement.isBlank()) st.execute(statement);
                }
            }
            return db;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
