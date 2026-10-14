package ie.griffith.tasks.service;

import ie.griffith.tasks.dao.TaskDao;
import ie.griffith.tasks.model.Task;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The rules of the app: validating tasks and searching / filtering / sorting them.
 * It uses the TaskDao interface, so it doesn't care whether tasks are in MySQL or H2.
 */
public class TaskService {
    public static final int MAX_TITLE = 100;

    private final TaskDao dao;
    private final Clock clock; // lets tests pretend "today" is a fixed date

    public TaskService(TaskDao dao, Clock clock) {
        this.dao = dao;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public Task add(Task task) {
        return dao.add(validate(task));
    }

    public void update(Task task) {
        if (!dao.update(validate(task))) throw new IllegalArgumentException("Task " + task.id() + " no longer exists");
    }

    public void delete(int id) {
        if (!dao.delete(id)) throw new IllegalArgumentException("Task " + id + " no longer exists");
    }

    public void toggleCompleted(Task task) {
        update(task.withCompleted(!task.completed()));
    }

    /** All tasks that match the filter, in the chosen order. */
    public List<Task> search(TaskFilter f) {
        String text = f.text() == null ? "" : f.text().trim().toLowerCase(Locale.ROOT);
        return dao.findAll().stream()
                .filter(t -> text.isEmpty()
                        || t.title().toLowerCase(Locale.ROOT).contains(text)
                        || t.description().toLowerCase(Locale.ROOT).contains(text))
                .filter(t -> f.category() == null || t.category().equalsIgnoreCase(f.category()))
                .filter(t -> f.priority() == null || t.priority() == f.priority())
                .filter(t -> !f.hideDone() || !t.completed())
                .sorted(comparator(f.sortBy()))
                .toList();
    }

    /** The categories in use, alphabetically (for the filter drop-down). */
    public List<String> categories() {
        return dao.findAll().stream().map(Task::category).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public long overdueCount() {
        return dao.findAll().stream().filter(t -> t.isOverdue(today())).count();
    }

    static Comparator<Task> comparator(TaskFilter.SortBy sortBy) {
        // Ties are always broken by deadline, then title, so the order is predictable.
        Comparator<Task> byDeadline = Comparator.comparing(Task::deadline).thenComparing(Task::title, String.CASE_INSENSITIVE_ORDER);
        return switch (sortBy) {
            case DEADLINE -> byDeadline;
            case PRIORITY -> Comparator.comparing(Task::priority).thenComparing(byDeadline);
            case TITLE -> Comparator.comparing(Task::title, String.CASE_INSENSITIVE_ORDER).thenComparing(byDeadline);
            case CATEGORY -> Comparator.comparing(Task::category, String.CASE_INSENSITIVE_ORDER).thenComparing(byDeadline);
        };
    }

    /** Checks the task and returns a tidied copy (trimmed text). Throws with a friendly message if invalid. */
    static Task validate(Task t) {
        if (t.title() == null || t.title().isBlank()) throw new IllegalArgumentException("Title is required");
        if (t.title().trim().length() > MAX_TITLE) throw new IllegalArgumentException("Title must be at most " + MAX_TITLE + " characters");
        if (t.deadline() == null) throw new IllegalArgumentException("Deadline is required");
        if (t.priority() == null) throw new IllegalArgumentException("Priority is required");
        if (t.category() == null || t.category().isBlank()) throw new IllegalArgumentException("Category is required");
        String description = t.description() == null ? "" : t.description().trim();
        return new Task(t.id(), t.title().trim(), description, t.deadline(), t.priority(), t.category().trim(), t.completed());
    }
}
