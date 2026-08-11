package exceptions;

import model.MatchStatus;

public class MatchNotInProgressException extends RuntimeException {
    public MatchNotInProgressException(MatchStatus currentStatus) {
        super("Cannot record a ball while match status is " + currentStatus);
    }
}
