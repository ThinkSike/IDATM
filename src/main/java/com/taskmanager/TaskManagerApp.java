package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.service.TaskManager;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class TaskManagerApp extends Application {
    private final TaskManager taskManager = new TaskManager();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private ListView<Task> taskListView;
    private TextField titleField, descriptionField, tagField;
    private ComboBox<Integer> priorityComboBox;
    private ComboBox<String> categoryComboBox;
    private DatePicker dueDatePicker;
    private TextField dueTimeField;

    @Override
    public void start(Stage primaryStage) {
        try {
            initializeUI(primaryStage);
        } catch (Exception e) {
            showAlert("Error", "Failed to initialize application: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initializeUI(Stage primaryStage) {
        primaryStage.setTitle("Task Manager");

        // Create the main layout
        BorderPane mainLayout = new BorderPane();
        
        try {
            // Create the input form
            GridPane inputForm = createInputForm();
            mainLayout.setTop(inputForm);

            // Create the task list
            taskListView = new ListView<>();
            taskListView.setCellFactory(listView -> new TaskListCell());
            mainLayout.setCenter(taskListView);

            // Create the control buttons
            HBox buttonBox = createControlButtons();
            mainLayout.setBottom(buttonBox);

            // Set up the scene
            Scene scene = new Scene(mainLayout, 800, 600);
            primaryStage.setScene(scene);
            primaryStage.show();

            // Refresh the task list
            refreshTaskList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize UI components: " + e.getMessage(), e);
        }
    }

    private GridPane createInputForm() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        try {
            // Title
            grid.add(new Label("Title:"), 0, 0);
            titleField = new TextField();
            titleField.setPromptText("Enter task title");
            grid.add(titleField, 1, 0);

            // Description
            grid.add(new Label("Description:"), 0, 1);
            descriptionField = new TextField();
            descriptionField.setPromptText("Enter task description");
            grid.add(descriptionField, 1, 1);

            // Priority
            grid.add(new Label("Priority:"), 0, 2);
            priorityComboBox = new ComboBox<>();
            priorityComboBox.getItems().addAll(1, 2, 3, 4, 5);
            priorityComboBox.setPromptText("Select priority");
            grid.add(priorityComboBox, 1, 2);

            // Due Date
            grid.add(new Label("Due Date:"), 0, 3);
            HBox dateTimeBox = new HBox(10);
            dueDatePicker = new DatePicker();
            dueTimeField = new TextField();
            dueTimeField.setPromptText("HH:mm");
            dateTimeBox.getChildren().addAll(dueDatePicker, dueTimeField);
            grid.add(dateTimeBox, 1, 3);

            // Category
            grid.add(new Label("Category:"), 0, 4);
            categoryComboBox = new ComboBox<>();
            categoryComboBox.getItems().addAll(
                "Work",
                "Personal",
                "Study",
                "Health",
                "Finance",
                "Home",
                "Shopping",
                "Other"
            );
            categoryComboBox.setPromptText("Select category");
            grid.add(categoryComboBox, 1, 4);

            // Tags
            grid.add(new Label("Tags:"), 0, 5);
            tagField = new TextField();
            tagField.setPromptText("Comma-separated tags");
            grid.add(tagField, 1, 5);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create input form: " + e.getMessage(), e);
        }

        return grid;
    }

    private HBox createControlButtons() {
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));

        try {
            Button addButton = new Button("Add Task");
            addButton.setOnAction(event -> addTask());

            Button completeButton = new Button("Complete Selected");
            completeButton.setOnAction(event -> completeSelectedTask());

            Button refreshButton = new Button("Refresh");
            refreshButton.setOnAction(event -> refreshTaskList());

            buttonBox.getChildren().addAll(addButton, completeButton, refreshButton);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create control buttons: " + e.getMessage(), e);
        }

        return buttonBox;
    }

    private void addTask() {
        try {
            validateInputFields();
            Task task = createTaskFromInput();
            taskManager.addTask(task);
            refreshTaskList();
            clearInputFields();
        } catch (IllegalArgumentException e) {
            showAlert("Validation Error", e.getMessage());
        } catch (DateTimeParseException e) {
            showAlert("Date/Time Error", "Invalid date or time format. Please use HH:mm format for time (e.g., 14:30)");
        } catch (Exception e) {
            showAlert("Error", "Failed to add task: " + e.getMessage());
        }
    }

    private void validateInputFields() {
        String title = titleField.getText().trim();
        String category = categoryComboBox.getValue();
        Integer priority = priorityComboBox.getValue();
        
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (category == null || category.isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority is required");
        }
        if (dueDatePicker.getValue() == null) {
            throw new IllegalArgumentException("Due date is required");
        }
        if (dueTimeField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Due time is required (HH:mm format)");
        }
    }

    private Task createTaskFromInput() {
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        Integer priority = priorityComboBox.getValue();
        String category = categoryComboBox.getValue();
        
        // Parse time
        String timeInput = dueTimeField.getText().trim();
        if (!timeInput.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            throw new IllegalArgumentException("Time must be in HH:mm format (e.g., 14:30)");
        }
        
        String[] timeParts = timeInput.split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        
        LocalDateTime dueDate = dueDatePicker.getValue().atTime(hours, minutes);
        
        // Validate due date is not in the past
        if (dueDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }

        Task task = new Task(title, description, category, priority, dueDate);
        
        // Add tags
        String[] tags = tagField.getText().split(",");
        for (String tag : tags) {
            if (!tag.trim().isEmpty()) {
                task.addTag(tag.trim());
            }
        }

        return task;
    }

    private void completeSelectedTask() {
        try {
            Task selectedTask = taskListView.getSelectionModel().getSelectedItem();
            if (selectedTask == null) {
                showAlert("Warning", "Please select a task to complete");
                return;
            }
            taskManager.completeTask(selectedTask.getId());
            refreshTaskList();
        } catch (Exception e) {
            showAlert("Error", "Failed to complete task: " + e.getMessage());
        }
    }

    private void refreshTaskList() {
        try {
            taskListView.getItems().clear();
            List<Task> tasks = taskManager.getTasksByPriority();
            if (tasks.isEmpty()) {
                taskListView.setPlaceholder(new Label("No tasks available"));
            } else {
                taskListView.getItems().addAll(tasks);
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to refresh task list: " + e.getMessage());
        }
    }

    private void clearInputFields() {
        try {
            titleField.clear();
            descriptionField.clear();
            priorityComboBox.setValue(null);
            dueDatePicker.setValue(null);
            dueTimeField.clear();
            categoryComboBox.setValue(null);
            tagField.clear();
        } catch (Exception e) {
            showAlert("Warning", "Failed to clear some input fields: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Failed to show alert: " + e.getMessage());
        }
    }

    private static class TaskListCell extends ListCell<Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            try {
                if (empty || task == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (Priority: %d, Due: %s, Category: %s)",
                        task.getTitle(),
                        task.getPriority(),
                        task.getDueDate().format(formatter),
                        task.getCategory()
                    ));
                }
            } catch (Exception e) {
                setText("Error displaying task");
                System.err.println("Error in TaskListCell: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}