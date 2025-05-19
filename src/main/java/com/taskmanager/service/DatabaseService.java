package com.taskmanager.service;

import com.taskmanager.model.Task;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final String DB_URL = "jdbc:sqlite:tasks.db";
    private static DatabaseService instance;
    private Connection connection;

    private DatabaseService() {
        initializeDatabase();
    }

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                category TEXT NOT NULL,
                priority INTEGER NOT NULL,
                due_date TEXT NOT NULL,
                completed BOOLEAN NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables", e);
        }
    }

    public void addTask(Task task) {
        String sql = """
            INSERT INTO tasks (title, description, category, priority, due_date, completed, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setString(3, task.getCategory());
            pstmt.setInt(4, task.getPriority());
            pstmt.setString(5, task.getDueDate().toString());
            pstmt.setBoolean(6, task.isCompleted());
            pstmt.setString(7, LocalDateTime.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add task", e);
        }
    }

    public void updateTask(Task task) {
        String sql = """
            UPDATE tasks 
            SET title = ?, description = ?, category = ?, priority = ?, 
                due_date = ?, completed = ?
            WHERE id = ?
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setString(3, task.getCategory());
            pstmt.setInt(4, task.getPriority());
            pstmt.setString(5, task.getDueDate().toString());
            pstmt.setBoolean(6, task.isCompleted());
            pstmt.setLong(7, task.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update task", e);
        }
    }

    public void deleteTask(long taskId) {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, taskId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete task", e);
        }
    }

    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY due_date";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get tasks", e);
        }
        return tasks;
    }

    public List<Task> getTasksByDateRange(LocalDateTime start, LocalDateTime end) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE due_date BETWEEN ? AND ? ORDER BY due_date";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get tasks by date range", e);
        }
        return tasks;
    }

    public List<Task> getTasksByCategory(String category) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE category = ? ORDER BY due_date";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get tasks by category", e);
        }
        return tasks;
    }

    public List<Task> getTasksByPriority(int priority) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE priority = ? ORDER BY due_date";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, priority);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get tasks by priority", e);
        }
        return tasks;
    }

    public List<Task> getCompletedTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE completed = 1 ORDER BY due_date";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get completed tasks", e);
        }
        return tasks;
    }

    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setCategory(rs.getString("category"));
        task.setPriority(rs.getInt("priority"));
        task.setDueDate(LocalDateTime.parse(rs.getString("due_date")));
        task.setCompleted(rs.getBoolean("completed"));
        return task;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close database connection", e);
        }
    }
} 