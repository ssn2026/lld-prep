package repository;

import exceptions.FriendRequestNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import model.FriendRequest;
import model.FriendRequestStatus;

public class FriendRequestRepository {
    private final Map<String, FriendRequest> requestsById = new LinkedHashMap<>();

    public void save(FriendRequest request) {
        requestsById.put(request.getId(), request);
    }

    public FriendRequest findById(String requestId) {
        FriendRequest request = requestsById.get(requestId);
        if (request == null) {
            throw new FriendRequestNotFoundException(requestId);
        }
        return request;
    }

    public boolean hasPendingBetween(String userA, String userB) {
        return requestsById.values().stream().anyMatch(r ->
                r.getStatus() == FriendRequestStatus.PENDING
                        && ((r.getFromUserId().equals(userA) && r.getToUserId().equals(userB))
                        || (r.getFromUserId().equals(userB) && r.getToUserId().equals(userA))));
    }
}
