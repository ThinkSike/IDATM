package com.taskmanager.service;

import com.taskmanager.model.Task;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TaskManager {
    private final DatabaseService dbService;

    public TaskManager() {
        this.dbService = DatabaseService.getInstance();
    }

    public void addTask(Task task) {
        dbService.addTask(task);
    }

    public void updateTask(Task task) {
        dbService.updateTask(task);
    }

    public void deleteTask(long taskId) {
        dbService.deleteTask(taskId);
    }

    public List<Task> getAllTasks() {
        return dbService.getAllTasks();
    }

    public List<Task> getTasksByDateRange(LocalDateTime start, LocalDateTime end) {
        return dbService.getTasksByDateRange(start, end);
    }

    public List<Task> getTasksByCategory(String category) {
        return dbService.getTasksByCategory(category);
    }

    public List<Task> getTasksByPriority(int priority) {
        return dbService.getTasksByPriority(priority);
    }

    public List<Task> getCompletedTasks() {
        return dbService.getCompletedTasks();
    }

    public List<Task> getTasksByPriority() {
        return getAllTasks().stream()
            .sorted((t1, t2) -> Integer.compare(t1.getPriority(), t2.getPriority()))
            .collect(Collectors.toList());
        }

    public void close() {
        dbService.close();
    }
} 