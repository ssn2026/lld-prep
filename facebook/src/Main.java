import exceptions.AlreadyFriendsException;
import exceptions.DuplicatePendingRequestException;
import exceptions.FriendRequestNotFoundException;
import exceptions.InvalidFriendRequestTransitionException;
import exceptions.SelfFriendRequestException;
import exceptions.UserNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.Post;
import observer.ConsoleFeedListener;
import observer.FeedListener;
import services.FacebookService;
import strategy.ChronologicalFeedStrategy;

/**
 * Drives FacebookService from a plain-text command script so the design
 * can be exercised end-to-end without a UI.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        FacebookService service = new FacebookService();
        service.addListener(new ConsoleFeedListener());
        service.addListener(new TranscriptFeedListener(output));

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            log(output, "> " + line);
            String result = execute(service, line);
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

    private static String execute(FacebookService service, String line) {
        String[] parts = line.split("\\s+", 3);
        String command = parts[0];
        try {
            return switch (command) {
                case "USER" -> {
                    String userId = service.registerUser(parts[1]);
                    yield "OK " + userId + " = " + parts[1];
                }
                case "REQUEST" -> {
                    String[] ids = parts[1].split(",");
                    String requestId = service.sendFriendRequest(ids[0], ids[1]);
                    yield "OK " + requestId + " (" + ids[0] + " -> " + ids[1] + ")";
                }
                case "ACCEPT" -> {
                    service.acceptFriendRequest(parts[1]);
                    yield "OK " + parts[1] + " accepted";
                }
                case "REJECT" -> {
                    service.rejectFriendRequest(parts[1]);
                    yield "OK " + parts[1] + " rejected";
                }
                case "POST" -> {
                    String postId = service.createPost(parts[1], parts[2]);
                    yield "OK " + postId + " by " + parts[1];
                }
                case "FEED" -> {
                    List<Post> feed = service.getNewsFeed(parts[1], new ChronologicalFeedStrategy());
                    yield "FEED " + parts[1] + "\n" + formatFeed(feed);
                }
                default -> "ERROR unknown command: " + command;
            };
        } catch (UserNotFoundException | FriendRequestNotFoundException | SelfFriendRequestException
                | AlreadyFriendsException | DuplicatePendingRequestException | InvalidFriendRequestTransitionException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String formatFeed(List<Post> feed) {
        if (feed.isEmpty()) {
            return "  (empty)";
        }
        StringBuilder sb = new StringBuilder();
        for (Post post : feed) {
            sb.append("  ").append(post.getId()).append(" [").append(post.getAuthorId()).append("] ")
                    .append(post.getContent()).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /** Feeds listener events into the same captured transcript as everything else, not just stdout. */
    private static class TranscriptFeedListener implements FeedListener {
        private final StringBuilder output;

        TranscriptFeedListener(StringBuilder output) {
            this.output = output;
        }

        @Override
        public void onNewPost(String authorId, String postId, String content) {
            log(output, "  [listener] " + authorId + " posted " + postId + ": " + content);
        }

        @Override
        public void onFriendRequestAccepted(String fromUserId, String toUserId) {
            log(output, "  [listener] " + fromUserId + " and " + toUserId + " are now friends");
        }
    }
}
