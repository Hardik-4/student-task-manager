package ie.griffith.tasks.ui;

import ie.griffith.tasks.dao.DataAccessException;
import ie.griffith.tasks.dao.Database;
import ie.griffith.tasks.dao.JdbcTaskDao;
import ie.griffith.tasks.model.Priority;
import ie.griffith.tasks.model.Task;
import ie.griffith.tasks.service.TaskFilter;
import ie.griffith.tasks.service.TaskService;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Main window: a toolbar for search/filter/sort, a table of tasks, and Add/Edit/Delete buttons.
 * Overdue tasks are shown in red, completed tasks in grey. Double-click a row to edit it.
 */
public class TaskManagerApp extends Application {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE dd MMM yyyy");

    private final TaskService service = new TaskService(new JdbcTaskDao(Database.fromProperties()), Clock.systemDefaultZone());
    private final TableView<Task> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final ComboBox<String> priorityFilter = new ComboBox<>();
    private final ComboBox<TaskFilter.SortBy> sortBy = new ComboBox<>();
    private final CheckBox hideDone = new CheckBox("Hide completed");
    private final Label status = new Label();

    private static final String ALL = "All";

    @Override
    public void start(Stage stage) {
        // ----- toolbar -----
        search.setPromptText("Search title or description...");
        priorityFilter.getItems().add(ALL);
        for (Priority p : Priority.values()) priorityFilter.getItems().add(p.name());
        priorityFilter.setValue(ALL);
        sortBy.getItems().addAll(TaskFilter.SortBy.values());
        sortBy.setValue(TaskFilter.SortBy.DEADLINE);

        // re-run the search whenever any control changes
        search.textProperty().addListener((obs, old, now) -> refresh());
        categoryFilter.setOnAction(e -> refresh());
        priorityFilter.setOnAction(e -> refresh());
        sortBy.setOnAction(e -> refresh());
        hideDone.setOnAction(e -> refresh());

        HBox filters = new HBox(8, search, new Label("Category"), categoryFilter, new Label("Priority"), priorityFilter,
                new Label("Sort"), sortBy, hideDone);
        filters.setPadding(new Insets(10));

        // ----- table -----
        table.getColumns().add(column("Title", 220, Task::title));
        table.getColumns().add(column("Deadline", 140, t -> t.deadline().format(DATE)));
        table.getColumns().add(column("Priority", 80, t -> t.priority().name()));
        table.getColumns().add(column("Category", 110, Task::category));
        table.getColumns().add(column("Status", 90, t -> t.completed() ? "Done"
                : t.isOverdue(service.today()) ? "OVERDUE" : "To do"));
        table.getColumns().add(column("Description", 300, Task::description));
        table.setPlaceholder(new Label("No tasks match."));

        // colour each row: red for overdue, grey for done
        table.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>() {
                @Override
                protected void updateItem(Task task, boolean empty) {
                    super.updateItem(task, empty);
                    if (empty || task == null) setStyle("");
                    else if (task.isOverdue(service.today())) setStyle("-fx-background-color: #ffd6d6;");
                    else if (task.completed()) setStyle("-fx-opacity: 0.55;");
                    else setStyle("");
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) edit(row.getItem());
            });
            return row;
        });

        // ----- buttons -----
        Button add = new Button("Add");
        Button edit = new Button("Edit");
        Button toggle = new Button("Mark done / not done");
        Button delete = new Button("Delete");
        add.setOnAction(e -> add());
        edit.setOnAction(e -> selected().ifPresent(this::edit));
        toggle.setOnAction(e -> selected().ifPresent(t -> run(() -> service.toggleCompleted(t))));
        delete.setOnAction(e -> selected().ifPresent(this::delete));
        HBox buttons = new HBox(8, add, edit, toggle, delete, status);
        buttons.setPadding(new Insets(10));

        BorderPane root = new BorderPane(table, filters, null, buttons, null);
        stage.setTitle("Student Task Manager");
        stage.setScene(new Scene(root, 1050, 600));
        stage.show();
        refresh();
    }

    /** Reloads the table using the current search/filter/sort settings. */
    private void refresh() {
        try {
            reloadCategories();
            String cat = categoryFilter.getValue();
            String pri = priorityFilter.getValue();
            TaskFilter filter = new TaskFilter(search.getText(),
                    cat == null || cat.equals(ALL) ? null : cat,
                    pri == null || pri.equals(ALL) ? null : Priority.valueOf(pri),
                    hideDone.isSelected(), sortBy.getValue());
            table.getItems().setAll(service.search(filter));
            status.setText(table.getItems().size() + " shown, " + service.overdueCount() + " overdue");
        } catch (DataAccessException e) {
            showError("Cannot reach the database. Is MySQL running (docker compose up -d)?\n\n" + e.getMessage());
        }
    }

    private void reloadCategories() {
        String current = categoryFilter.getValue();
        categoryFilter.setOnAction(null); // don't trigger refresh while we change the items
        categoryFilter.getItems().setAll(ALL);
        categoryFilter.getItems().addAll(service.categories());
        categoryFilter.setValue(current != null && categoryFilter.getItems().contains(current) ? current : ALL);
        categoryFilter.setOnAction(e -> refresh());
    }

    private void add() {
        TaskForm.show(null, service.categories()).ifPresent(t -> run(() -> service.add(t)));
    }

    private void edit(Task task) {
        TaskForm.show(task, service.categories()).ifPresent(t -> run(() -> service.update(t)));
    }

    private void delete(Task task) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + task.title() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> run(() -> service.delete(task.id())));
    }

    /** Runs an action, shows any error in a dialog, then refreshes the table. */
    private void run(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | DataAccessException e) {
            showError(e.getMessage());
        }
        refresh();
    }

    private java.util.Optional<Task> selected() {
        Task t = table.getSelectionModel().getSelectedItem();
        if (t == null) status.setText("Select a task first.");
        return java.util.Optional.ofNullable(t);
    }

    private static TableColumn<Task, String> column(String name, double width, Function<Task, String> value) {
        TableColumn<Task, String> col = new TableColumn<>(name);
        col.setPrefWidth(width);
        col.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return col;
    }

    private static void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
