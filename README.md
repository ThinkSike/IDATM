Here’s a concise summary of MainUI.java:

Purpose: Main JavaFX UI for a desktop task manager app.
Tabs: Tasks (list & filter), Calendar (month view), Dashboard (stats).
Theme: Light/dark toggle, responsive design.
Task Management: Add, edit, delete, filter (status, priority, category), import/export (JSON).
Reminders: Alerts for tasks due soon, with suppression option.
UI: Custom card-style ListView cells, smooth animations, adaptive tab sizing.
Persistence: Uses a TaskManager service (with SQLite DB) for all task operations.
Other: Plays a sound for reminders, saves suppressed reminders to a file.
