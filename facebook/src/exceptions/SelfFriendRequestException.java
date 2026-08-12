package exceptions;

public class SelfFriendRequestException extends RuntimeException {
    public SelfFriendRequestException(String userId) {
        super("User " + userId + " cannot send a friend request to themselves");
    }
}
