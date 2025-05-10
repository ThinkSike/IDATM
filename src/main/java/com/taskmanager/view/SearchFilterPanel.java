package com.taskmanager.view;

import com.taskmanager.service.TaskManager;
import com.taskmanager.model.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.stream.Collectors;

public class SearchFilterPanel extends HBox {
    private final TaskManager taskManager;
    private TextField searchField;
    private ComboBox<String> categoryFilter;
    private ComboBox<Integer> priorityFilter;
    private DatePicker startDate;
    private DatePicker endDate;
    private ComboBox<String> statusFilter;
    private ListView<Task> taskListView;

    public SearchFilterPanel(TaskManager taskManager, ListView<Task> taskListView) {
        this.taskManager = taskManager;
        this.taskListView = taskListView;
        setPadding(new Insets(10));
        setSpacing(10);
        
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        searchField = new TextField();
        searchField.setPromptText("Search tasks...");
        searchField.textProperty().addListener((observable, oldValue, newVal) -> {
            List<Task> filteredTasks = taskManager.getTasksByPriority().stream()
                .filter(task -> task.getTitle().toLowerCase().contains(newVal.toLowerCase()) ||
                              task.getDescription().toLowerCase().contains(newVal.toLowerCase()))
                .collect(Collectors.toList());
            updateTaskList(filteredTasks);
        });
        
        categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("Category");
        refreshCategories();
        
        priorityFilter = new ComboBox<>();
        priorityFilter.getItems().addAll(1, 2, 3, 4, 5);
        priorityFilter.setPromptText("Priority");
        
        startDate = new DatePicker();
        startDate.setPromptText("Start Date");
        
        endDate = new DatePicker();
        endDate.setPromptText("End Date");
        
        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "Pending", "Completed");
        statusFilter.setValue("All");
    }

    private void layoutComponents() {
        getChildren().addAll(
            new Label("Search:"),
            searchField,
            new Label("Category:"),
            categoryFilter,
            new Label("Priority:"),
            priorityFilter,
            new Label("Date Range:"),
            startDate,
            new Label("to"),
            endDate,
            new Label("Status:"),
            statusFilter
        );
    }

    public void refreshCategories() {
        categoryFilter.getItems().clear();
        categoryFilter.getItems().addAll(
            "Work", "Personal", "Study", "Health", "Finance", "Home", "Shopping", "Other"
        );
    }

    private void updateTaskList(List<Task> tasks) {
        taskListView.getItems().clear();
        taskListView.getItems().addAll(tasks);
    }
} 