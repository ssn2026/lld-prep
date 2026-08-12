package exceptions;

import model.FriendRequestStatus;

public class InvalidFriendRequestTransitionException extends RuntimeException {
    public InvalidFriendRequestTransitionException(FriendRequestStatus currentStatus, String action) {
        super("Cannot " + action + " a friend request that is " + currentStatus);
    }
}
