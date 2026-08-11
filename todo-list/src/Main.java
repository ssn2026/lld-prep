import exceptions.InvalidTaskTransitionException;
import exceptions.TaskNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.Task;
import model.TaskPriority;
import observer.ConsoleTaskListener;
import services.TodoListService;
import strategy.CreatedOrderSortStrategy;
import strategy.DueDateSortStrategy;
import strategy.PrioritySortStrategy;
import strategy.TaskSortStrategy;

/**
 * Drives TodoListService from a plain-text command script so the design can
 * be exercised end-to-end without a UI.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        TodoListService service = new TodoListService();
        service.addListener(new ConsoleTaskListener());

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String result = execute(service, line);
            log(output, "> " + line);
            log(output, result);
        }

        if (args.length >= 2) {
            Files.writeString(Path.of(args[1]), output.toString());
        }
    }

    private static void log(StringBuilder output, String line) {
        System.out.println(line);
        output.append(line).append(System.lineSeparator());
    }

    private static String execute(TodoListService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "ADD" -> {
                    String title = parts[1];
                    TaskPriority priority = TaskPriority.valueOf(parts[2]);
                    int dueInDays = Integer.parseInt(parts[3]);
                    String taskId = service.addTask(title, title, priority, dueInDays);
                    yield "OK added " + taskId + " (" + title + ", " + priority + ", due in " + dueInDays + "d)";
                }
                case "START" -> {
                    service.startTask(parts[1]);
                    yield "OK " + parts[1] + " -> " + service.getTask(parts[1]).getStatus();
                }
                case "COMPLETE" -> {
                    service.completeTask(parts[1]);
                    yield "OK " + parts[1] + " -> " + service.getTask(parts[1]).getStatus();
                }
                case "REOPEN" -> {
                    service.reopenTask(parts[1]);
                    yield "OK " + parts[1] + " -> " + service.getTask(parts[1]).getStatus();
                }
                case "ARCHIVE" -> {
                    service.archiveTask(parts[1]);
                    yield "OK " + parts[1] + " -> " + service.getTask(parts[1]).getStatus();
                }
                case "DELETE" -> {
                    service.deleteTask(parts[1]);
                    yield "OK deleted " + parts[1];
                }
                case "LIST" -> {
                    TaskSortStrategy strategy = switch (parts[1]) {
                        case "DUE" -> new DueDateSortStrategy();
                        case "PRIORITY" -> new PrioritySortStrategy();
                        default -> new CreatedOrderSortStrategy();
                    };
                    List<Task> tasks = service.listTasks(strategy);
                    yield "LIST (" + parts[1] + ")\n" + formatTasks(tasks);
                }
                default -> "ERROR unknown command: " + command;
            };
        } catch (TaskNotFoundException | InvalidTaskTransitionException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String formatTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return "  (no tasks)";
        }
        StringBuilder sb = new StringBuilder();
        for (Task task : tasks) {
            sb.append("  ").append(task.getId()).append(" [").append(task.getStatus()).append("] ")
                    .append(task.getTitle()).append(" priority=").append(task.getPriority())
                    .append(" dueInDays=").append(task.getDueInDays()).append('\n');
        }
        return sb.toString().stripTrailing();
    }
}
