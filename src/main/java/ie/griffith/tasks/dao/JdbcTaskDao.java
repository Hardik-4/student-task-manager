package ie.griffith.tasks.dao;

import ie.griffith.tasks.model.Priority;
import ie.griffith.tasks.model.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TaskDao implemented with JDBC. Every query uses a PreparedStatement with ? placeholders,
 * so text typed by the user can never change the SQL (no SQL injection).
 * try-with-resources closes connections and statements automatically.
 */
public class JdbcTaskDao implements TaskDao {
    private final Database db;

    public JdbcTaskDao(Database db) {
        this.db = db;
    }

    @Override
    public Task add(Task t) {
        String sql = "INSERT INTO task (title, description, deadline, priority, category, completed) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, t);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return t.withId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not add task", e);
        }
    }

    @Override
    public boolean update(Task t) {
        String sql = "UPDATE task SET title = ?, description = ?, deadline = ?, priority = ?, category = ?, completed = ? "
                + "WHERE task_id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, t);
            ps.setInt(7, t.id());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update task", e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM task WHERE task_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete task", e);
        }
    }

    @Override
    public Optional<Task> findById(int id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM task WHERE task_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load task", e);
        }
    }

    @Override
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM task ORDER BY deadline, task_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) tasks.add(map(rs));
            return tasks;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load tasks", e);
        }
    }

    /** Puts a task's fields into parameters 1-6 (shared by INSERT and UPDATE). */
    private static void fill(PreparedStatement ps, Task t) throws SQLException {
        ps.setString(1, t.title());
        ps.setString(2, t.description());
        ps.setDate(3, Date.valueOf(t.deadline()));
        ps.setString(4, t.priority().name());
        ps.setString(5, t.category());
        ps.setBoolean(6, t.completed());
    }

    /** Turns the current database row into a Task object. */
    private static Task map(ResultSet rs) throws SQLException {
        return new Task(rs.getInt("task_id"), rs.getString("title"), rs.getString("description"),
                rs.getDate("deadline").toLocalDate(), Priority.valueOf(rs.getString("priority")),
                rs.getString("category"), rs.getBoolean("completed"));
    }
}
