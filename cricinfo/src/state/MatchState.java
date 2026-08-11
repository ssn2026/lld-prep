package state;

import exceptions.IllegalMatchOperationException;
import exceptions.MatchNotInProgressException;
import model.MatchStatus;

/**
 * Match-phase lifecycle, held once on CricInfoService (there's one match
 * per service instance -- same shape as the ATM's AtmState, which the
 * per-Task state in todo-list/ deliberately does NOT follow, since a
 * todo list has many independent per-entity lifecycles instead of one
 * shared machine).
 */
public interface MatchState {
    MatchStatus getStatus();

    default MatchState startFirstInnings() {
        throw new IllegalMatchOperationException("Cannot start the match from status " + getStatus());
    }

    default void requireInningsInProgress() {
        throw new MatchNotInProgressException(getStatus());
    }

    default MatchState startSecondInnings() {
        throw new IllegalMatchOperationException("Cannot start the second innings from status " + getStatus());
    }

    default MatchState endInnings() {
        throw new IllegalMatchOperationException("Cannot end an innings from status " + getStatus());
    }
}
