package com.taskmanager;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.taskmanager.service.TaskManager;
import com.taskmanager.model.Task;
import com.taskmanager.view.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MainUI extends Application {
    private TaskManager taskManager;
    private TabPane tabPane;
    private boolean isDarkTheme = false;

    @Override
    public void start(Stage primaryStage) {
        taskManager = new TaskManager();
        
        // Create main layout
        BorderPane root = new BorderPane();
        
        // Create menu bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);
        
        // Create tab pane
        tabPane = new TabPane();
        tabPane.getTabs().addAll(
            createTaskListTab(),
            createCalendarTab(),
            createDashboardTab()
        );
        root.setCenter(tabPane);
        
        // Create task list
        ListView<Task> taskList = new ListView<>();
        taskList.setCellFactory(e -> new TaskListCell());
        
        // Create search panel
        SearchFilterPanel searchPanel = new SearchFilterPanel(taskManager, taskList);
        root.setBottom(searchPanel);
        
        // Set up scene
        Scene scene = new Scene(root, 1200, 800);
        applyTheme(scene);
        
        primaryStage.setTitle("Intelligent Desktop Assistant for Task Management");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().add(exitItem);
        
        // View Menu
        Menu viewMenu = new Menu("View");
        CheckMenuItem darkThemeItem = new CheckMenuItem("Dark Theme");
        darkThemeItem.setOnAction(e -> {
            isDarkTheme = darkThemeItem.isSelected();
            applyTheme(tabPane.getScene());
        });
        viewMenu.getItems().add(darkThemeItem);
        
        menuBar.getMenus().addAll(fileMenu, viewMenu);
        return menuBar;
    }

    private Tab createTaskListTab() {
        Tab tab = new Tab("Tasks");
        tab.setClosable(false);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Add task button
        Button addTaskBtn = new Button("Add Task");
        addTaskBtn.setOnAction(e -> showAddTaskDialog());
        
        // Task list
        ListView<Task> taskList = new ListView<>();
        taskList.setCellFactory(e -> new TaskListCell());
        
        content.getChildren().addAll(addTaskBtn, taskList);
        tab.setContent(content);
        
        return tab;
    }

    private Tab createCalendarTab() {
        Tab tab = new Tab("Calendar");
        tab.setClosable(false);
        tab.setContent(new CalendarTab(taskManager));
        return tab;
    }

    private Tab createDashboardTab() {
        Tab tab = new Tab("Dashboard");
        tab.setClosable(false);
        tab.setContent(new DashboardView(taskManager));
        return tab;
    }

    private void showAddTaskDialog() {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Add New Task");
        dialog.setHeaderText("Enter Task Details");

        // Create the custom dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField();
        TextField descriptionField = new TextField();
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Work", "Personal", "Study", "Health", "Finance", "Home", "Shopping", "Other");
        ComboBox<Integer> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(1, 2, 3, 4, 5);
        DatePicker dueDatePicker = new DatePicker();
        TextField dueTimeField = new TextField();
        dueTimeField.setPromptText("HH:mm");

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Priority:"), 0, 3);
        grid.add(priorityCombo, 1, 3);
        grid.add(new Label("Due Date:"), 0, 4);
        grid.add(dueDatePicker, 1, 4);
        grid.add(new Label("Due Time:"), 0, 5);
        grid.add(dueTimeField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String title = titleField.getText();
                    String description = descriptionField.getText();
                    String category = categoryCombo.getValue();
                    Integer priority = priorityCombo.getValue();
                    LocalDate dueDate = dueDatePicker.getValue();
                    String timeStr = dueTimeField.getText();
                    
                    if (title.isEmpty() || category == null || priority == null || dueDate == null || timeStr.isEmpty()) {
                        showAlert("Error", "All fields are required");
                        return null;
                    }

                    String[] timeParts = timeStr.split(":");
                    LocalDateTime dueDateTime = dueDate.atTime(
                        Integer.parseInt(timeParts[0]),
                        Integer.parseInt(timeParts[1])
                    );

                    Task task = new Task(title, description, category, priority, dueDateTime);
                    taskManager.addTask(task);
                    return task;
                } catch (Exception e) {
                    showAlert("Error", "Invalid input: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void applyTheme(Scene scene) {
        String themeFile = isDarkTheme ? "/styles/dark-theme.css" : "/styles/light-theme.css";
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource(themeFile).toExternalForm());
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 