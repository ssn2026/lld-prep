package state;

import model.FriendRequest;
import model.FriendRequestStatus;

public class PendingState implements FriendRequestState {
    public static final PendingState INSTANCE = new PendingState();

    private PendingState() {
    }

    @Override
    public FriendRequestStatus getStatus() {
        return FriendRequestStatus.PENDING;
    }

    @Override
    public FriendRequestState accept(FriendRequest request) {
        return AcceptedState.INSTANCE;
    }

    @Override
    public FriendRequestState reject(FriendRequest request) {
        return RejectedState.INSTANCE;
    }
}
