package state;

import model.FriendRequestStatus;

/** Terminal: accept()/reject() fall through to FriendRequestState's throwing defaults. */
public class RejectedState implements FriendRequestState {
    public static final RejectedState INSTANCE = new RejectedState();

    private RejectedState() {
    }

    @Override
    public FriendRequestStatus getStatus() {
        return FriendRequestStatus.REJECTED;
    }
}
