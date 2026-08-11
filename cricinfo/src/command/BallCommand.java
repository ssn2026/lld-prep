package command;

import model.Innings;

/**
 * One delivery's outcome, encapsulated as a Command so CricInfoService can
 * dispatch any ball type through the same execute() call instead of a big
 * if/switch over delivery types.
 */
public interface BallCommand {
    void execute(Innings innings);
}
