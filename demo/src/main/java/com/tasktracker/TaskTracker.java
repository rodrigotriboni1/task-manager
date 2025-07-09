package com.tasktracker;

import java.time.format.DateTimeFormatter;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class TaskTracker {

    private static final String TASKS_FILE = "tasks.json";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private List<Task> tasks;
    private int nextId;

    public TaskTracker() {
        this.tasks = new ArrayList<>();
        this.nextId = 1;
        loadTasks();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        TaskTracker tracker = new TaskTracker();
        
        try {
            switch (args[0].toLowerCase()) {
                case "add":
                    if (args.length < 2) {
                        System.err.println("Error: Task description is required");
                        System.exit(1);
                    }
                    tracker.addTask(args[1]);
                    break;
                    
                case "update":
                    if (args.length < 3) {
                        System.err.println("Error: Task ID and description are required");
                        System.exit(1);
                    }
                    tracker.updateTask(Integer.parseInt(args[1]), args[2]);
                    break;
                    
                case "delete":
                    if (args.length < 2) {
                        System.err.println("Error: Task ID is required");
                        System.exit(1);
                    }
                    tracker.deleteTask(Integer.parseInt(args[1]));
                    break;
                    
                case "mark-in-progress":
                    if (args.length < 2) {
                        System.err.println("Error: Task ID is required");
                        System.exit(1);
                    }
                    tracker.markInProgress(Integer.parseInt(args[1]));
                    break;
                    
                case "mark-done":
                    if (args.length < 2) {
                        System.err.println("Error: Task ID is required");
                        System.exit(1);
                    }
                    tracker.markDone(Integer.parseInt(args[1]));
                    break;
                    
                case "list":
                    String status = args.length > 1 ? args[1] : null;
                    tracker.listTasks(status);
                    break;
                    
                default:
                    System.err.println("Error: Unknown command '" + args[0] + "'");
                    printUsage();
                    System.exit(1);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid task ID. Please provide a valid number.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Task Tracker CLI");
        System.out.println("Usage:");
        System.out.println("  java TaskTracker add \"Task description\"");
        System.out.println("  java TaskTracker update <id> \"New description\"");
        System.out.println("  java TaskTracker delete <id>");
        System.out.println("  java TaskTracker mark-in-progress <id>");
        System.out.println("  java TaskTracker mark-done <id>");
        System.out.println("  java TaskTracker list [todo|in-progress|done]");
    }
    
    private void addTask(String description) {
        Task task = new Task(nextId++, description);
        tasks.add(task);
        saveTasks();
        System.out.println("Task added successfully (ID: " + task.getId() + ")");
    }
    
    private void updateTask(int id, String description) {
        Task task = findTaskById(id);
        if (task == null) {
            System.err.println("Error: Task with ID " + id + " not found");
            return;
        }
        
        task.setDescription(description);
        task.setUpdatedAt(LocalDateTime.now());
        saveTasks();
        System.out.println("Task updated successfully (ID: " + id + ")");
    }
    
    private void deleteTask(int id) {
        Task task = findTaskById(id);
        if (task == null) {
            System.err.println("Error: Task with ID " + id + " not found");
            return;
        }
        
        tasks.remove(task);
        saveTasks();
        System.out.println("Task deleted successfully (ID: " + id + ")");
    }
    
    private void markInProgress(int id) {
        Task task = findTaskById(id);
        if (task == null) {
            System.err.println("Error: Task with ID " + id + " not found");
            return;
        }
        
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setUpdatedAt(LocalDateTime.now());
        saveTasks();
        System.out.println("Task marked as in progress (ID: " + id + ")");
    }
    
    private void markDone(int id) {
        Task task = findTaskById(id);
        if (task == null) {
            System.err.println("Error: Task with ID " + id + " not found");
            return;
        }
        
        task.setStatus(TaskStatus.DONE);
        task.setUpdatedAt(LocalDateTime.now());
        saveTasks();
        System.out.println("Task marked as done (ID: " + id + ")");
    }
    
    private void listTasks(String statusFilter) {
        List<Task> filteredTasks = tasks;
        
        if (statusFilter != null) {
            TaskStatus status;
            switch (statusFilter.toLowerCase()) {
                case "todo":
                    status = TaskStatus.TODO;
                    break;
                case "in-progress":
                    status = TaskStatus.IN_PROGRESS;
                    break;
                case "done":
                    status = TaskStatus.DONE;
                    break;
                default:
                    System.err.println("Error: Invalid status filter. Use 'todo', 'in-progress', or 'done'");
                    return;
            }
            filteredTasks = tasks.stream()
                    .filter(task -> task.getStatus() == status)
                    .collect(Collectors.toList());
        }
        
        if (filteredTasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }
        
        System.out.println("Tasks:");
        for (Task task : filteredTasks) {
            System.out.println(task.toString());
        }
    }
    
    private Task findTaskById(int id) {
        return tasks.stream()
                .filter(task -> task.getId() == id)
                .findFirst()
                .orElse(null);
    }
    
    private void loadTasks() {
        try {
            if (!Files.exists(Paths.get(TASKS_FILE))) {
                // Create empty file if it doesn't exist
                Files.createFile(Paths.get(TASKS_FILE));
                Files.write(Paths.get(TASKS_FILE), "[]".getBytes());
            }
            
            String content = Files.readString(Paths.get(TASKS_FILE));
            if (content.trim().isEmpty()) {
                content = "[]";
            }
            
            parseTasksFromJson(content);
            
        } catch (IOException e) {
            System.err.println("Error loading tasks: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void saveTasks() {
        try {
            String json = tasksToJson();
            Files.write(Paths.get(TASKS_FILE), json.getBytes());
        } catch (IOException e) {
            System.err.println("Error saving tasks: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void parseTasksFromJson(String json) {
        // Simple JSON parsing without external libraries
        json = json.trim();
        if (json.equals("[]")) {
            return;
        }
        
        // Remove outer brackets
        json = json.substring(1, json.length() - 1);
        
        if (json.trim().isEmpty()) {
            return;
        }
        
        // Split by task objects
        String[] taskStrings = splitJsonObjects(json);
        
        for (String taskString : taskStrings) {
            if (taskString.trim().isEmpty()) continue;
            
            Task task = parseTaskFromJson(taskString);
            if (task != null) {
                tasks.add(task);
                nextId = Math.max(nextId, task.getId() + 1);
            }
        }
    }
    
    private String[] splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = 0;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(json.substring(start, i + 1));
                    start = i + 1;
                    // Skip comma and whitespace
                    while (start < json.length() && (json.charAt(start) == ',' || Character.isWhitespace(json.charAt(start)))) {
                        start++;
                    }
                    i = start - 1;
                }
            }
        }
        
        return objects.toArray(new String[0]);
    }
    
    private Task parseTaskFromJson(String json) {
        try {
            int id = extractIntValue(json, "id");
            String description = extractStringValue(json, "description");
            String statusStr = extractStringValue(json, "status");
            String createdAtStr = extractStringValue(json, "createdAt");
            String updatedAtStr = extractStringValue(json, "updatedAt");
            
            TaskStatus status = TaskStatus.valueOf(statusStr.toUpperCase().replace("-", "_"));
            LocalDateTime createdAt = LocalDateTime.parse(createdAtStr, DATE_FORMATTER);
            LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr, DATE_FORMATTER);
            
            return new Task(id, description, status, createdAt, updatedAt);
        } catch (Exception e) {
            System.err.println("Error parsing task: " + e.getMessage());
            return null;
        }
    }
    
    private String extractStringValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return "";
        
        startIndex += searchKey.length();
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }
        
        if (startIndex >= json.length() || json.charAt(startIndex) != '"') return "";
        
        startIndex++; // Skip opening quote
        int endIndex = startIndex;
        while (endIndex < json.length() && json.charAt(endIndex) != '"') {
            if (json.charAt(endIndex) == '\\') {
                endIndex += 2; // Skip escaped character
            } else {
                endIndex++;
            }
        }
        
        return json.substring(startIndex, endIndex);
    }
    
    private int extractIntValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return 0;
        
        startIndex += searchKey.length();
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }
        
        int endIndex = startIndex;
        while (endIndex < json.length() && Character.isDigit(json.charAt(endIndex))) {
            endIndex++;
        }
        
        return Integer.parseInt(json.substring(startIndex, endIndex));
    }
    
    private String tasksToJson() {
        StringBuilder json = new StringBuilder("[\n");
        
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            json.append("  {\n");
            json.append("    \"id\": ").append(task.getId()).append(",\n");
            json.append("    \"description\": \"").append(escapeJson(task.getDescription())).append("\",\n");
            json.append("    \"status\": \"").append(task.getStatus().toString().toLowerCase().replace("_", "-")).append("\",\n");
            json.append("    \"createdAt\": \"").append(task.getCreatedAt().format(DATE_FORMATTER)).append("\",\n");
            json.append("    \"updatedAt\": \"").append(task.getUpdatedAt().format(DATE_FORMATTER)).append("\"\n");
            json.append("  }");
            
            if (i < tasks.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("]");
        return json.toString();
    }
    
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}