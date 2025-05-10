package com.taskmanager.view;

import com.taskmanager.service.TaskManager;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Map;
import java.util.stream.Collectors;
import com.taskmanager.model.Task;

public class DashboardView extends VBox {
    private final TaskManager taskManager;
    private PieChart statusChart;
    private BarChart<String, Number> categoryChart;
    private BarChart<String, Number> priorityChart;

    public DashboardView(TaskManager taskManager) {
        this.taskManager = taskManager;
        setPadding(new Insets(10));
        setSpacing(20);
        
        initializeCharts();
        layoutCharts();
        updateData();
    }

    private void initializeCharts() {
        // Status Distribution Chart
        statusChart = new PieChart();
        statusChart.setTitle("Task Status Distribution");
        
        // Category Distribution Chart
        CategoryAxis categoryAxis = new CategoryAxis();
        NumberAxis categoryValueAxis = new NumberAxis();
        categoryChart = new BarChart<>(categoryAxis, categoryValueAxis);
        categoryChart.setTitle("Tasks by Category");
        
        // Priority Distribution Chart
        CategoryAxis priorityAxis = new CategoryAxis();
        NumberAxis priorityValueAxis = new NumberAxis();
        priorityChart = new BarChart<>(priorityAxis, priorityValueAxis);
        priorityChart.setTitle("Tasks by Priority");
    }

    private void layoutCharts() {
        HBox topCharts = new HBox(20);
        topCharts.getChildren().addAll(statusChart, categoryChart);
        topCharts.setPadding(new Insets(10));
        
        HBox bottomCharts = new HBox(20);
        bottomCharts.getChildren().add(priorityChart);
        bottomCharts.setPadding(new Insets(10));
        
        getChildren().addAll(topCharts, bottomCharts);
    }

    public void updateData() {
        // Update Status Chart
        ObservableList<PieChart.Data> statusData = FXCollections.observableArrayList(
            new PieChart.Data("Completed", taskManager.getCompletedTasks().size()),
            new PieChart.Data("Pending", taskManager.getTasksByPriority().size())
        );
        statusChart.setData(statusData);
        
        // Update Category Chart
        Map<String, Long> categoryCounts = taskManager.getTasksByPriority().stream()
            .collect(Collectors.groupingBy(Task::getCategory, Collectors.counting()));
        ObservableList<XYChart.Data<String, Number>> categoryData = FXCollections.observableArrayList();
        categoryCounts.forEach((category, count) -> 
            categoryData.add(new XYChart.Data<>(category, count)));
        categoryChart.getData().clear();
        categoryChart.getData().add(new XYChart.Series<>("Categories", categoryData));
        
        // Update Priority Chart
        Map<Integer, Long> priorityCounts = taskManager.getTasksByPriority().stream()
            .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
        ObservableList<XYChart.Data<String, Number>> priorityData = FXCollections.observableArrayList();
        priorityCounts.forEach((priority, count) -> 
            priorityData.add(new XYChart.Data<>(String.valueOf(priority), count)));
        priorityChart.getData().clear();
        priorityChart.getData().add(new XYChart.Series<>("Priorities", priorityData));
    }
} 