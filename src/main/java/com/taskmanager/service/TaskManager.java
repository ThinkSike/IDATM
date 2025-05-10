package com.taskmanager.service;

import com.taskmanager.model.Task;
import java.util.*;
import java.time.LocalDateTime;

public class TaskManager {
    // Priority Queue (Min Heap) for task scheduling
    private final PriorityQueue<Task> taskQueue;
    
    // Hash Table for quick task lookup by ID
    private final Map<String, Task> taskMap;
    
    // TreeMap for categorized tasks (B+ Tree implementation)
    private final TreeMap<String, Set<Task>> categoryMap;
    
    // TreeMap for tasks sorted by due date (B+ Tree implementation)
    private final TreeMap<LocalDateTime, Set<Task>> dueDateMap;
    
    // Trie for tag suggestions
    private final Trie tagTrie;
    
    // Set for completed tasks (TreeSet for ordered storage)
    private final TreeSet<Task> completedTasks;
    
    // Map for priority-based task groups (Hash Table)
    private final Map<Integer, Set<Task>> priorityMap;

    public TaskManager() {
        // Initialize PriorityQueue with custom comparator for task priority
        this.taskQueue = new PriorityQueue<>((t1, t2) -> {
            int priorityCompare = Integer.compare(t1.getPriority(), t2.getPriority());
            if (priorityCompare != 0) return priorityCompare;
            return t1.getDueDate().compareTo(t2.getDueDate());
        });
        
        this.taskMap = new HashMap<>();
        this.categoryMap = new TreeMap<>();
        this.dueDateMap = new TreeMap<>();
        this.tagTrie = new Trie();
        this.completedTasks = new TreeSet<>((t1, t2) -> {
            int dateCompare = t1.getDueDate().compareTo(t2.getDueDate());
            if (dateCompare != 0) return dateCompare;
            return t1.getId().compareTo(t2.getId());
        });
        this.priorityMap = new HashMap<>();
    }

    public void addTask(Task task) {
        // Add to priority queue
        taskQueue.offer(task);
        
        // Add to hash table for quick lookup
        taskMap.put(task.getId(), task);
        
        // Add to category map (B+ Tree)
        categoryMap.computeIfAbsent(task.getCategory(), key -> new TreeSet<>()).add(task);
        
        // Add to due date map (B+ Tree)
        dueDateMap.computeIfAbsent(task.getDueDate(), key -> new TreeSet<>()).add(task);
        
        // Add to priority map
        priorityMap.computeIfAbsent(task.getPriority(), key -> new TreeSet<>()).add(task);
        
        // Add tags to trie for suggestions
        for (String tag : task.getTags()) {
            tagTrie.insert(tag.toLowerCase());
        }
    }

    public Task getTask(String id) {
        return taskMap.get(id);
    }

    public List<Task> getTasksByCategory(String category) {
        Set<Task> tasks = categoryMap.get(category);
        return tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
    }

    public List<Task> getTasksByPriority(int priority) {
        Set<Task> tasks = priorityMap.get(priority);
        return tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
    }

    public List<Task> getTasksByDueDateRange(LocalDateTime start, LocalDateTime end) {
        List<Task> tasks = new ArrayList<>();
        dueDateMap.subMap(start, true, end, true)
                 .values()
                 .forEach(tasks::addAll);
        return tasks;
    }

    public List<String> getTagSuggestions(String prefix) {
        return tagTrie.getSuggestions(prefix.toLowerCase());
    }

    public Task getNextTask() {
        return taskQueue.peek();
    }

    public void completeTask(String id) {
        Task task = taskMap.get(id);
        if (task != null) {
            task.setCompleted(true);
            taskQueue.remove(task);
            completedTasks.add(task);
            
            // Remove from active task collections
            categoryMap.get(task.getCategory()).remove(task);
            dueDateMap.get(task.getDueDate()).remove(task);
            priorityMap.get(task.getPriority()).remove(task);
        }
    }

    public List<Task> getTasksByPriority() {
        List<Task> tasks = new ArrayList<>(taskQueue);
        Collections.sort(tasks);
        return tasks;
    }

    public List<Task> getTasksByDueDate() {
        List<Task> tasks = new ArrayList<>();
        dueDateMap.values().forEach(tasks::addAll);
        return tasks;
    }

    public List<Task> getCompletedTasks() {
        return new ArrayList<>(completedTasks);
    }

    // Trie implementation for tag suggestions
    private static class Trie {
        private static class TrieNode {
            Map<Character, TrieNode> children = new HashMap<>();
            Set<String> words = new HashSet<>();
        }

        private final TrieNode root;

        public Trie() {
            root = new TrieNode();
        }

        public void insert(String word) {
            TrieNode current = root;
            for (char c : word.toCharArray()) {
                current.children.putIfAbsent(c, new TrieNode());
                current = current.children.get(c);
                current.words.add(word);
            }
        }

        public List<String> getSuggestions(String prefix) {
            List<String> suggestions = new ArrayList<>();
            TrieNode node = findNode(prefix);
            if (node != null) {
                suggestions.addAll(node.words);
            }
            return suggestions;
        }

        private TrieNode findNode(String prefix) {
            TrieNode current = root;
            for (char c : prefix.toCharArray()) {
                if (!current.children.containsKey(c)) {
                    return null;
                }
                current = current.children.get(c);
            }
            return current;
        }
    }
} 