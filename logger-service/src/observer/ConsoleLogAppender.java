package observer;

public class ConsoleLogAppender implements LogAppender {
    @Override
    public void append(String formattedLine) {
        System.out.println("[console] " + formattedLine);
    }
}
