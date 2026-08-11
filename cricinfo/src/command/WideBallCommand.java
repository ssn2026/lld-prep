package command;

import model.Innings;

/** A wide adds 1 extra run and, unlike a legal delivery, does not count toward the over. */
public class WideBallCommand implements BallCommand {
    @Override
    public void execute(Innings innings) {
        innings.addRuns(1);
    }
}
