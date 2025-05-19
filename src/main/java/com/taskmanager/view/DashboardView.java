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
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.shape.Circle;
import javafx.scene.layout.Priority;
import javafx.scene.Scene;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.geometry.Side;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;

/**
 * Dashboard view showing statistics and overview.
 * 
 * @SuppressWarnings is used to suppress warnings related to JavaFX collection operations
 * which are type-safe at runtime due to JavaFX's internal checks.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DashboardView extends VBox {
    private final TaskManager taskManager;
    private PieChart statusChart;
    private BarChart<String, Number> categoryChart;
    private BarChart<String, Number> priorityChart;
    private LineChart<String, Number> completionTrendChart;
    private Label totalTasksLabel;
    private Label completedTasksLabel;
    private Label pendingTasksLabel;
    private Label overdueTasksLabel;
    private Timeline refreshTimeline;

    public DashboardView(TaskManager taskManager) {
        this.taskManager = taskManager;
        setPadding(new Insets(15));
        setSpacing(25);
        
        initializeCharts();
        initializeStatsLabels();
        layoutDashboard();
        setupAutoRefresh();
        updateData();
    }

    private void initializeStatsLabels() {
        totalTasksLabel = createStatLabel("Total Tasks: 0", "📊");
        completedTasksLabel = createStatLabel("Completed: 0", "✅");
        pendingTasksLabel = createStatLabel("Pending: 0", "⏳");
        overdueTasksLabel = createStatLabel("Overdue: 0", "⚠️");
    }

    private Label createStatLabel(String text, String icon) {
        Label label = new Label(icon + " " + text);
        label.setFont(Font.font("System", FontWeight.BOLD, 16));
        label.setPadding(new Insets(10));
        return label;
    }

    private StackPane createCard(Node content, String title) {
        // Card container
        VBox card = new VBox(10);
        card.getStyleClass().add("dashboard-card");
        card.setPadding(new Insets(15));
        
        // Title
        if (title != null && !title.isEmpty()) {
            Label titleLabel = new Label(title);
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
            titleLabel.getStyleClass().add("card-title");
            card.getChildren().add(titleLabel);
        }
        
        // Content
        card.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        
        // Add card to stack pane for shadow effects
        StackPane cardContainer = new StackPane(card);
        cardContainer.setPadding(new Insets(5));
        
        // Add drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(Color.color(0, 0, 0, 0.2));
        card.setEffect(dropShadow);
        
        return cardContainer;
    }

    private void initializeCharts() {
        // Status Distribution Chart
        statusChart = new PieChart();
        statusChart.setTitle("Task Status Distribution");
        statusChart.setLegendVisible(true);
        statusChart.setLabelsVisible(true);
        statusChart.setLabelLineLength(20);
        statusChart.getStyleClass().add("chart");
        statusChart.setMinHeight(250);
        statusChart.setPrefHeight(250);
        
        // Make chart responsive
        statusChart.setMinSize(200, 200);
        statusChart.setPrefSize(300, 250);
        statusChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // Add a listener to apply styles when chart data changes
        statusChart.dataProperty().addListener((obs, oldData, newData) -> {
            if (newData != null) {
                Platform.runLater(() -> {
                    for (PieChart.Data data : newData) {
                        if (data.getNode() != null) {
                            String styleClass = "";
                            if (data.getName().equals("Completed")) {
                                styleClass = "chart-pie-completed";
                            } else if (data.getName().equals("Pending")) {
                                styleClass = "chart-pie-pending";
                            } else if (data.getName().equals("Overdue")) {
                                styleClass = "chart-pie-overdue";
                            }
                            if (!styleClass.isEmpty()) {
                                data.getNode().getStyleClass().add(styleClass);
                            }
                        }
                    }
                });
            }
        });
        
        // Category Distribution Chart
        CategoryAxis categoryAxis = new CategoryAxis();
        NumberAxis categoryValueAxis = new NumberAxis();
        categoryValueAxis.setLabel("Number of Tasks");
        categoryChart = new BarChart<>(categoryAxis, categoryValueAxis);
        categoryChart.setTitle("Tasks by Category");
        categoryChart.setLegendVisible(false);
        categoryChart.setAnimated(true);
        categoryChart.getStyleClass().add("chart");
        categoryAxis.getStyleClass().add("axis");
        categoryValueAxis.getStyleClass().add("axis");
        
        // Make chart responsive
        categoryChart.setMinSize(200, 200);
        categoryChart.setPrefSize(300, 250);
        categoryChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        categoryChart.setLegendSide(Side.BOTTOM);
        
        // Priority Distribution Chart
        CategoryAxis priorityAxis = new CategoryAxis();
        priorityAxis.setLabel("Priority Level");
        NumberAxis priorityValueAxis = new NumberAxis();
        priorityValueAxis.setLabel("Number of Tasks");
        priorityChart = new BarChart<>(priorityAxis, priorityValueAxis);
        priorityChart.setTitle("Tasks by Priority");
        priorityChart.setLegendVisible(false);
        priorityChart.setAnimated(true);
        priorityChart.getStyleClass().add("chart");
        priorityAxis.getStyleClass().add("axis");
        priorityValueAxis.getStyleClass().add("axis");
        
        // Make chart responsive
        priorityChart.setMinSize(200, 200);
        priorityChart.setPrefSize(300, 250);
        priorityChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        priorityChart.setLegendSide(Side.BOTTOM);

        // Completion Trend Chart
        CategoryAxis trendAxis = new CategoryAxis();
        trendAxis.setLabel("Date");
        NumberAxis trendValueAxis = new NumberAxis();
        trendValueAxis.setLabel("Completed Tasks");
        completionTrendChart = new LineChart<>(trendAxis, trendValueAxis);
        completionTrendChart.setTitle("Task Completion Trend (Last 7 Days)");
        completionTrendChart.setLegendVisible(true);
        completionTrendChart.setCreateSymbols(true);
        completionTrendChart.setAnimated(true);
        completionTrendChart.getStyleClass().add("chart");
        trendAxis.getStyleClass().add("axis");
        trendValueAxis.getStyleClass().add("axis");
        
        // Make chart responsive
        completionTrendChart.setMinSize(200, 200);
        completionTrendChart.setPrefSize(300, 250);
        completionTrendChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        completionTrendChart.setLegendSide(Side.BOTTOM);
        
        // Make sure charts can resize when window resizes
        categoryChart.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        priorityChart.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        completionTrendChart.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        statusChart.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
    }

    private void layoutDashboard() {
        // Create header with title
        Label dashboardHeader = new Label("Task Dashboard");
        dashboardHeader.setFont(Font.font("System", FontWeight.BOLD, 22));
        dashboardHeader.setPadding(new Insets(0, 0, 10, 0));
        
        // Stats Section - Create a card for statistics
        HBox statsBox = new HBox(30);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.getChildren().addAll(totalTasksLabel, completedTasksLabel, pendingTasksLabel, overdueTasksLabel);
        statsBox.setPadding(new Insets(15));
        statsBox.getStyleClass().add("stats-box");
        statsBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statsBox, Priority.ALWAYS);

        // Add styling to stat labels
        totalTasksLabel.getStyleClass().add("stat-total");
        completedTasksLabel.getStyleClass().add("stat-completed");
        pendingTasksLabel.getStyleClass().add("stat-pending");
        overdueTasksLabel.getStyleClass().add("stat-overdue");
        
        // Create a card for the statistics
        StackPane statsCard = createCard(statsBox, "Task Statistics Overview");

        // Top row of charts - restore GridPane layout but keep it responsive
        GridPane chartsGrid = new GridPane();
        chartsGrid.setHgap(20);
        chartsGrid.setVgap(20);
        chartsGrid.setPadding(new Insets(0));
        chartsGrid.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(chartsGrid, Priority.ALWAYS);
        
        // Status chart in a card
        StackPane statusChartCard = createCard(statusChart, null);
        GridPane.setConstraints(statusChartCard, 0, 0);
        GridPane.setHgrow(statusChartCard, Priority.ALWAYS);
        GridPane.setVgrow(statusChartCard, Priority.ALWAYS);
        
        // Category chart in a card
        StackPane categoryChartCard = createCard(categoryChart, null);
        GridPane.setConstraints(categoryChartCard, 1, 0);
        GridPane.setHgrow(categoryChartCard, Priority.ALWAYS);
        GridPane.setVgrow(categoryChartCard, Priority.ALWAYS);
        
        // Priority chart in a card
        StackPane priorityChartCard = createCard(priorityChart, null);
        GridPane.setConstraints(priorityChartCard, 0, 1);
        GridPane.setHgrow(priorityChartCard, Priority.ALWAYS);
        GridPane.setVgrow(priorityChartCard, Priority.ALWAYS);
        
        // Completion trend chart in a card
        StackPane trendChartCard = createCard(completionTrendChart, null);
        GridPane.setConstraints(trendChartCard, 1, 1);
        GridPane.setHgrow(trendChartCard, Priority.ALWAYS);
        GridPane.setVgrow(trendChartCard, Priority.ALWAYS);
        
        // Add all charts to the grid
        chartsGrid.getChildren().addAll(statusChartCard, categoryChartCard, priorityChartCard, trendChartCard);
        
        // Set column constraints for even sizing
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        chartsGrid.getColumnConstraints().addAll(col1, col2);
        
        // Set row constraints for even sizing
        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(50);
        chartsGrid.getRowConstraints().addAll(row1, row2);

        // Add everything to a container VBox
        VBox contentBox = new VBox(15);
        contentBox.getStyleClass().add("dashboard-container");
        contentBox.getChildren().addAll(dashboardHeader, statsCard, chartsGrid);
        
        // Add all components to a scroll pane so they're always accessible
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("dashboard-scroll");
        
        // Hide scrollbars but keep scrolling functionality
        scrollPane.getStyleClass().add("invisible-scrollbar");
        
        getChildren().add(scrollPane);
        
        // Make the dashboard take available space
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
    }

    private void setupAutoRefresh() {
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5), event -> updateData())
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    public void updateData() {
        List<Task> allTasks = taskManager.getAllTasks();
        List<Task> completedTasks = taskManager.getCompletedTasks();
        List<Task> pendingTasks = allTasks.stream()
            .filter(task -> !task.isCompleted())
            .collect(Collectors.toList());
        List<Task> overdueTasks = pendingTasks.stream()
            .filter(task -> task.getDueDate().isBefore(LocalDateTime.now()))
            .collect(Collectors.toList());

        // Update Stats Labels
        totalTasksLabel.setText("📊 Total Tasks: " + allTasks.size());
        completedTasksLabel.setText("✅ Completed: " + completedTasks.size());
        pendingTasksLabel.setText("⏳ Pending: " + pendingTasks.size());
        overdueTasksLabel.setText("⚠️ Overdue: " + overdueTasks.size());

        // Update Status Chart
        ObservableList<PieChart.Data> statusData = FXCollections.observableArrayList();
        if (completedTasks.size() > 0) {
            statusData.add(new PieChart.Data("Completed", completedTasks.size()));
        }
        if (pendingTasks.size() > 0) {
            statusData.add(new PieChart.Data("Pending", pendingTasks.size()));
        }
        if (overdueTasks.size() > 0) {
            statusData.add(new PieChart.Data("Overdue", overdueTasks.size()));
        }
        statusChart.setData(statusData);
        
        // Update Category Chart
        Map<String, Long> categoryCounts = allTasks.stream()
            .collect(Collectors.groupingBy(Task::getCategory, Collectors.counting()));
        ObservableList<XYChart.Data<String, Number>> categoryData = FXCollections.observableArrayList();
        categoryCounts.forEach((category, count) -> 
            categoryData.add(new XYChart.Data<>(category, count)));
        categoryChart.getData().clear();
        XYChart.Series<String, Number> categorySeries = new XYChart.Series<>("Categories", categoryData);
        categoryChart.getData().add(categorySeries);
        
        // Update Priority Chart
        Map<Integer, Long> priorityCounts = allTasks.stream()
            .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
        ObservableList<XYChart.Data<String, Number>> priorityData = FXCollections.observableArrayList();
        
        // Ensure all priorities from 1-5 are shown even if count is 0
        for (int i = 1; i <= 5; i++) {
            Long count = priorityCounts.getOrDefault(i, 0L);
            priorityData.add(new XYChart.Data<>(String.valueOf(i), count));
        }
        
        priorityChart.getData().clear();
        XYChart.Series<String, Number> prioritySeries = new XYChart.Series<>("Priorities", priorityData);
        priorityChart.getData().add(prioritySeries);

        // Update Completion Trend Chart
        LocalDate today = LocalDate.now();
        ObservableList<XYChart.Data<String, Number>> completionData = FXCollections.observableArrayList();
        
        // Ensure all 7 days are shown with data
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String formattedDate = date.format(DateTimeFormatter.ofPattern("MMM dd"));
            
            long completedCount = completedTasks.stream()
                .filter(task -> task.getDueDate().toLocalDate().equals(date))
                .count();
            
            completionData.add(new XYChart.Data<>(formattedDate, completedCount));
        }
        
        completionTrendChart.getData().clear();
        XYChart.Series<String, Number> trendSeries = new XYChart.Series<>("Completed Tasks", completionData);
        completionTrendChart.getData().add(trendSeries);
        
        // Schedule styling to occur once rendering is complete
        Platform.runLater(() -> {
            // Apply styling to series
            safelyApplyStyle(trendSeries, "chart-series-trend");
            safelyApplyStyle(categorySeries, "chart-series-category");
            safelyApplyStyle(prioritySeries, "chart-series-priority");
            
            // Apply styling to data points
            safelyApplyStyleToDataPoints(categorySeries, "category-data-item");
            safelyApplyStyleToDataPoints(prioritySeries, "priority-data-item");
            safelyApplyStyleToDataPoints(trendSeries, "trend-data-item");
        });
    }

    private String getThemeMode() {
        // Try to detect theme from the scene's stylesheet
        Scene scene = getScene();
        if (scene != null && scene.getStylesheets() != null) {
            for (String sheet : scene.getStylesheets()) {
                if (sheet.toLowerCase().contains("dark")) return "dark";
                if (sheet.toLowerCase().contains("light")) return "light";
            }
        }
        // Default to light
        return "light";
    }

    public void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    // Helper method to safely apply CSS to chart series
    private void safelyApplyStyle(XYChart.Series<?, ?> series, String styleClass) {
        if (series != null && series.getNode() != null) {
            Platform.runLater(() -> {
                try {
                    if (!series.getNode().getStyleClass().contains(styleClass)) {
                        series.getNode().getStyleClass().add(styleClass);
                    }
                } catch (Exception e) {
                    // Safely ignore styling exceptions
                }
            });
        }
    }
    
    // Helper method to safely apply CSS to chart data points
    private <X, Y> void safelyApplyStyleToDataPoints(XYChart.Series<X, Y> series, String styleClass) {
        if (series != null && series.getData() != null) {
            for (XYChart.Data<X, Y> data : series.getData()) {
                Platform.runLater(() -> {
                    try {
                        if (data.getNode() != null && !data.getNode().getStyleClass().contains(styleClass)) {
                            data.getNode().getStyleClass().add(styleClass);
                        }
                    } catch (Exception e) {
                        // Safely ignore styling exceptions
                    }
                });
            }
        }
    }
} 