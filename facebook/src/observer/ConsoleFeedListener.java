package observer;

public class ConsoleFeedListener implements FeedListener {
    @Override
    public void onNewPost(String authorId, String postId, String content) {
        System.out.println("[listener] " + authorId + " posted " + postId + ": " + content);
    }

    @Override
    public void onFriendRequestAccepted(String fromUserId, String toUserId) {
        System.out.println("[listener] " + fromUserId + " and " + toUserId + " are now friends");
    }
}
