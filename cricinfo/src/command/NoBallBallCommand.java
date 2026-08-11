package command;

import model.Innings;

/** A no-ball adds 1 extra run and, like a wide, does not count toward the over. */
public class NoBallBallCommand implements BallCommand {
    @Override
    public void execute(Innings innings) {
        innings.addRuns(1);
    }
}
