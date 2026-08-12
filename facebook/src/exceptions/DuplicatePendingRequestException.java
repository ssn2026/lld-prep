package exceptions;

public class DuplicatePendingRequestException extends RuntimeException {
    public DuplicatePendingRequestException(String userA, String userB) {
        super("A pending friend request already exists between " + userA + " and " + userB);
    }
}
