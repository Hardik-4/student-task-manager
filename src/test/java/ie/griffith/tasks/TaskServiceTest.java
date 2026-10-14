package ie.griffith.tasks;

import ie.griffith.tasks.dao.JdbcTaskDao;
import ie.griffith.tasks.model.Priority;
import ie.griffith.tasks.model.Task;
import ie.griffith.tasks.service.TaskFilter;
import ie.griffith.tasks.service.TaskFilter.SortBy;
import ie.griffith.tasks.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {
    // Pretend today is 25 Oct 2026. Sample deadlines: CV 20 Oct, Gym 15 Oct (done), OS lab 30 Oct,
    // rent 1 Nov, Databases 6 Nov.
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);
    private TaskService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new TaskService(new JdbcTaskDao(TestDb.create()), fixed);
    }

    private static List<String> titles(List<Task> tasks) {
        return tasks.stream().map(Task::title).toList();
    }

    private TaskFilter sortedBy(SortBy s) {
        return new TaskFilter(null, null, null, false, s);
    }

    @Test
    void overdueMeansPastDeadlineAndNotDone() {
        assertEquals(1, service.overdueCount()); // Update CV; Gym is past but completed
        Task cv = service.search(new TaskFilter("cv", null, null, false, SortBy.DEADLINE)).get(0);
        assertTrue(cv.isOverdue(TODAY));
        assertFalse(cv.isOverdue(LocalDate.of(2026, 10, 20))); // due today is not overdue yet
    }

    @Test
    void searchMatchesTitleOrDescriptionIgnoringCase() {
        assertEquals(List.of("Databases CA2"), titles(service.search(new TaskFilter("NORMALISE", null, null, false, SortBy.DEADLINE))));
        assertEquals(List.of("OS lab 5"), titles(service.search(new TaskFilter("os lab", null, null, false, SortBy.DEADLINE))));
        assertTrue(service.search(new TaskFilter("nothing like this", null, null, false, SortBy.DEADLINE)).isEmpty());
    }

    @Test
    void filtersCombine() {
        TaskFilter f = new TaskFilter(null, "personal", Priority.HIGH, false, SortBy.DEADLINE);
        assertEquals(List.of("Pay rent"), titles(service.search(f)));
        TaskFilter hide = new TaskFilter(null, "Personal", null, true, SortBy.DEADLINE);
        assertEquals(List.of("Pay rent"), titles(service.search(hide))); // Gym is done, so hidden
    }

    @Test
    void sorting() {
        assertEquals(List.of("Gym", "Update CV", "OS lab 5", "Pay rent", "Databases CA2"),
                titles(service.search(sortedBy(SortBy.DEADLINE))));
        assertEquals(List.of("Update CV", "Pay rent", "Databases CA2", "OS lab 5", "Gym"),
                titles(service.search(sortedBy(SortBy.PRIORITY))));   // HIGH first, then by deadline
        assertEquals(List.of("Databases CA2", "Gym", "OS lab 5", "Pay rent", "Update CV"),
                titles(service.search(sortedBy(SortBy.TITLE))));
        assertEquals("Career", service.search(sortedBy(SortBy.CATEGORY)).get(0).category());
    }

    @Test
    void addEditDelete() {
        Task t = service.add(new Task(0, "  Write README  ", null, TODAY.plusDays(2), Priority.MEDIUM, " College ", false));
        assertEquals("Write README", t.title());   // text is trimmed
        assertEquals("", t.description());
        assertEquals("College", t.category());

        service.update(new Task(t.id(), "Write README v2", "", t.deadline(), Priority.HIGH, "College", false));
        service.toggleCompleted(service.search(new TaskFilter("v2", null, null, false, SortBy.DEADLINE)).get(0));
        Task updated = service.search(new TaskFilter("v2", null, null, false, SortBy.DEADLINE)).get(0);
        assertTrue(updated.completed());
        assertEquals(Priority.HIGH, updated.priority());

        service.delete(t.id());
        assertThrows(IllegalArgumentException.class, () -> service.delete(t.id()));
    }

    @Test
    void validation() {
        assertThrows(IllegalArgumentException.class,
                () -> service.add(new Task(0, " ", "", TODAY, Priority.LOW, "X", false)));
        assertThrows(IllegalArgumentException.class,
                () -> service.add(new Task(0, "x".repeat(101), "", TODAY, Priority.LOW, "X", false)));
        assertThrows(IllegalArgumentException.class,
                () -> service.add(new Task(0, "T", "", null, Priority.LOW, "X", false)));
        assertThrows(IllegalArgumentException.class,
                () -> service.add(new Task(0, "T", "", TODAY, null, "X", false)));
        assertThrows(IllegalArgumentException.class,
                () -> service.add(new Task(0, "T", "", TODAY, Priority.LOW, "", false)));
    }

    @Test
    void categoriesAreDistinctAndSorted() {
        assertEquals(List.of("Career", "College", "Personal"), service.categories());
    }
}
