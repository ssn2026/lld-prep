package exceptions;

public class FriendRequestNotFoundException extends RuntimeException {
    public FriendRequestNotFoundException(String requestId) {
        super("No friend request found with id: " + requestId);
    }
}
