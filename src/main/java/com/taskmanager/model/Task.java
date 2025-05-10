package com.taskmanager.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Task implements Comparable<Task> {
    private String id;
    private String title;
    private String description;
    private String category;
    private int priority;
    private LocalDateTime dueDate;
    private boolean completed;
    private Set<String> tags;
    private boolean reminderEnabled;

    public Task(String title, String description, String category, int priority, LocalDateTime dueDate) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completed = false;
        this.tags = new HashSet<>();
        this.reminderEnabled = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public Set<String> getTags() { return tags; }
    public void addTag(String tag) { this.tags.add(tag); }
    public void removeTag(String tag) { this.tags.remove(tag); }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public void setReminderEnabled(boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }

    @Override
    public int compareTo(Task other) {
        int priorityCompare = Integer.compare(this.priority, other.priority);
        if (priorityCompare != 0) return priorityCompare;
        return this.dueDate.compareTo(other.dueDate);
    }

    @Override
    public String toString() {
        return title + " (Priority: " + priority + ", Due: " + dueDate + ")";
    }
} 