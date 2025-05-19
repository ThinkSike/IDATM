package com.taskmanager.view;

import com.taskmanager.model.Task;
import com.taskmanager.service.TaskManager;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.time.*;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import javafx.scene.input.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;

/**
 * Calendar tab with month view.
 * 
 * @SuppressWarnings is used to suppress warnings related to JavaFX collection operations
 * which are type-safe at runtime due to JavaFX's internal checks.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonthViewCalendarTab extends VBox {
    private final TaskManager taskManager;
    private YearMonth currentMonth;
    private GridPane monthGrid;
    private Label monthLabel;

    public MonthViewCalendarTab(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.currentMonth = YearMonth.now();
        setPadding(new Insets(20));
        setSpacing(20);
        getStyleClass().add("calendar-container");
        getChildren().addAll(createNavigationBar(), createMonthGrid());
        updateMonthGrid();
    }

    private HBox createNavigationBar() {
        HBox nav = new HBox(15);
        nav.setPadding(new Insets(10));
        nav.setAlignment(Pos.CENTER);
        nav.getStyleClass().add("calendar-nav-bar");
        
        // Title with month and year
        monthLabel = new Label();
        monthLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        monthLabel.getStyleClass().add("calendar-month-title");
        
        // Navigation buttons with better styling
        Button prevMonth = new Button("◄ Previous");
        prevMonth.getStyleClass().add("calendar-nav-button");
        
        Button nextMonth = new Button("Next ►");
        nextMonth.getStyleClass().add("calendar-nav-button");
        
        Button todayButton = new Button("Today");
        todayButton.getStyleClass().add("calendar-today-button");
        
        // Help icon with interaction instructions
        Button helpButton = new Button("?");
        helpButton.getStyleClass().add("calendar-help-button");
        
        Tooltip helpTooltip = new Tooltip(
            "Calendar Interactions:\n" +
            "• Double-click day: Create new task\n" +
            "• Double-click task: Edit task\n" +
            "• Ctrl+click task: Toggle completion\n" +
            "• Drag task: Move to different day\n" +
            "• Right-click day: Show context menu"
        );
        helpTooltip.setShowDuration(Duration.seconds(10));
        Tooltip.install(helpButton, helpTooltip);
        
        prevMonth.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateMonthGrid();
        });
        
        nextMonth.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateMonthGrid();
        });
        
        todayButton.setOnAction(e -> {
            currentMonth = YearMonth.now();
            updateMonthGrid();
        });

        // Add a spacer HBox to push elements to the sides
        HBox leftControls = new HBox(10);
        leftControls.setAlignment(Pos.CENTER_LEFT);
        leftControls.getChildren().add(prevMonth);
        
        HBox centerControls = new HBox(10);
        centerControls.setAlignment(Pos.CENTER);
        centerControls.getChildren().addAll(monthLabel, todayButton);
        
        HBox rightControls = new HBox(10);
        rightControls.setAlignment(Pos.CENTER_RIGHT);
        rightControls.getChildren().addAll(helpButton, nextMonth);
        
        // Use regions with priorities to space controls evenly
        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        
        nav.getChildren().addAll(leftControls, leftSpacer, centerControls, rightSpacer, rightControls);
        
        // Add a bottom border to the navigation bar
        Border bottomBorder = new Border(
            new BorderStroke(
                Color.TRANSPARENT, Color.TRANSPARENT, Color.LIGHTGRAY, Color.TRANSPARENT,
                BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 1, 0),
                Insets.EMPTY
            )
        );
        nav.setBorder(bottomBorder);
        
        return nav;
    }

    private GridPane createMonthGrid() {
        monthGrid = new GridPane();
        monthGrid.setHgap(10);
        monthGrid.setVgap(10);
        monthGrid.setPadding(new Insets(15));
        monthGrid.getStyleClass().add("calendar-grid");
        monthGrid.setMaxWidth(Double.MAX_VALUE);
        monthGrid.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(monthGrid, Priority.ALWAYS);
        
        // Add a subtle drop shadow to the entire grid
        DropShadow gridShadow = new DropShadow();
        gridShadow.setRadius(5.0);
        gridShadow.setOffsetX(1.0);
        gridShadow.setOffsetY(1.0);
        gridShadow.setColor(Color.color(0, 0, 0, 0.2));
        monthGrid.setEffect(gridShadow);
        
        return monthGrid;
    }

    private void updateMonthGrid() {
        monthGrid.getChildren().clear();
        monthGrid.getColumnConstraints().clear();
        monthGrid.getRowConstraints().clear();
        
        // Header row: days of week
        String[] daysOfWeek = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (int d = 0; d < 7; d++) {
            VBox dayHeader = new VBox();
            dayHeader.setAlignment(Pos.CENTER);
            dayHeader.setPadding(new Insets(8));
            dayHeader.getStyleClass().add("calendar-day-header");
            
            // Add day name (abbreviated and full)
            Label dayLabel = new Label(daysOfWeek[d].substring(0, 3));
            dayLabel.getStyleClass().add("calendar-day-name");
            
            // Add weekend styling
            if (d == 0 || d == 6) {
                dayLabel.getStyleClass().add("calendar-weekend-day");
            }
            
            dayHeader.getChildren().addAll(dayLabel);
            monthGrid.add(dayHeader, d, 0);
        }
        
        // Set column constraints for equal width
        for (int d = 0; d < 7; d++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPercentWidth(100.0 / 7);
            colConst.setHgrow(Priority.ALWAYS);
            monthGrid.getColumnConstraints().add(colConst);
        }
        
        // Fill in the days
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
        int daysInMonth = currentMonth.lengthOfMonth();
        int row = 1, col = 0;
        int totalRows = ((firstDayOfWeek + daysInMonth - 1) / 7) + 1;
        
        // Set row constraints for equal height (including header row)
        for (int r = 0; r <= totalRows; r++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setPercentHeight(100.0 / (totalRows + 1));
            rowConst.setVgrow(Priority.ALWAYS);
            if (r == 0) {
                // Make header row slightly smaller
                rowConst.setPercentHeight(10);
            }
            monthGrid.getRowConstraints().add(rowConst);
        }
        
        // Add empty cells before the first day
        for (col = 0; col < firstDayOfWeek; col++) {
            VBox emptyCell = new VBox();
            emptyCell.setMinHeight(80);
            emptyCell.setMinWidth(100);
            emptyCell.setMaxWidth(Double.MAX_VALUE);
            emptyCell.setMaxHeight(Double.MAX_VALUE);
            emptyCell.getStyleClass().add("calendar-cell");
            emptyCell.getStyleClass().add("calendar-empty-cell");
            GridPane.setHgrow(emptyCell, Priority.ALWAYS);
            GridPane.setVgrow(emptyCell, Priority.ALWAYS);
            monthGrid.add(emptyCell, col, row);
        }
        
        // Add day cells
        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate date = currentMonth.atDay(i);
            VBox dayCell = createDayCell(date);
            GridPane.setHgrow(dayCell, Priority.ALWAYS);
            GridPane.setVgrow(dayCell, Priority.ALWAYS);
            monthGrid.add(dayCell, col, row);
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
        
        // Update month label
        monthLabel.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentMonth.getYear());
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(3);
        cell.setPadding(new Insets(5));
        cell.getStyleClass().add("calendar-cell");
        cell.setMinHeight(80);
        cell.setMinWidth(100);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(cell, Priority.ALWAYS);
        
        // Date header container
        HBox dateHeader = new HBox();
        dateHeader.setAlignment(Pos.CENTER_LEFT);
        dateHeader.setPadding(new Insets(2, 0, 5, 0));
        
        // Date number label
        Label dateLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dateLabel.getStyleClass().add("calendar-date-label");
        
        // Highlight today's date
        if (date.equals(LocalDate.now())) {
            StackPane todayIndicator = new StackPane();
            todayIndicator.getStyleClass().add("calendar-today-indicator");
            
            Circle circle = new Circle(12);
            circle.setFill(Color.DODGERBLUE);
            
            dateLabel.setTextFill(Color.WHITE);
            todayIndicator.getChildren().add(circle);
            todayIndicator.getChildren().add(dateLabel);
            dateHeader.getChildren().add(todayIndicator);
        } else {
            dateHeader.getChildren().add(dateLabel);
            
            // Add weekend styling
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                cell.getStyleClass().add("calendar-weekend-cell");
                dateLabel.getStyleClass().add("calendar-weekend-day");
            }
        }
        
        // Add different month styling
        if (date.getMonth() != currentMonth.getMonth()) {
            cell.getStyleClass().add("calendar-different-month-cell");
        }
        
        cell.getChildren().add(dateHeader);
        
        // Create scrollable task container
        VBox taskContainer = new VBox(2);
        taskContainer.setSpacing(3);
        taskContainer.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(taskContainer, Priority.ALWAYS);
        
        // Get tasks for this date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<Task> dayTasks = taskManager.getTasksByDateRange(startOfDay, endOfDay);
        
        // Show task names with drag-and-drop and priority styling
        for (Task task : dayTasks) {
            // Create task item
            HBox taskItem = new HBox(5);
            taskItem.setAlignment(Pos.CENTER_LEFT);
            taskItem.getStyleClass().add("calendar-task-item");
            
            // Add priority indicator
            Circle priorityIndicator = createPriorityIndicator(task.getPriority());
            
            // Completed indicator
            if (task.isCompleted()) {
                taskItem.getStyleClass().add("calendar-task-completed");
            }
            
            // Task title with ellipsis if too long
            Label taskLabel = new Label(task.getTitle());
            taskLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(taskLabel, Priority.ALWAYS);
            taskLabel.getStyleClass().add("calendar-task-label");
            
            taskItem.getChildren().addAll(priorityIndicator, taskLabel);
            
            // Make the entire task item clickable for editing
            taskItem.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    // Single click to toggle completion
                    if (event.getClickCount() == 1 && event.isControlDown()) {
                        task.setCompleted(!task.isCompleted());
                        taskManager.updateTask(task);
                        updateMonthGrid();
                        event.consume();
                    } 
                    // Double click to edit the task
                    else if (event.getClickCount() == 2) {
                        showEditTaskDialog(task);
                        event.consume();
                    }
                }
            });
            
            // Drag detected
            taskItem.setOnDragDetected(event -> {
                Dragboard db = taskItem.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(task.getId()));
                db.setContent(content);
                event.consume();
            });
            
            // Show task details on hover
            Tooltip tooltip = new Tooltip(
                "Title: " + task.getTitle() + "\n" +
                "Description: " + task.getDescription() + "\n" +
                "Priority: " + task.getPriority() + "\n" +
                "Time: " + task.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "\n" +
                "Status: " + (task.isCompleted() ? "Completed" : "Pending") + "\n\n" +
                "Double-click to edit\n" +
                "Ctrl+click to toggle completion"
            );
            Tooltip.install(taskItem, tooltip);
            
            taskContainer.getChildren().add(taskItem);
        }
        
        cell.getChildren().add(taskContainer);
        
        // Accept drag over
        cell.setOnDragOver(event -> {
            if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        
        // Handle drop
        cell.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                try {
                    long taskId = Long.parseLong(db.getString());
                    Task draggedTask = taskManager.getAllTasks().stream().filter(t -> t.getId() == taskId).findFirst().orElse(null);
                    if (draggedTask != null && !draggedTask.getDueDate().toLocalDate().equals(date)) {
                        // Keep the same time, change only the date
                        LocalDateTime newDue = date.atTime(draggedTask.getDueDate().toLocalTime());
                        draggedTask.setDueDate(newDue);
                        taskManager.updateTask(draggedTask);
                        updateMonthGrid();
                        success = true;
                    }
                } catch (Exception ignored) {}
            }
            event.setDropCompleted(success);
            event.consume();
        });
        
        // Unified mouse event handler for both right-click menu and double-click
        cell.setOnMouseClicked(e -> {
            // Handle double-click to create a new task
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                showCreateTaskDialog(date);
                return;
            }
            
            // Handle right-click context menu
            if (e.getButton() == MouseButton.SECONDARY) {
                ContextMenu menu = new ContextMenu();
                if (!dayTasks.isEmpty()) {
                    MenuItem editTask = new MenuItem("Edit Task");
                    if (dayTasks.size() == 1) {
                        // For a single task, edit directly
                        editTask.setOnAction(ev -> showEditTaskDialog(dayTasks.get(0)));
                    } else {
                        // For multiple tasks, show selection dialog
                        editTask.setOnAction(ev -> showSelectTaskToEditDialog(dayTasks));
                    }
                    MenuItem createTask = new MenuItem("Create Another Task");
                    createTask.setOnAction(ev -> showCreateTaskDialog(date));
                    MenuItem deleteTask = new MenuItem("Delete Task");
                    if (dayTasks.size() == 1) {
                        deleteTask.setOnAction(ev -> {
                            Task taskToDelete = dayTasks.get(0);
                            // Get the main window and store its maximized state
                            Stage mainStage = (Stage) getScene().getWindow();
                            boolean wasMaximized = mainStage.isMaximized();
                            
                            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                            confirmDialog.setTitle("Confirm Delete");
                            confirmDialog.setHeaderText("Delete Task");
                            confirmDialog.setContentText("Are you sure you want to delete the task: " + taskToDelete.getTitle() + "?");
                            
                            // Add an event handler to restore maximized state when dialog closes
                            confirmDialog.setOnHidden(dialogEvent -> {
                                if (wasMaximized) {
                                    // Use Platform.runLater to ensure this happens after all other events
                                    Platform.runLater(() -> {
                                        mainStage.setMaximized(true);
                                    });
                                }
                            });
                            
                            confirmDialog.showAndWait().ifPresent(response -> {
                                if (response == ButtonType.OK) {
                                    taskManager.deleteTask(taskToDelete.getId());
                                    updateMonthGrid();
                                }
                            });
                        });
                    } else {
                        deleteTask.setOnAction(ev -> showDeleteTaskDialog(dayTasks));
                    }
                    menu.getItems().addAll(editTask, createTask, deleteTask);
                } else {
                    MenuItem createTask = new MenuItem("Create Task");
                    createTask.setOnAction(ev -> showCreateTaskDialog(date));
                    menu.getItems().add(createTask);
                }
                menu.show(cell, e.getScreenX(), e.getScreenY());
            }
        });
        
        return cell;
    }

    private Circle createPriorityIndicator(int priority) {
        Circle priorityIndicator = new Circle(4);
        priorityIndicator.getStyleClass().add("task-priority-indicator");
        priorityIndicator.getStyleClass().add("task-priority-" + priority);
        
        // Add programmatic glow effect to match CSS
        DropShadow glow = new DropShadow();
        glow.setRadius(6);
        glow.setSpread(0.4);
        
        // Set color based on priority
        switch (priority) {
            case 1: glow.setColor(Color.web("#FF1744")); break;
            case 2: glow.setColor(Color.web("#FF9100")); break;
            case 3: glow.setColor(Color.web("#FFEA00")); break;
            case 4: glow.setColor(Color.web("#00E676")); break;
            case 5: glow.setColor(Color.web("#00B0FF")); break;
            default: glow.setColor(Color.web("#2196F3")); break;
        }
        
        priorityIndicator.setEffect(glow);
        return priorityIndicator;
    }

    /**
     * Detects if the application is currently using dark theme
     */
    private boolean isDarkTheme() {
        Scene scene = this.getScene();
        if (scene != null) {
            // Check if the scene has the dark theme stylesheet applied
            for (String stylesheet : scene.getStylesheets()) {
                if (stylesheet.toLowerCase().contains("dark")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Applies theme-consistent styling to a dialog
     */
    private void applyThemeToDialog(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        Scene dialogScene = dialogPane.getScene();
        
        // First clear any existing style sheets
        dialogScene.getStylesheets().clear();
        
        // Add the appropriate theme stylesheet
        if (isDarkTheme()) {
            dialogScene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
        } else {
            dialogScene.getStylesheets().add(getClass().getResource("/styles/light-theme.css").toExternalForm());
        }
    }
    
    /**
     * Applies theme-consistent styling to an alert
     */
    private void applyThemeToAlert(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        Scene dialogScene = dialogPane.getScene();
        
        // First clear any existing style sheets
        dialogScene.getStylesheets().clear();
        
        // Add the appropriate theme stylesheet
        if (isDarkTheme()) {
            dialogScene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
        } else {
            dialogScene.getStylesheets().add(getClass().getResource("/styles/light-theme.css").toExternalForm());
        }
    }

    private void showCreateTaskDialog(LocalDate date) {
        // Get the main window and store its maximized state
        Stage mainStage = (Stage) getScene().getWindow();
        boolean wasMaximized = mainStage.isMaximized();
        
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Create Task");
        dialog.setHeaderText("Create Task for " + date);
        
        // Add custom styling class to dialog pane
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("modern-dialog");
        
        // Create a VBox as the main container with proper spacing and padding
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setMinWidth(400);
        
        // Create fields with better styling
        Label titleLabel = new Label("Title");
        titleLabel.getStyleClass().add("dialog-label");
        TextField titleField = new TextField();
        titleField.setPromptText("Enter task title");
        titleField.getStyleClass().add("dialog-field");
        
        Label descLabel = new Label("Description");
        descLabel.getStyleClass().add("dialog-label");
        TextArea descField = new TextArea();
        descField.setPromptText("Enter task description");
        descField.setPrefRowCount(3);
        descField.getStyleClass().add("dialog-field");
        descField.setWrapText(true);
        
        Label categoryLabel = new Label("Category");
        categoryLabel.getStyleClass().add("dialog-label");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Work", "Personal", "Study", "Health", "Finance", "Home", "Shopping", "Other");
        categoryCombo.setValue("Other");
        categoryCombo.getStyleClass().add("dialog-field");
        
        Label priorityLabel = new Label("Priority");
        priorityLabel.getStyleClass().add("dialog-label");
        
        // Create a priority slider with labels
        HBox priorityBox = new HBox(10);
        priorityBox.setAlignment(Pos.CENTER_LEFT);
        
        ComboBox<Integer> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(1, 2, 3, 4, 5);
        priorityCombo.setValue(3);
        priorityCombo.getStyleClass().add("dialog-field");
        priorityCombo.getStyleClass().add("priority-dropdown");
        
        // Set cell factory for priority combo box to apply proper styling
        priorityCombo.setCellFactory(param -> new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("Priority " + item);
                    getStyleClass().removeIf(s -> s.startsWith("priority-level-"));
                    getStyleClass().add("priority-level-" + item);
                }
            }
        });
        
        // Set button cell to show selected priority with styling
        priorityCombo.setButtonCell(new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("Priority " + item);
                    getStyleClass().removeIf(s -> s.startsWith("priority-level-"));
                    getStyleClass().add("priority-level-" + item);
                }
            }
        });
        
        // Add priority level labels with colored circles
        for (int i = 1; i <= 5; i++) {
            Circle priorityCircle = new Circle(8);
            priorityCircle.getStyleClass().add("task-priority-" + i);
            
            // Create a glow effect for better visibility
            DropShadow glow = new DropShadow();
            glow.setRadius(8);
            glow.setSpread(0.5);
            
            switch (i) {
                case 1: glow.setColor(Color.web("#FF1744")); break;
                case 2: glow.setColor(Color.web("#FF9100")); break;
                case 3: glow.setColor(Color.web("#FFEA00")); break;
                case 4: glow.setColor(Color.web("#00E676")); break;
                case 5: glow.setColor(Color.web("#00B0FF")); break;
            }
            
            priorityCircle.setEffect(glow);
            
            // Add a listener to highlight the selected priority
            final int priority = i;
            priorityCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal == priority) {
                    priorityCircle.setStroke(Color.BLACK);
                    priorityCircle.setStrokeWidth(1.5);
                } else {
                    priorityCircle.setStroke(null);
                }
            });
            
            priorityBox.getChildren().add(priorityCircle);
        }
        
        Label timeLabel = new Label("Time (HH:mm)");
        timeLabel.getStyleClass().add("dialog-label");
        TextField timeField = new TextField();
        timeField.setPromptText("e.g., 14:30");
        timeField.getStyleClass().add("dialog-field");
        
        // Organize fields in the content VBox
        content.getChildren().addAll(
            titleLabel, titleField, 
            descLabel, descField,
            categoryLabel, categoryCombo,
            priorityLabel, priorityCombo, priorityBox,
            timeLabel, timeField
        );
        
        // Set dialog content
        dialogPane.setContent(content);
        
        // Style buttons
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        
        okButton.getStyleClass().add("dialog-button");
        okButton.getStyleClass().add("dialog-primary-button");
        cancelButton.getStyleClass().add("dialog-button");
        
        // Apply theme to dialog
        applyThemeToDialog(dialog);
        
        // Add an event handler to restore maximized state when dialog closes
        dialog.setOnHidden(e -> {
            if (wasMaximized) {
                // Use Platform.runLater to ensure this happens after all other events
                Platform.runLater(() -> {
                    mainStage.setMaximized(true);
                });
            }
        });
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String title = titleField.getText().trim();
                String desc = descField.getText().trim();
                String category = categoryCombo.getValue();
                Integer priority = priorityCombo.getValue();
                String timeStr = timeField.getText().trim();
                if (!title.isEmpty() && timeStr.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    String[] parts = timeStr.split(":");
                    LocalDateTime due = date.atTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                    Task task = new Task(title, desc, category, priority, due);
                    taskManager.addTask(task);
                    updateMonthGrid();
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showEditTaskDialog(Task task) {
        // Get the main window and store its maximized state
        Stage mainStage = (Stage) getScene().getWindow();
        boolean wasMaximized = mainStage.isMaximized();
        
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");
        dialog.setHeaderText("Edit Task for " + task.getDueDate().toLocalDate());
        
        // Add custom styling class to dialog pane
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("modern-dialog");
        
        // Create a VBox as the main container with proper spacing and padding
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setMinWidth(400);
        
        // Create fields with better styling
        Label titleLabel = new Label("Title");
        titleLabel.getStyleClass().add("dialog-label");
        TextField titleField = new TextField(task.getTitle());
        titleField.getStyleClass().add("dialog-field");
        
        Label descLabel = new Label("Description");
        descLabel.getStyleClass().add("dialog-label");
        TextArea descField = new TextArea(task.getDescription());
        descField.setPrefRowCount(3);
        descField.getStyleClass().add("dialog-field");
        descField.setWrapText(true);
        
        Label categoryLabel = new Label("Category");
        categoryLabel.getStyleClass().add("dialog-label");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Work", "Personal", "Study", "Health", "Finance", "Home", "Shopping", "Other");
        categoryCombo.setValue(task.getCategory());
        categoryCombo.getStyleClass().add("dialog-field");
        
        Label priorityLabel = new Label("Priority");
        priorityLabel.getStyleClass().add("dialog-label");
        
        // Create a priority slider with labels
        HBox priorityBox = new HBox(10);
        priorityBox.setAlignment(Pos.CENTER_LEFT);
        
        ComboBox<Integer> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(1, 2, 3, 4, 5);
        priorityCombo.setValue(task.getPriority());
        priorityCombo.getStyleClass().add("dialog-field");
        priorityCombo.getStyleClass().add("priority-dropdown");
        
        // Set cell factory for priority combo box to apply proper styling
        priorityCombo.setCellFactory(param -> new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("Priority " + item);
                    getStyleClass().removeIf(s -> s.startsWith("priority-level-"));
                    getStyleClass().add("priority-level-" + item);
                }
            }
        });
        
        // Set button cell to show selected priority with styling
        priorityCombo.setButtonCell(new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("Priority " + item);
                    getStyleClass().removeIf(s -> s.startsWith("priority-level-"));
                    getStyleClass().add("priority-level-" + item);
                }
            }
        });
        
        // Add priority level labels with colored circles
        for (int i = 1; i <= 5; i++) {
            Circle priorityCircle = new Circle(8);
            priorityCircle.getStyleClass().add("task-priority-" + i);
            
            // Create a glow effect for better visibility
            DropShadow glow = new DropShadow();
            glow.setRadius(8);
            glow.setSpread(0.5);
            
            switch (i) {
                case 1: glow.setColor(Color.web("#FF1744")); break;
                case 2: glow.setColor(Color.web("#FF9100")); break;
                case 3: glow.setColor(Color.web("#FFEA00")); break;
                case 4: glow.setColor(Color.web("#00E676")); break;
                case 5: glow.setColor(Color.web("#00B0FF")); break;
            }
            
            priorityCircle.setEffect(glow);
            
            // Add a listener to highlight the selected priority
            final int priority = i;
            priorityCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal == priority) {
                    priorityCircle.setStroke(Color.BLACK);
                    priorityCircle.setStrokeWidth(1.5);
                } else {
                    priorityCircle.setStroke(null);
                }
            });
            
            // Set initial stroke for current priority
            if (task.getPriority() == i) {
                priorityCircle.setStroke(Color.BLACK);
                priorityCircle.setStrokeWidth(1.5);
            }
            
            priorityBox.getChildren().add(priorityCircle);
        }
        
        Label completedLabel = new Label("Status");
        completedLabel.getStyleClass().add("dialog-label");
        HBox statusBox = new HBox(10);
        CheckBox completedCheckBox = new CheckBox("Mark as completed");
        completedCheckBox.setSelected(task.isCompleted());
        completedCheckBox.getStyleClass().add("dialog-checkbox");
        statusBox.getChildren().add(completedCheckBox);
        
        Label timeLabel = new Label("Time (HH:mm)");
        timeLabel.getStyleClass().add("dialog-label");
        TextField timeField = new TextField(String.format("%02d:%02d", task.getDueDate().getHour(), task.getDueDate().getMinute()));
        timeField.getStyleClass().add("dialog-field");
        
        // Organize fields in the content VBox
        content.getChildren().addAll(
            titleLabel, titleField, 
            descLabel, descField,
            categoryLabel, categoryCombo,
            priorityLabel, priorityCombo, priorityBox,
            completedLabel, statusBox,
            timeLabel, timeField
        );
        
        // Set dialog content
        dialogPane.setContent(content);
        
        // Style buttons
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        
        okButton.getStyleClass().add("dialog-button");
        okButton.getStyleClass().add("dialog-primary-button");
        cancelButton.getStyleClass().add("dialog-button");
        
        // Apply theme to dialog
        applyThemeToDialog(dialog);
        
        // Add an event handler to restore maximized state when dialog closes
        dialog.setOnHidden(e -> {
            if (wasMaximized) {
                // Use Platform.runLater to ensure this happens after all other events
                Platform.runLater(() -> {
                    mainStage.setMaximized(true);
                });
            }
        });
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String title = titleField.getText().trim();
                String desc = descField.getText().trim();
                String category = categoryCombo.getValue();
                Integer priority = priorityCombo.getValue();
                String timeStr = timeField.getText().trim();
                boolean completed = completedCheckBox.isSelected();
                
                if (!title.isEmpty() && timeStr.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    String[] parts = timeStr.split(":");
                    LocalDateTime due = task.getDueDate().toLocalDate().atTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                    task.setTitle(title);
                    task.setDescription(desc);
                    task.setCategory(category);
                    task.setPriority(priority);
                    task.setDueDate(due);
                    task.setCompleted(completed);
                    taskManager.updateTask(task);
                    updateMonthGrid();
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showDeleteTaskDialog(List<Task> tasks) {
        // Get the main window and store its maximized state
        Stage mainStage = (Stage) getScene().getWindow();
        boolean wasMaximized = mainStage.isMaximized();
        
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Delete Task");
        dialog.setHeaderText("Select a task to delete");
        
        // Add custom styling class to dialog pane
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("modern-dialog");
        dialogPane.getStyleClass().add("delete-dialog");
        
        // Create a VBox as the main container with proper spacing and padding
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setMinWidth(400);
        
        Label instructionLabel = new Label("Please select the task you want to delete:");
        instructionLabel.getStyleClass().add("dialog-instruction");
        content.getChildren().add(instructionLabel);
        
        // Create a styled list of tasks
        VBox taskList = new VBox(10);
        taskList.setPadding(new Insets(10));
        taskList.getStyleClass().add("task-list-container");
        
        for (Task task : tasks) {
            // Create card-like container for each task
            VBox taskCard = new VBox(5);
            taskCard.setPadding(new Insets(10));
            taskCard.getStyleClass().add("task-card-mini");
            
            // Create title row with priority indicator
            HBox titleRow = new HBox(10);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            
            Circle priorityCircle = new Circle(6);
            priorityCircle.getStyleClass().add("task-priority-" + task.getPriority());
            
            Label titleLabel = new Label(task.getTitle());
            titleLabel.getStyleClass().add("task-title-mini");
            
            titleRow.getChildren().addAll(priorityCircle, titleLabel);
            
            // Create info row
            Label infoLabel = new Label(String.format(
                "Due: %s • Category: %s • %s",
                task.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                task.getCategory(),
                task.isCompleted() ? "Completed" : "Pending"
            ));
            infoLabel.getStyleClass().add("task-info-mini");
            
            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().add("delete-button");
            deleteButton.setMaxWidth(Double.MAX_VALUE);
            
            taskCard.getChildren().addAll(titleRow, infoLabel, deleteButton);
            
            deleteButton.setOnAction(e -> {
                // Get the main window and store its maximized state
                Stage buttonStage = (Stage) getScene().getWindow();
                boolean buttonMaximized = buttonStage.isMaximized();
                
                Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmDialog.setTitle("Confirm Delete");
                confirmDialog.setHeaderText("Delete Task");
                confirmDialog.setContentText("Are you sure you want to delete the task: " + task.getTitle() + "?");
                
                // Style the confirmation dialog
                DialogPane confirmPane = confirmDialog.getDialogPane();
                confirmPane.getStyleClass().add("modern-dialog");
                confirmPane.getStyleClass().add("delete-confirm-dialog");
                
                // Style buttons
                Button confirmButton = (Button) confirmPane.lookupButton(ButtonType.OK);
                Button cancelConfirmButton = (Button) confirmPane.lookupButton(ButtonType.CANCEL);
                
                confirmButton.getStyleClass().add("dialog-button");
                confirmButton.getStyleClass().add("delete-confirm-button");
                cancelConfirmButton.getStyleClass().add("dialog-button");
                
                // Apply theme to alert
                applyThemeToAlert(confirmDialog);
                
                // Add an event handler to restore maximized state when dialog closes
                confirmDialog.setOnHidden(ev -> {
                    if (buttonMaximized) {
                        // Use Platform.runLater to ensure this happens after all other events
                        Platform.runLater(() -> {
                            buttonStage.setMaximized(true);
                        });
                    }
                });
                
                confirmDialog.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        taskManager.deleteTask(task.getId());
                        dialog.close();
                        updateMonthGrid();
                    }
                });
            });
            
            taskList.getChildren().add(taskCard);
        }
        
        // Add scroll pane if there are many tasks
        ScrollPane scrollPane = new ScrollPane(taskList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(Math.min(tasks.size() * 100, 300));
        scrollPane.getStyleClass().add("dialog-scroll-pane");
        
        content.getChildren().add(scrollPane);
        
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CANCEL);
        
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button");
        
        // Apply theme to dialog
        applyThemeToDialog(dialog);
        
        // Add an event handler to restore maximized state when dialog closes
        dialog.setOnHidden(e -> {
            if (wasMaximized) {
                mainStage.setMaximized(true);
            }
        });
        
        dialog.showAndWait();
    }

    private void showSelectTaskToEditDialog(List<Task> tasks) {
        // Get the main window and store its maximized state
        Stage mainStage = (Stage) getScene().getWindow();
        boolean wasMaximized = mainStage.isMaximized();
        
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Select Task to Edit");
        dialog.setHeaderText("Select a task to edit");
        
        // Add custom styling class to dialog pane
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("modern-dialog");
        dialogPane.getStyleClass().add("select-task-dialog");
        
        // Create a VBox as the main container with proper spacing and padding
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setMinWidth(400);
        
        Label instructionLabel = new Label("Please select the task you want to edit:");
        instructionLabel.getStyleClass().add("dialog-instruction");
        content.getChildren().add(instructionLabel);
        
        // Create a styled list of tasks
        VBox taskList = new VBox(10);
        taskList.setPadding(new Insets(10));
        taskList.getStyleClass().add("task-list-container");
        
        for (Task task : tasks) {
            // Create card-like container for each task
            VBox taskCard = new VBox(5);
            taskCard.setPadding(new Insets(10));
            taskCard.getStyleClass().add("task-card-mini");
            
            // Create title row with priority indicator
            HBox titleRow = new HBox(10);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            
            Circle priorityCircle = new Circle(6);
            priorityCircle.getStyleClass().add("task-priority-" + task.getPriority());
            
            Label titleLabel = new Label(task.getTitle());
            titleLabel.getStyleClass().add("task-title-mini");
            
            titleRow.getChildren().addAll(priorityCircle, titleLabel);
            
            // Create info row
            Label infoLabel = new Label(String.format(
                "Due: %s • Category: %s • %s",
                task.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                task.getCategory(),
                task.isCompleted() ? "Completed" : "Pending"
            ));
            infoLabel.getStyleClass().add("task-info-mini");
            
            Button editButton = new Button("Edit");
            editButton.getStyleClass().add("edit-button");
            editButton.setMaxWidth(Double.MAX_VALUE);
            
            taskCard.getChildren().addAll(titleRow, infoLabel, editButton);
            
            editButton.setOnAction(e -> {
                showEditTaskDialog(task);
            });
            
            taskList.getChildren().add(taskCard);
        }
        
        // Add scroll pane if there are many tasks
        ScrollPane scrollPane = new ScrollPane(taskList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(Math.min(tasks.size() * 100, 300));
        scrollPane.getStyleClass().add("dialog-scroll-pane");
        
        content.getChildren().add(scrollPane);
        
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CANCEL);
        
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button");
        
        // Apply theme to dialog
        applyThemeToDialog(dialog);
        
        // Add an event handler to restore maximized state when dialog closes
        dialog.setOnHidden(e -> {
            if (wasMaximized) {
                mainStage.setMaximized(true);
            }
        });
        
        dialog.showAndWait();
    }
} 