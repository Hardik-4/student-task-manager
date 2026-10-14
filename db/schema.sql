-- Student Task Manager schema
CREATE TABLE IF NOT EXISTS task (
    task_id     INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(100)  NOT NULL,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    deadline    DATE          NOT NULL,
    priority    VARCHAR(6)    NOT NULL CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    category    VARCHAR(50)   NOT NULL,
    completed   BOOLEAN       NOT NULL DEFAULT FALSE
);

-- Sample tasks
INSERT INTO task (title, description, deadline, priority, category, completed) VALUES
  ('Databases CA2', 'Normalise the library schema to 3NF', '2026-11-06', 'HIGH', 'College', FALSE),
  ('OS lab 5', 'Round Robin scheduler in C', '2026-10-30', 'MEDIUM', 'College', FALSE),
  ('Update CV', 'Add StockSmart project', '2026-10-20', 'HIGH', 'Career', FALSE),
  ('Gym', 'Leg day', '2026-10-15', 'LOW', 'Personal', TRUE),
  ('Pay rent', '', '2026-11-01', 'HIGH', 'Personal', FALSE);
