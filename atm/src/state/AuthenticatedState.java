package state;

import model.AtmStatus;

public final class AuthenticatedState implements AtmState {

    public static final AuthenticatedState INSTANCE = new AuthenticatedState();

    private AuthenticatedState() {
    }

    @Override
    public AtmStatus getStatus() {
        return AtmStatus.AUTHENTICATED;
    }

    @Override
    public void requireAuthenticated() {
    }

    @Override
    public AtmState ejectCard() {
        return IdleState.INSTANCE;
    }
}
