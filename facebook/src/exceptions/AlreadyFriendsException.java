package exceptions;

public class AlreadyFriendsException extends RuntimeException {
    public AlreadyFriendsException(String userA, String userB) {
        super(userA + " and " + userB + " are already friends");
    }
}
