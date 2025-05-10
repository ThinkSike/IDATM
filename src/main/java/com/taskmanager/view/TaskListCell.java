package com.taskmanager.view;

import com.taskmanager.model.Task;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import java.time.format.DateTimeFormatter;

public class TaskListCell extends ListCell<Task> {
    private final HBox content;
    private final Label titleLabel;
    private final Label dueDateLabel;
    private final CheckBox completedCheckBox;
    private final DateTimeFormatter dateFormatter;

    public TaskListCell() {
        content = new HBox(10);
        content.setPadding(new Insets(5));
        
        titleLabel = new Label();
        dueDateLabel = new Label();
        completedCheckBox = new CheckBox();
        dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        content.getChildren().addAll(completedCheckBox, titleLabel, dueDateLabel);
    }

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        
        if (empty || task == null) {
            setGraphic(null);
        } else {
            titleLabel.setText(task.getTitle());
            dueDateLabel.setText(task.getDueDate().format(dateFormatter));
            completedCheckBox.setSelected(task.isCompleted());
            
            // Update style based on completion status
            if (task.isCompleted()) {
                titleLabel.setStyle("-fx-text-fill: gray; -fx-strikethrough: true;");
            } else {
                titleLabel.setStyle("");
            }
            
            setGraphic(content);
        }
    }
} 