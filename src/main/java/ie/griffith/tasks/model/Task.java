package ie.griffith.tasks.model;

import java.time.LocalDate;

/**
 * One task. A record is an immutable class: Java writes the constructor, getters,
 * equals, hashCode and toString for us. id is 0 until the task is saved.
 */
public record Task(int id, String title, String description, LocalDate deadline,
                   Priority priority, String category, boolean completed) {

    /** A task is overdue if it is not done and its deadline is before today. */
    public boolean isOverdue(LocalDate today) {
        return !completed && deadline.isBefore(today);
    }

    /** Returns a copy with a different id (used after the database creates one). */
    public Task withId(int newId) {
        return new Task(newId, title, description, deadline, priority, category, completed);
    }

    public Task withCompleted(boolean done) {
        return new Task(id, title, description, deadline, priority, category, done);
    }
}
