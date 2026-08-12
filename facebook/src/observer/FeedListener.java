package observer;

public interface FeedListener {
    void onNewPost(String authorId, String postId, String content);

    void onFriendRequestAccepted(String fromUserId, String toUserId);
}
