package model;

import state.FriendRequestState;
import state.PendingState;

public class FriendRequest {
    private final String id;
    private final String fromUserId;
    private final String toUserId;
    private FriendRequestState state = PendingState.INSTANCE;

    public FriendRequest(String id, String fromUserId, String toUserId) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
    }

    public String getId() {
        return id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public FriendRequestStatus getStatus() {
        return state.getStatus();
    }

    public void accept() {
        state = state.accept(this);
    }

    public void reject() {
        state = state.reject(this);
    }
}
