package command;

import model.Innings;

public class RunsBallCommand implements BallCommand {
    private final int runs;

    public RunsBallCommand(int runs) {
        this.runs = runs;
    }

    @Override
    public void execute(Innings innings) {
        innings.addRuns(runs);
        if (runs % 2 == 1) {
            innings.swapEnds();
        }
        innings.recordLegalBall();
    }
}
