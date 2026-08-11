package factory;

import command.BallCommand;
import command.NoBallBallCommand;
import command.RunsBallCommand;
import command.WicketBallCommand;
import command.WideBallCommand;

public class BallCommandFactory {
    public static BallCommand create(String type, int runs) {
        return switch (type) {
            case "RUN" -> new RunsBallCommand(runs);
            case "WICKET" -> new WicketBallCommand();
            case "WIDE" -> new WideBallCommand();
            case "NOBALL" -> new NoBallBallCommand();
            default -> throw new IllegalArgumentException("Unknown ball type: " + type);
        };
    }
}
