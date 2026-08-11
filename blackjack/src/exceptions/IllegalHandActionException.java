package exceptions;

import model.HandStatus;

public class IllegalHandActionException extends RuntimeException {
    public IllegalHandActionException(HandStatus currentStatus, String action) {
        super("Cannot " + action + " a hand that is " + currentStatus);
    }
}
