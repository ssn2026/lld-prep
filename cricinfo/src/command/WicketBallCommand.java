package command;

import model.Innings;

public class WicketBallCommand implements BallCommand {
    @Override
    public void execute(Innings innings) {
        innings.recordWicket();
        innings.recordLegalBall();
    }
}
