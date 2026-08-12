package repository;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class FriendshipRepository {
    private final Map<String, Set<String>> friendsByUser = new LinkedHashMap<>();

    public void addFriendship(String userA, String userB) {
        friendsByUser.computeIfAbsent(userA, id -> new HashSet<>()).add(userB);
        friendsByUser.computeIfAbsent(userB, id -> new HashSet<>()).add(userA);
    }

    public boolean areFriends(String userA, String userB) {
        Set<String> friends = friendsByUser.get(userA);
        return friends != null && friends.contains(userB);
    }

    public Set<String> getFriendsOf(String userId) {
        return friendsByUser.getOrDefault(userId, Set.of());
    }
}
