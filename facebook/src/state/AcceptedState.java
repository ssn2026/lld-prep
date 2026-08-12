package state;

import model.FriendRequestStatus;

/** Terminal: accept()/reject() fall through to FriendRequestState's throwing defaults. */
public class AcceptedState implements FriendRequestState {
    public static final AcceptedState INSTANCE = new AcceptedState();

    private AcceptedState() {
    }

    @Override
    public FriendRequestStatus getStatus() {
        return FriendRequestStatus.ACCEPTED;
    }
}
