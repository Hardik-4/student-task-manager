package ie.griffith.tasks.ui;

import ie.griffith.tasks.model.Priority;
import ie.griffith.tasks.model.Task;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** A pop-up dialog for adding or editing a task. */
public class TaskForm {

    /**
     * Shows the dialog. Pass an existing task to edit it, or null to add a new one.
     * Returns the filled-in task, or empty if the user pressed Cancel.
     */
    public static Optional<Task> show(Task existing, List<String> categories) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add task" : "Edit task");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField title = new TextField();
        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        DatePicker deadline = new DatePicker(LocalDate.now().plusDays(7));
        ComboBox<Priority> priority = new ComboBox<>();
        priority.getItems().addAll(Priority.values());
        priority.setValue(Priority.MEDIUM);
        ComboBox<String> category = new ComboBox<>();
        category.setEditable(true); // the user can type a new category
        category.getItems().addAll(categories);
        CheckBox done = new CheckBox("Completed");

        if (existing != null) {
            title.setText(existing.title());
            description.setText(existing.description());
            deadline.setValue(existing.deadline());
            priority.setValue(existing.priority());
            category.setValue(existing.category());
            done.setSelected(existing.completed());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0, new Label("Title*"), title);
        grid.addRow(1, new Label("Description"), description);
        grid.addRow(2, new Label("Deadline*"), deadline);
        grid.addRow(3, new Label("Priority*"), priority);
        grid.addRow(4, new Label("Category*"), category);
        grid.addRow(5, new Label(""), done);
        dialog.getDialogPane().setContent(grid);

        // Turn the form fields into a Task when OK is pressed (validation happens in TaskService).
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            int id = existing == null ? 0 : existing.id();
            return new Task(id, title.getText(), description.getText(), deadline.getValue(),
                    priority.getValue(), category.getEditor().getText(), done.isSelected());
        });
        return dialog.showAndWait();
    }
}
