package ie.griffith.tasks.dao;

import ie.griffith.tasks.model.Task;

import java.util.List;
import java.util.Optional;

/**
 * DAO (Data Access Object) interface: WHAT we can do with stored tasks.
 * The rest of the app only uses this interface, not the SQL behind it.
 */
public interface TaskDao {
    Task add(Task task);
    boolean update(Task task);
    boolean delete(int id);
    Optional<Task> findById(int id);
    List<Task> findAll();
}
