package ie.griffith.tasks;

import ie.griffith.tasks.dao.JdbcTaskDao;
import ie.griffith.tasks.model.Priority;
import ie.griffith.tasks.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the real JDBC code against an H2 in-memory database. */
class JdbcTaskDaoTest {
    private JdbcTaskDao dao;

    @BeforeEach
    void setUp() {
        dao = new JdbcTaskDao(TestDb.create());
    }

    @Test
    void sampleDataIsLoaded() {
        assertEquals(5, dao.findAll().size());
    }

    @Test
    void addAndFind() {
        Task saved = dao.add(new Task(0, "Revise SQL", "joins", LocalDate.of(2026, 12, 1), Priority.HIGH, "College", false));
        assertTrue(saved.id() > 0);
        Task loaded = dao.findById(saved.id()).orElseThrow();
        assertEquals(saved, loaded);
    }

    @Test
    void update() {
        Task t = dao.findAll().get(0);
        Task changed = new Task(t.id(), "New title", "new", t.deadline().plusDays(1), Priority.LOW, "Other", true);
        assertTrue(dao.update(changed));
        assertEquals(changed, dao.findById(t.id()).orElseThrow());
    }

    @Test
    void delete() {
        int id = dao.findAll().get(0).id();
        assertTrue(dao.delete(id));
        assertTrue(dao.findById(id).isEmpty());
        assertFalse(dao.delete(id)); // already gone
    }

    @Test
    void textWithQuotesIsStoredSafely() {
        // a prepared statement treats this as plain text, not SQL
        String tricky = "O'Brien's task'); DROP TABLE task; --";
        Task saved = dao.add(new Task(0, tricky, "", LocalDate.now(), Priority.LOW, "Test", false));
        assertEquals(tricky, dao.findById(saved.id()).orElseThrow().title());
        assertEquals(6, dao.findAll().size());
    }

    @Test
    void findAllIsOrderedByDeadline() {
        var tasks = dao.findAll();
        for (int i = 1; i < tasks.size(); i++) {
            assertFalse(tasks.get(i).deadline().isBefore(tasks.get(i - 1).deadline()));
        }
    }
}
