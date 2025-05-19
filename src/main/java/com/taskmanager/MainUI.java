package com.taskmanager;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import com.taskmanager.service.TaskManager;
import com.taskmanager.model.Task;
import com.taskmanager.view.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.DoubleProperty;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.scene.effect.DropShadow;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * Main application UI class.
 * 
 * @SuppressWarnings("unchecked") is used to suppress warnings related to JavaFX collection operations
 * which are type-safe at runtime due to JavaFX's internal checks.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MainUI extends Application {
    private TaskManager taskManager;
    private TabPane tabPane;
    private boolean isDarkTheme = false;
    private ObservableList<Task> taskData;
    private ListView<Task> taskListView;
    private static final String SUPPRESSION_FILE = System.getProperty("user.home") + File.separator + ".suppressed_reminders.txt";
    private Set<String> suppressedReminders = new HashSet<>(); // Format: taskId:yyyy-MM-dd

    @Override
    public void start(Stage primaryStage) {
        taskManager = new TaskManager();
        loadSuppressedReminders();
        
        // Create main layout
        BorderPane root = new BorderPane();
        
        // Create menu bar with theme toggle
        Node topContainer = createMenuBar();
        root.setTop(topContainer);
        
        // Initialize taskData and taskTable BEFORE creating tabs
        taskData = FXCollections.observableArrayList(taskManager.getAllTasks());
        taskListView = new ListView<>(taskData);
        taskListView.setCellFactory(list -> new TaskCardCell());
        taskListView.setFixedCellSize(-1); // Enable pixel-based smooth scrolling
        taskListView.setCache(true);
        taskListView.setCacheHint(javafx.scene.CacheHint.SPEED);
        
        // Create tab pane
        tabPane = new TabPane();
        tabPane.getTabs().addAll(
            createTaskListTab(),
            createCalendarTab(),
            createDashboardTab()
        );
        
        // Make tabs responsive to different screen resolutions
        tabPane.setTabMinWidth(80); // Set minimum tab width to ensure visibility on smaller screens
        tabPane.setTabMaxWidth(150); // Limit maximum width for consistency
        
        // Enable TabPane to properly resize with window
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.prefHeightProperty().bind(primaryStage.heightProperty());
        tabPane.prefWidthProperty().bind(primaryStage.widthProperty());
        tabPane.setMinSize(300, 200); // Set reasonable minimum size
        
        // Adjust tab sizes based on scene width
        root.widthProperty().addListener((obs, oldVal, newVal) -> {
            adjustTabSizes(newVal.doubleValue());
        });
        
        // Use ScrollPane as wrapper for TabPane to ensure content is always accessible
        ScrollPane scrollWrapper = new ScrollPane(tabPane);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setFitToHeight(true);
        scrollWrapper.setPannable(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollWrapper.getStyleClass().addAll("main-scroll-pane", "invisible-scrollbar");
        
        // Ensure scrolling behavior is smooth
        scrollWrapper.setVvalue(0);
        scrollWrapper.setHvalue(0);
        scrollWrapper.setPadding(new Insets(0));
        
        root.setCenter(scrollWrapper);
        
        // Set up scene
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/responsive.css").toExternalForm());
        applyTheme(scene);
        
        // Set minimum window size to prevent unusable UI
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(300);
        
        primaryStage.setTitle("Intelligent Desktop Assistant for Task Management");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Add window resize listener
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            adjustTabSizes(newVal.doubleValue());
        });
        
        // Initial adjustment based on starting size
        adjustTabSizes(scene.getWidth());

        checkAndShowReminders();
    }

    private Node createMenuBar() {
        // Create the main top container
        BorderPane topContainer = new BorderPane();
        
        // Create the menu bar
        MenuBar menuBar = new MenuBar();
        
        // File Menu
        Menu fileMenu = new Menu("File");
        
        MenuItem exportItem = new MenuItem("Export Tasks to JSON");
        exportItem.setOnAction(e -> exportTasksToJson());
        
        MenuItem importItem = new MenuItem("Import Tasks from JSON");
        importItem.setOnAction(e -> importTasksFromJson());
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        
        fileMenu.getItems().addAll(exportItem, importItem, new SeparatorMenuItem(), exitItem);
        
        // Empty View Menu
        Menu viewMenu = new Menu("View");
        
        menuBar.getMenus().addAll(fileMenu, viewMenu);
        
        // Set menu bar to the left side of the top container
        topContainer.setLeft(menuBar);
        
        // Add a theme toggle button in the right corner
        Button themeToggle = new Button();
        themeToggle.getStyleClass().add("theme-toggle");
        themeToggle.setPrefSize(120, 30); // Make button larger
        
        // Set initial icon and tooltip based on current theme
        updateThemeToggle(themeToggle);
        
        themeToggle.setOnAction(e -> {
            // Toggle theme state
            isDarkTheme = !isDarkTheme;
            
            // Update button appearance
            updateThemeToggle(themeToggle);
            
            // Apply theme changes
            applyTheme(tabPane.getScene());
            
            // Refresh the ListView to update card styles
            if (taskListView != null) {
                taskListView.refresh();
            }
        });
        
        // Create a right-aligned container for the theme toggle
        HBox rightContainer = new HBox(themeToggle);
        rightContainer.setAlignment(Pos.CENTER_RIGHT);
        rightContainer.setPadding(new Insets(0, 10, 0, 0));
        
        // Set the theme toggle container to the right side
        topContainer.setRight(rightContainer);
        
        return topContainer;
    }

    // Helper method to update theme toggle appearance
    private void updateThemeToggle(Button themeToggle) {
        if (isDarkTheme) {
            // Currently in dark theme - show moon icon
            themeToggle.setText("DARK MODE");
            themeToggle.setGraphic(new Label("🌙"));
            themeToggle.setTooltip(new Tooltip("Currently using Dark theme. Click to switch to Light theme."));
        } else {
            // Currently in light theme - show sun icon
            themeToggle.setText("LIGHT MODE");
            themeToggle.setGraphic(new Label("☀️"));
            themeToggle.setTooltip(new Tooltip("Currently using Light theme. Click to switch to Dark theme."));
        }
    }

    private Tab createTaskListTab() {
        Tab tab = new Tab("Tasks");
        tab.setClosable(false);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getStyleClass().add("table-card");
        
        // Add task and filter buttons in a header bar
        Button addTaskBtn = new Button("Add Task");
        addTaskBtn.setOnAction(e -> showAddTaskDialog());
        
        // Create filter button with menu
        MenuButton filterBtn = new MenuButton("Filter Tasks");
        filterBtn.getStyleClass().addAll("button", "filter-button");
        filterBtn.setPrefHeight(addTaskBtn.getHeight());
        
        // Reset filter function
        Runnable resetFilters = () -> taskData.setAll(taskManager.getAllTasks());
        
        // Show All item
        MenuItem showAllItem = new MenuItem("Show All");
        showAllItem.setOnAction(e -> resetFilters.run());
        
        // Avoid using nested submenus - use separator and headers instead
        SeparatorMenuItem statusSeparator = new SeparatorMenuItem();
        Label statusLabel = new Label("STATUS");
        statusLabel.getStyleClass().add("menu-header");
        CustomMenuItem statusHeader = new CustomMenuItem(statusLabel);
        statusHeader.setHideOnClick(false);
        
        MenuItem showCompletedItem = new MenuItem("✓ Completed Tasks");
        showCompletedItem.setOnAction(e -> {
            taskData.setAll(taskManager.getAllTasks().stream()
                    .filter(Task::isCompleted)
                    .collect(java.util.stream.Collectors.<Task>toList()));
        });
        
        MenuItem showPendingItem = new MenuItem("○ Uncompleted Tasks");
        showPendingItem.setOnAction(e -> {
            taskData.setAll(taskManager.getAllTasks().stream()
                    .filter(task -> !task.isCompleted())
                    .collect(java.util.stream.Collectors.<Task>toList()));
        });
        
        MenuItem showOverdueItem = new MenuItem("⏰ Overdue Tasks");
        showOverdueItem.setOnAction(e -> {
            LocalDateTime now = LocalDateTime.now();
            taskData.setAll(taskManager.getAllTasks().stream()
                    .filter(task -> !task.isCompleted() && task.getDueDate().isBefore(now))
                    .collect(java.util.stream.Collectors.<Task>toList()));
        });
        
        // Priority section - with header instead of submenu
        SeparatorMenuItem prioritySeparator = new SeparatorMenuItem();
        Label priorityLabel = new Label("PRIORITY");
        priorityLabel.getStyleClass().add("menu-header");
        CustomMenuItem priorityHeader = new CustomMenuItem(priorityLabel);
        priorityHeader.setHideOnClick(false);
        
        MenuItem priority1Item = new MenuItem("⚪ Priority 1 (Highest)");
        priority1Item.setOnAction(e -> {
            taskData.setAll(taskManager.getAllTasks().stream()
                    .filter(task -> task.getPriority() == 1)
                    .collect(java.util.stream.Collectors.<Task>toList()));
        });
        
        MenuItem priority2Item = new MenuItem("⚪ Priority 2");
        priority2Item.setOnAction(e -> {
            taskData.setAll(taskManager.getAllTasks().stream()
                    .filter(task -> task.getPriority() == 2)
                    .collect(java.util.stream.Collectors.<Task>toList()));
        });
        
        MenuItem priority3Item = new MenuItem("⚪ Priority 3");
        priority3Item.setOnAction(e -> {
            taskData.setAll(taskManager.getAllTasks().stream()
                    .filter(task -> task.getPriority() == 3)
                    .collect(java.util.stream.Collectors.<Task>toList()));
        });
        
        MenuItem priority4Item = new MenuItem("⚪ Priority 4");
        priority4Item.setOnAction(e -> {
            taskData.setAll(taskManager.getAllTasks().stream()
                    .filter(task -> task.getPriority() == 4)
                    .collect(java.util.stream.Collectors.<Task>toList()));
        });
        
        MenuItem priority5Item = new MenuItem("⚪ Priority 5");
        priority5Item.setOnAction(e -> {
            taskData.setAll(taskManager.getAllTasks().stream()
                    .filter(task -> task.getPriority() == 5)
                    .collect(java.util.stream.Collectors.<Task>toList()));
        });
        
        // Category section - with header instead of submenu
        SeparatorMenuItem categorySeparator = new SeparatorMenuItem();
        Label categoryLabel = new Label("CATEGORY");
        categoryLabel.getStyleClass().add("menu-header");
        CustomMenuItem categoryHeader = new CustomMenuItem(categoryLabel);
        categoryHeader.setHideOnClick(false);
        
        // Initial list for categories
        List<MenuItem> categoryItems = new ArrayList<>();
        
        // Function to refresh category filters
        Runnable refreshCategoryMenu = () -> {
            // Remove existing category items
            filterBtn.getItems().removeAll(categoryItems);
            categoryItems.clear();
            
            // Get all unique categories
            Set<String> categories = taskManager.getAllTasks().stream()
                .map(Task::getCategory)
                .collect(java.util.stream.Collectors.toSet());
            
            // Add menu item for each category
            for (String category : categories) {
                if (category != null && !category.isEmpty()) {
                    MenuItem categoryItem = new MenuItem("📂 " + category);
                    categoryItem.setOnAction(e -> {
                        String selectedCategory = category; // Use captured category
                        taskData.setAll(taskManager.getAllTasks().stream()
                            .filter(task -> selectedCategory.equals(task.getCategory()))
                            .collect(java.util.stream.Collectors.<Task>toList()));
                    });
                    categoryItems.add(categoryItem);
                }
            }
            
            // If there are no categories
            if (categoryItems.isEmpty()) {
                MenuItem noCategories = new MenuItem("No Categories Available");
                noCategories.setDisable(true);
                categoryItems.add(noCategories);
            }
            
            // Add all category items after the category header
            int headerIndex = filterBtn.getItems().indexOf(categoryHeader);
            if (headerIndex >= 0) {
                filterBtn.getItems().addAll(headerIndex + 1, categoryItems);
            }
        };
        
        // Add all the items to the menu button in a flat structure (no nested menus)
        filterBtn.getItems().addAll(
            showAllItem, 
            statusSeparator, statusHeader, 
            showCompletedItem, showPendingItem, showOverdueItem,
            prioritySeparator, priorityHeader,
            priority1Item, priority2Item, priority3Item, priority4Item, priority5Item,
            categorySeparator, categoryHeader
        );
        
        // Initial population of categories
        refreshCategoryMenu.run();
        
        // Create top bar with buttons
        HBox headerBar = new HBox(10);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        headerBar.getChildren().addAll(addTaskBtn, spacer, filterBtn);
        
        // Refresh category menu whenever tasks change
        taskData.addListener((javafx.collections.ListChangeListener<Task>) c -> {
            refreshCategoryMenu.run();
        });
        
        // ===== IMPROVED SCROLLING EXPERIENCE =====
        // Enable smooth pixel-based scrolling
        taskListView.setFixedCellSize(-1);
        
        // Add CSS class for smooth scrolling
        taskListView.getStyleClass().add("smooth-scroll-list");
        
        // Create a ScrollPane wrapper with better scroll characteristics
        ScrollPane scrollPane = new ScrollPane(taskListView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true); 
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("elegant-scroll");
        
        // Remove ListView border so it blends with ScrollPane
        taskListView.setStyle("-fx-background-insets: 0; -fx-padding: 0;");
        
        // Custom scroll tracking for smoother animations
        DoubleProperty scrollPosition = new SimpleDoubleProperty(0);
        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            scrollPosition.set(newVal.doubleValue());
            // Only animate when scrolling is slow enough to avoid lagging
            if (Math.abs(oldVal.doubleValue() - newVal.doubleValue()) < 0.05) {
                animateVisibleCards(scrollPosition.get());
            }
        });
        
        content.getChildren().addAll(headerBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        tab.setContent(content);
        
        // Add animation when the tab is selected
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                animateTaskCards();
            }
        });
        return tab;
    }

    private Tab createCalendarTab() {
        Tab tab = new Tab("Calendar");
        tab.setClosable(false);
        tab.setContent(new MonthViewCalendarTab(taskManager));
        return tab;
    }

    private Tab createDashboardTab() {
        Tab tab = new Tab("Dashboard");
        tab.setClosable(false);
        tab.setContent(new DashboardView(taskManager));
        return tab;
    }

    private void showAddTaskDialog() {
        // Store reference to the current stage and its state
        Stage mainStage = (Stage) tabPane.getScene().getWindow();
        boolean wasMaximized = mainStage.isMaximized();
        double width = mainStage.getWidth();
        double height = mainStage.getHeight();
        double x = mainStage.getX();
        double y = mainStage.getY();
        
        // Create a custom dialog window instead of using Dialog
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);
        dialog.setTitle("Add New Task");
        dialog.getIcons().addAll(mainStage.getIcons());
        
        // Set dialog size
        dialog.setWidth(600);
        dialog.setHeight(450);
        
        // Center on parent
        dialog.setX(x + (width - 600) / 2);
        dialog.setY(y + (height - 450) / 2);
        
        // Create the content grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(30, 30, 30, 30));

        Label headerLabel = new Label("Enter Task Details");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        TextField titleField = new TextField();
        TextField descriptionField = new TextField();
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Work", "Personal", "Study", "Health", "Finance", "Home", "Shopping", "Other");
        ComboBox<Integer> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(1, 2, 3, 4, 5);
        DatePicker dueDatePicker = new DatePicker();
        TextField dueTimeField = new TextField();
        dueTimeField.setPromptText("HH:mm");

        grid.add(headerLabel, 0, 0, 2, 1);
        GridPane.setMargin(headerLabel, new Insets(0, 0, 20, 0));
        
        grid.add(new Label("Title:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descriptionField, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        grid.add(new Label("Priority:"), 0, 4);
        grid.add(priorityCombo, 1, 4);
        grid.add(new Label("Due Date:"), 0, 5);
        grid.add(dueDatePicker, 1, 5);
        grid.add(new Label("Due Time:"), 0, 6);
        grid.add(dueTimeField, 1, 6);
        
        // Add buttons in an HBox
        Button addButton = new Button("Add");
        Button cancelButton = new Button("Cancel");
        
        HBox buttons = new HBox(10, addButton, cancelButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttons, 0, 7, 2, 1);
        GridPane.setMargin(buttons, new Insets(20, 0, 0, 0));
        
        // Set column constraints
        ColumnConstraints column1 = new ColumnConstraints(100, 100, 100);
        ColumnConstraints column2 = new ColumnConstraints(400, 400, 400);
        grid.getColumnConstraints().addAll(column1, column2);
        
        // Set up button actions
        addButton.setOnAction(e -> {
            try {
                validateTaskInput(titleField.getText(), categoryCombo.getValue(), 
                               priorityCombo.getValue(), dueDatePicker.getValue(), 
                               dueTimeField.getText());
                
                String title = titleField.getText().trim();
                String description = descriptionField.getText().trim();
                String category = categoryCombo.getValue();
                Integer priority = priorityCombo.getValue();
                LocalDate dueDate = dueDatePicker.getValue();
                String timeStr = dueTimeField.getText().trim();
                
                String[] timeParts = timeStr.split(":");
                LocalDateTime dueDateTime = dueDate.atTime(
                    Integer.parseInt(timeParts[0]),
                    Integer.parseInt(timeParts[1])
                );

                // Validate due date is not in the past
                if (dueDateTime.isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException("Due date cannot be in the past");
                }

                Task task = new Task(title, description, category, priority, dueDateTime);
                taskManager.addTask(task);
                taskData.setAll(taskManager.getAllTasks());
                
                dialog.close();
            } catch (IllegalArgumentException ex) {
                showAlert("Validation Error", ex.getMessage());
            } catch (DateTimeParseException ex) {
                showAlert("Date/Time Error", "Invalid date or time format. Please use HH:mm format for time (e.g., 14:30)");
            } catch (Exception ex) {
                showAlert("Error", "Failed to add task: " + ex.getMessage());
            }
        });
        
        cancelButton.setOnAction(e -> dialog.close());

        // Set up the scene with the grid
        Scene scene = new Scene(grid);
        
        // Apply current theme to dialog
        applyTheme(scene);
        
        dialog.setScene(scene);
        
        // Ensure dialog doesn't resize when switching themes
        dialog.setResizable(false);
        
        // Add an event handler to restore maximized state when dialog closes
        dialog.setOnHidden(e -> {
            if (wasMaximized) {
                // Use Platform.runLater to ensure this happens after all other events
                javafx.application.Platform.runLater(() -> {
                    mainStage.setMaximized(true);
                });
            }
        });
        
        // Show dialog and wait for it to close
        dialog.showAndWait();
    }

    private void showEditTaskDialog() {
        Task selected = taskListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Store reference to the current stage and its state
        Stage mainStage = (Stage) tabPane.getScene().getWindow();
        boolean wasMaximized = mainStage.isMaximized();

        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");
        dialog.setHeaderText("Edit Task Details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(selected.getTitle());
        TextField descriptionField = new TextField(selected.getDescription());
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Work", "Personal", "Study", "Health", "Finance", "Home", "Shopping", "Other");
        categoryCombo.setValue(selected.getCategory());
        ComboBox<Integer> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(1, 2, 3, 4, 5);
        priorityCombo.setValue(selected.getPriority());
        DatePicker dueDatePicker = new DatePicker(selected.getDueDate().toLocalDate());
        TextField dueTimeField = new TextField(selected.getDueDate().toLocalTime().toString());
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

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Add an event handler to restore maximized state when dialog closes
        dialog.setOnCloseRequest(e -> {
            if (wasMaximized) {
                // Use Platform.runLater to ensure this happens after all other events
                javafx.application.Platform.runLater(() -> {
                    mainStage.setMaximized(true);
                });
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    validateTaskInput(titleField.getText(), categoryCombo.getValue(), 
                                   priorityCombo.getValue(), dueDatePicker.getValue(), 
                                   dueTimeField.getText());

                    String title = titleField.getText().trim();
                    String description = descriptionField.getText().trim();
                    String category = categoryCombo.getValue();
                    Integer priority = priorityCombo.getValue();
                    LocalDate dueDate = dueDatePicker.getValue();
                    String timeStr = dueTimeField.getText().trim();

                    String[] timeParts = timeStr.split(":");
                    LocalDateTime dueDateTime = dueDate.atTime(
                        Integer.parseInt(timeParts[0]),
                        Integer.parseInt(timeParts[1])
                    );

                    // Validate due date is not in the past
                    if (dueDateTime.isBefore(LocalDateTime.now())) {
                        throw new IllegalArgumentException("Due date cannot be in the past");
                    }

                    selected.setTitle(title);
                    selected.setDescription(description);
                    selected.setCategory(category);
                    selected.setPriority(priority);
                    selected.setDueDate(dueDateTime);
                    taskManager.updateTask(selected);
                    taskData.setAll(taskManager.getAllTasks());
                    return selected;
                } catch (IllegalArgumentException e) {
                    showAlert("Validation Error", e.getMessage());
                    return null;
                } catch (DateTimeParseException e) {
                    showAlert("Date/Time Error", "Invalid date or time format. Please use HH:mm format for time (e.g., 14:30)");
                    return null;
                } catch (Exception e) {
                    showAlert("Error", "Failed to edit task: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteSelectedTask() {
        Task selected = taskListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        taskManager.deleteTask(selected.getId());
        taskData.setAll(taskManager.getAllTasks());
    }

    private void validateTaskInput(String title, String category, Integer priority, 
                                 LocalDate dueDate, String timeStr) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (category == null || category.isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority is required");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date is required");
        }
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Due time is required (HH:mm format)");
        }
        if (!timeStr.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            throw new IllegalArgumentException("Time must be in HH:mm format (e.g., 14:30)");
        }
    }

    private void applyTheme(Scene scene) {
        String cssFile = isDarkTheme ? "/styles/dark-theme.css" : "/styles/light-theme.css";
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
        // Refresh the ListView to update card styles
        if (taskListView != null) {
            taskListView.refresh();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void playTingSound() {
        try {
            String soundPath = getClass().getResource("/ting.mp3").toExternalForm();
            Media sound = new Media(soundPath);
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();
        } catch (Exception e) {
            // Ignore if sound fails
        }
    }

    private void checkAndShowReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        for (Task task : taskManager.getAllTasks()) {
            if (task.isCompleted()) continue;
            LocalDateTime due = task.getDueDate();
            long taskId = task.getId();
            String suppressionKey = taskId + ":" + today.toString();
            // First reminder: 1 day before
            if (!suppressedReminders.contains(suppressionKey)) {
                if (due.toLocalDate().minusDays(1).equals(today)) {
                    showReminderDialog(task, "Task due tomorrow!", taskId, today);
                }
                // Second reminder: 1 hour before
                if (due.minusHours(1).isBefore(now) && due.isAfter(now)) {
                    showReminderDialog(task, "Task due in 1 hour!", taskId, today);
                }
            }
        }
    }

    private void showReminderDialog(Task task, String message, long taskId, LocalDate today) {
        Stage mainStage = (Stage) tabPane.getScene().getWindow();
        boolean wasMaximized = mainStage.isMaximized();
        
        playTingSound();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Task Reminder");
        alert.setHeaderText(message);
        alert.setContentText("Title: " + task.getTitle() + "\nDue: " + task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
        ButtonType suppressBtn = new ButtonType("Don't remind me again today");
        alert.getButtonTypes().add(suppressBtn);
        
        // Add event handler to restore maximized state when dialog closes
        alert.setOnHidden(e -> {
            if (wasMaximized) {
                // Use Platform.runLater to ensure this happens after all other events
                javafx.application.Platform.runLater(() -> {
                    mainStage.setMaximized(true);
                });
            }
        });
        
        alert.showAndWait().ifPresent(result -> {
            if (result == suppressBtn) {
                suppressedReminders.add(taskId + ":" + today.toString());
                saveSuppressedReminders();
            }
        });
    }

    private void loadSuppressedReminders() {
        suppressedReminders.clear();
        File file = new File(SUPPRESSION_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    suppressedReminders.add(line.trim());
                }
            } catch (IOException e) {
                // Ignore, just start with empty set
            }
        }
    }

    private void saveSuppressedReminders() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SUPPRESSION_FILE))) {
            for (String key : suppressedReminders) {
                writer.write(key);
                writer.newLine();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    // Card-style cell for each task
    private class TaskCardCell extends ListCell<Task> {
        private HBox card;
        private Circle priorityCircle;
        private VBox textBox;
        private Label titleLabel;
        private Label descLabel;
        private HBox metaBox;
        private Label catLabel;
        private Label dueLabel;
        private CheckBox completedBox;
        private boolean firstShow = true;

        public TaskCardCell() {
            card = new HBox(16);
            card.getStyleClass().add("task-card");
            card.setPadding(new Insets(20));
            card.setSpacing(16);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setCache(true);
            card.setCacheHint(javafx.scene.CacheHint.SPEED);

            priorityCircle = new Circle(10);
            priorityCircle.getStyleClass().add("task-priority");

            HBox.setMargin(priorityCircle, new Insets(0, 0, 0, 0));  // Consistent margin

            textBox = new VBox(8);
            textBox.setAlignment(Pos.CENTER_LEFT);
            textBox.setPrefWidth(500); // Using preferred width for consistent sizing
            textBox.setMinWidth(500); 
            textBox.setMaxWidth(500);
            
            titleLabel = new Label();
            titleLabel.getStyleClass().add("task-title");
            descLabel = new Label();
            descLabel.getStyleClass().add("task-description");
            descLabel.setWrapText(true);
            metaBox = new HBox(16);
            metaBox.setAlignment(Pos.CENTER_LEFT);
            catLabel = new Label();
            catLabel.getStyleClass().add("task-meta");
            dueLabel = new Label();
            dueLabel.getStyleClass().add("task-meta");
            metaBox.getChildren().addAll(catLabel, dueLabel);
            textBox.getChildren().addAll(titleLabel, descLabel, metaBox);

            // Make checkbox width consistent
            completedBox = new CheckBox();
            completedBox.getStyleClass().add("task-checkbox");
            completedBox.setPrefWidth(140); // Fixed width for all checkboxes
            completedBox.setMinWidth(140);
            completedBox.setMaxWidth(140);
            completedBox.setAlignment(Pos.CENTER_LEFT); // Align text to left within the checkbox
            completedBox.setOnAction(e -> {
                Task task = getItem();
                if (task != null) {
                    task.setCompleted(completedBox.isSelected());
                    completedBox.setText(task.isCompleted() ? "Completed" : "Not Completed");
                    taskManager.updateTask(task);
                    taskData.setAll(taskManager.getAllTasks());
                }
            });

            // Add specific margin to ensure perfect alignment
            HBox.setMargin(completedBox, new Insets(0, 0, 0, 0));

            card.getChildren().addAll(priorityCircle, textBox, completedBox);
            card.setCache(true);
            card.setCacheHint(javafx.scene.CacheHint.SPEED);
        }

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setGraphic(null);
                setContextMenu(null);
                firstShow = true;
            } else {
                // Update content only
                priorityCircle.getStyleClass().removeIf(s -> s.startsWith("task-priority-"));
                priorityCircle.getStyleClass().add("task-priority-" + task.getPriority());
                
                // Add programmatic glow effect to match CSS
                DropShadow glow = new DropShadow();
                glow.setRadius(10);
                glow.setSpread(0.4);
                
                // Set color based on priority
                switch (task.getPriority()) {
                    case 1: glow.setColor(Color.web("#FF1744")); break;
                    case 2: glow.setColor(Color.web("#FF9100")); break;
                    case 3: glow.setColor(Color.web("#FFEA00")); break;
                    case 4: glow.setColor(Color.web("#00E676")); break;
                    case 5: glow.setColor(Color.web("#00B0FF")); break;
                    default: glow.setColor(Color.web("#2196F3")); break;
                }
                
                priorityCircle.setEffect(glow);
                
                titleLabel.setText(task.getTitle());
                descLabel.setText(task.getDescription());
                catLabel.setText("Category: " + task.getCategory());
                dueLabel.setText("Due: " + task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
                completedBox.setSelected(task.isCompleted());
                completedBox.setText(task.isCompleted() ? "Completed" : "Not Completed");

                // Context menu
                ContextMenu contextMenu = new ContextMenu();
                MenuItem editItem = new MenuItem("Edit Task");
                editItem.setOnAction(e -> {
                    getListView().getSelectionModel().select(getItem());
                    showEditTaskDialog();
                });
                MenuItem deleteItem = new MenuItem("Delete Task");
                deleteItem.setOnAction(e -> {
                    Task selected = getItem();
                    if (selected != null) {
                        // Get the main window and store its maximized state
                        Stage mainStage = (Stage) getListView().getScene().getWindow();
                        boolean wasMaximized = mainStage.isMaximized();
                        
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Delete Task");
                        confirm.setHeaderText(null);
                        confirm.setContentText("Are you sure you want to delete the task: '" + selected.getTitle() + "'?");
                        
                        // Add event handler to restore maximized state when dialog closes
                        confirm.setOnHidden(event -> {
                            if (wasMaximized) {
                                // Use Platform.runLater to ensure this happens after all other events
                                javafx.application.Platform.runLater(() -> {
                                    mainStage.setMaximized(true);
                                });
                            }
                        });
                        
                        confirm.showAndWait().ifPresent(result -> {
                            if (result == ButtonType.OK) {
                                taskManager.deleteTask(selected.getId());
                                taskData.setAll(taskManager.getAllTasks());
                            }
                        });
                    }
                });
                MenuItem createAnotherItem = new MenuItem("Create Another Task");
                createAnotherItem.setOnAction(e -> showAddTaskDialog());
                contextMenu.getItems().addAll(editItem, deleteItem, createAnotherItem);
                setContextMenu(contextMenu);

                setGraphic(card);

                // Only animate the first time the cell is shown
                if (firstShow) {
                    card.setOpacity(0);
                    FadeTransition ft = new FadeTransition(Duration.millis(400), card);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.play();
                    firstShow = false;
                } else {
                    card.setOpacity(1);
                }
            }
        }
    }

    // Animate only the visible cards with staggered effect
    private void animateVisibleCards(double scrollPosition) {
        // Get the visible cells
        for (Node node : taskListView.lookupAll(".list-cell")) {
            if (node instanceof ListCell && node.isVisible() && ((ListCell<?>) node).getItem() != null) {
                if (node.getOpacity() < 1.0) {
                    // Get the graphics container
                    Node graphic = ((ListCell<?>) node).getGraphic();
                    if (graphic != null) {
                        // Calculate staggered delay based on position
                        double delay = ((ListCell<?>) node).getIndex() * 50;
                        
                        // Fade in with scaling
                        ScaleTransition st = new ScaleTransition(Duration.millis(150), graphic);
                        st.setFromX(0.96);
                        st.setFromY(0.96);
                        st.setToX(1.0);
                        st.setToY(1.0);
                        
                        FadeTransition ft = new FadeTransition(Duration.millis(200), graphic);
                        ft.setFromValue(graphic.getOpacity());
                        ft.setToValue(1.0);
                        
                        ParallelTransition pt = new ParallelTransition(ft, st);
                        pt.setDelay(Duration.millis(delay));
                        pt.play();
                    }
                }
            }
        }
    }

    // Animate all visible cards in the ListView with a cascade fade-in effect
    private void animateTaskCards() {
        int visibleCellCount = Math.min(taskListView.getItems().size(), 10); // Limit animation to avoid performance issues
        
        for (int i = 0; i < visibleCellCount; i++) {
            Node cell = taskListView.lookup(".list-cell:nth-child(" + (i + 1) + ")");
            if (cell != null && cell instanceof ListCell && ((ListCell<?>) cell).getGraphic() != null) {
                Node graphic = ((ListCell<?>) cell).getGraphic();
                graphic.setOpacity(0);
                graphic.setScaleX(0.95);
                graphic.setScaleY(0.95);
                
                // Staggered delay based on index
                double delay = i * 60;
                
                // Combined scale and fade transitions
                ScaleTransition st = new ScaleTransition(Duration.millis(300), graphic);
                st.setFromX(0.95);
                st.setFromY(0.95);
                st.setToX(1.0);
                st.setToY(1.0);
                
                FadeTransition ft = new FadeTransition(Duration.millis(350), graphic);
                ft.setFromValue(0);
                ft.setToValue(1.0);
                
                ParallelTransition pt = new ParallelTransition(st, ft);
                pt.setDelay(Duration.millis(delay));
                pt.play();
            }
        }
    }

    // Export tasks to JSON file
    private void exportTasksToJson() {
        // Store reference to the current stage and its state
        Stage mainStage = (Stage) tabPane.getScene().getWindow();
        boolean wasMaximized = mainStage.isMaximized();
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Tasks to JSON");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        fileChooser.setInitialFileName("tasks_export.json");
        
        File file = fileChooser.showSaveDialog(mainStage);
        
        // Restore maximized state if needed
        if (wasMaximized) {
            javafx.application.Platform.runLater(() -> {
                mainStage.setMaximized(true);
            });
        }
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Create Gson with LocalDateTime adapter
                Gson gson = createGsonWithAdapters();
                
                // Convert task list to JSON and write to file
                List<Task> tasks = taskManager.getAllTasks();
                String json = gson.toJson(tasks);
                writer.write(json);
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Export Successful");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Tasks have been successfully exported to " + file.getAbsolutePath());
                
                // Add an event handler to restore maximized state when dialog closes
                successAlert.setOnHidden(e -> {
                    if (wasMaximized) {
                        // Use Platform.runLater to ensure this happens after all other events
                        javafx.application.Platform.runLater(() -> {
                            mainStage.setMaximized(true);
                        });
                    }
                });
                
                successAlert.showAndWait();
                
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Export Error");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText("Failed to export tasks: " + ex.getMessage());
                
                // Add an event handler to restore maximized state when dialog closes
                errorAlert.setOnHidden(e -> {
                    if (wasMaximized) {
                        // Use Platform.runLater to ensure this happens after all other events
                        javafx.application.Platform.runLater(() -> {
                            mainStage.setMaximized(true);
                        });
                    }
                });
                
                errorAlert.showAndWait();
            }
        }
    }
    
    // Import tasks from JSON file
    private void importTasksFromJson() {
        // Store reference to the current stage and its state
        Stage mainStage = (Stage) tabPane.getScene().getWindow();
        boolean wasMaximized = mainStage.isMaximized();
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Tasks from JSON");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        
        File file = fileChooser.showOpenDialog(mainStage);
        
        // Restore maximized state if needed
        if (wasMaximized) {
            javafx.application.Platform.runLater(() -> {
                mainStage.setMaximized(true);
            });
        }
        
        if (file != null) {
            try (FileReader reader = new FileReader(file)) {
                // Create confirmation dialog
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Import Tasks");
                confirmAlert.setHeaderText("Import Mode");
                confirmAlert.setContentText("Do you want to replace all existing tasks or add to them?");
                
                ButtonType replaceButton = new ButtonType("Replace All");
                ButtonType addButton = new ButtonType("Add to Existing");
                ButtonType cancelButton = ButtonType.CANCEL;
                
                confirmAlert.getButtonTypes().setAll(replaceButton, addButton, cancelButton);
                
                if (wasMaximized) {
                    // Add an event handler to restore maximized state when dialog closes
                    confirmAlert.setOnCloseRequest(e -> {
                        javafx.application.Platform.runLater(() -> {
                            mainStage.setMaximized(true);
                        });
                    });
                }
                
                ButtonType result = confirmAlert.showAndWait().orElse(cancelButton);
                
                if (result == cancelButton) {
                    return;
                }
                
                if (result == replaceButton) {
                    // Delete all existing tasks
                    for (Task task : new ArrayList<>(taskManager.getAllTasks())) {
                        taskManager.deleteTask(task.getId());
                    }
                }
                
                // Create Gson with LocalDateTime adapter
                Gson gson = createGsonWithAdapters();
                
                // Define the type for list of tasks
                Type taskListType = new TypeToken<ArrayList<Task>>(){}.getType();
                
                // Parse JSON
                List<Task> importedTasks = gson.fromJson(reader, taskListType);
                int importCount = 0;
                
                // Add imported tasks
                if (importedTasks != null) {
                    for (Task task : importedTasks) {
                        taskManager.addTask(task);
                        importCount++;
                    }
                }
                
                // Refresh the task data
                taskData.setAll(taskManager.getAllTasks());
                
                // Show success message
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Import Successful");
                successAlert.setHeaderText(null);
                successAlert.setContentText(importCount + " tasks have been successfully imported.");
                
                // Add an event handler to restore maximized state when dialog closes
                successAlert.setOnHidden(e -> {
                    if (wasMaximized) {
                        // Use Platform.runLater to ensure this happens after all other events
                        javafx.application.Platform.runLater(() -> {
                            mainStage.setMaximized(true);
                        });
                    }
                });
                
                successAlert.showAndWait();
                
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Import Error");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText("Failed to import tasks: " + ex.getMessage());
                
                // Add an event handler to restore maximized state when dialog closes
                errorAlert.setOnHidden(e -> {
                    if (wasMaximized) {
                        // Use Platform.runLater to ensure this happens after all other events
                        javafx.application.Platform.runLater(() -> {
                            mainStage.setMaximized(true);
                        });
                    }
                });
                
                errorAlert.showAndWait();
            }
        }
    }
    
    // Create Gson with custom adapters for LocalDateTime
    private Gson createGsonWithAdapters() {
        // Create adapter for LocalDateTime
        JsonSerializer<LocalDateTime> dateTimeSerializer = (src, typeOfSrc, context) -> 
            new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        JsonDeserializer<LocalDateTime> dateTimeDeserializer = (json, typeOfT, context) -> 
            LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Create Gson builder with adapters
        return new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, dateTimeSerializer)
            .registerTypeAdapter(LocalDateTime.class, dateTimeDeserializer)
            .create();
    }

    // Adjust tab sizes based on available width
    private void adjustTabSizes(double width) {
        if (width < 600) {
            // Very small screens - reduce minimum width to ensure tabs are visible
            tabPane.setTabMinWidth(50);
            tabPane.setTabMaxWidth(100);
            
            // For extremely small screens, ensure tabs are still usable
            if (width < 400) {
                ensureTabVisibility();
            } else {
                // Restore standard tab view for small but not tiny screens
                restoreTabLabels();
            }
        } else if (width < 800) {
            // Small screens
            tabPane.setTabMinWidth(70);
            tabPane.setTabMaxWidth(120);
            restoreTabLabels();
        } else {
            // Normal/large screens
            tabPane.setTabMinWidth(80);
            tabPane.setTabMaxWidth(150);
            restoreTabLabels();
        }
    }
    
    // Handle very small screens by potentially using icons instead of text
    private void ensureTabVisibility() {
        // Store original tab texts if not already stored
        for (Tab tab : tabPane.getTabs()) {
            // Store original text if not already stored
            if (!tab.getProperties().containsKey("originalText")) {
                tab.getProperties().put("originalText", tab.getText());
                
                // For extremely small screens, we can use shorter text or icons
                if (tab.getText().equals("Tasks")) {
                    tab.setText("T");
                } else if (tab.getText().equals("Calendar")) {
                    tab.setText("C");
                } else if (tab.getText().equals("Dashboard")) {
                    tab.setText("D");
                }
            }
        }
    }
    
    // Restore original tab labels
    private void restoreTabLabels() {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getProperties().containsKey("originalText")) {
                tab.setText((String)tab.getProperties().get("originalText"));
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}