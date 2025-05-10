package com.taskmanager.view;

import com.taskmanager.service.TaskManager;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.taskmanager.model.Task;

public class CalendarTab extends VBox {
    private final TaskManager taskManager;
    private GridPane calendarGrid;
    private Label monthYearLabel;
    private YearMonth currentMonth;

    public CalendarTab(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.currentMonth = YearMonth.now();
        
        setPadding(new Insets(10));
        setSpacing(10);
        
        initializeComponents();
        updateCalendar();
    }

    private void initializeComponents() {
        // Month navigation
        HBox navigation = new HBox(10);
        Button prevMonth = new Button("←");
        Button nextMonth = new Button("→");
        monthYearLabel = new Label();
        
        prevMonth.setOnAction(event -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });
        
        nextMonth.setOnAction(event -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });
        
        navigation.getChildren().addAll(prevMonth, monthYearLabel, nextMonth);
        
        // Calendar grid
        calendarGrid = new GridPane();
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);
        
        // Add day headers
        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(daysOfWeek[i]);
            calendarGrid.add(dayLabel, i, 0);
        }
        
        getChildren().addAll(navigation, calendarGrid);
    }

    private void updateCalendar() {
        monthYearLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        
        // Clear existing calendar cells
        calendarGrid.getChildren().clear();
        
        // Add day headers back
        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(daysOfWeek[i]);
            calendarGrid.add(dayLabel, i, 0);
        }
        
        // Fill in the days
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
        
        for (int i = 1; i <= currentMonth.lengthOfMonth(); i++) {
            LocalDate date = currentMonth.atDay(i);
            VBox dayCell = createDayCell(date);
            calendarGrid.add(dayCell, dayOfWeek, (i + dayOfWeek - 1) / 7 + 1);
            dayOfWeek = (dayOfWeek + 1) % 7;
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(5);
        cell.setPadding(new Insets(5));
        cell.setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");
        
        Label dateLabel = new Label(String.valueOf(date.getDayOfMonth()));
        cell.getChildren().add(dateLabel);
        
        // Get tasks for this date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<Task> dayTasks = taskManager.getTasksByDueDateRange(startOfDay, endOfDay);
        
        // Add task indicators
        for (Task task : dayTasks) {
            Label taskLabel = new Label("• " + task.getTitle());
            taskLabel.setStyle(task.isCompleted() ? "-fx-text-fill: gray;" : "-fx-text-fill: #2196F3;");
            cell.getChildren().add(taskLabel);
        }
        
        return cell;
    }
} 