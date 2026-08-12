package state;

import exceptions.InvalidFriendRequestTransitionException;
import model.FriendRequest;
import model.FriendRequestStatus;

/**
 * One FriendRequest's own lifecycle, held per-instance on the request
 * itself -- same shape as todo-list/'s per-Task TaskState (many
 * independent requests, each with its own state), not the ATM/CrickInfo
 * style of one shared state on a service.
 */
public interface FriendRequestState {
    FriendRequestStatus getStatus();

    default FriendRequestState accept(FriendRequest request) {
        throw new InvalidFriendRequestTransitionException(getStatus(), "accept");
    }

    default FriendRequestState reject(FriendRequest request) {
        throw new InvalidFriendRequestTransitionException(getStatus(), "reject");
    }
}
