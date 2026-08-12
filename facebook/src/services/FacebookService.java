package services;

import exceptions.AlreadyFriendsException;
import exceptions.DuplicatePendingRequestException;
import exceptions.SelfFriendRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import model.FriendRequest;
import model.Post;
import model.User;
import observer.FeedListener;
import repository.FriendRequestRepository;
import repository.FriendshipRepository;
import repository.PostRepository;
import repository.UserRepository;
import strategy.FeedRankingStrategy;

/**
 * The single public entry point. Owns users, friendships, friend requests
 * (each with its own FriendRequestState), and posts, and notifies
 * FeedListeners on new posts and accepted requests.
 */
public class FacebookService {
    private final UserRepository userRepository = new UserRepository();
    private final FriendshipRepository friendshipRepository = new FriendshipRepository();
    private final FriendRequestRepository requestRepository = new FriendRequestRepository();
    private final PostRepository postRepository = new PostRepository();
    private final List<FeedListener> listeners = new java.util.ArrayList<>();
    private final AtomicInteger userSeq = new AtomicInteger();
    private final AtomicInteger requestSeq = new AtomicInteger();
    private final AtomicInteger postSeq = new AtomicInteger();

    public void addListener(FeedListener listener) {
        listeners.add(listener);
    }

    public String registerUser(String name) {
        String userId = "u" + userSeq.incrementAndGet();
        userRepository.save(new User(userId, name));
        return userId;
    }

    public String sendFriendRequest(String fromUserId, String toUserId) {
        userRepository.findById(fromUserId);
        userRepository.findById(toUserId);
        if (fromUserId.equals(toUserId)) {
            throw new SelfFriendRequestException(fromUserId);
        }
        if (friendshipRepository.areFriends(fromUserId, toUserId)) {
            throw new AlreadyFriendsException(fromUserId, toUserId);
        }
        if (requestRepository.hasPendingBetween(fromUserId, toUserId)) {
            throw new DuplicatePendingRequestException(fromUserId, toUserId);
        }
        String requestId = "r" + requestSeq.incrementAndGet();
        FriendRequest request = new FriendRequest(requestId, fromUserId, toUserId);
        requestRepository.save(request);
        return requestId;
    }

    public void acceptFriendRequest(String requestId) {
        FriendRequest request = requestRepository.findById(requestId);
        request.accept();
        friendshipRepository.addFriendship(request.getFromUserId(), request.getToUserId());
        notifyFriendRequestAccepted(request.getFromUserId(), request.getToUserId());
    }

    public void rejectFriendRequest(String requestId) {
        FriendRequest request = requestRepository.findById(requestId);
        request.reject();
    }

    public String createPost(String userId, String content) {
        userRepository.findById(userId);
        String postId = "p" + postSeq.incrementAndGet();
        Post post = new Post(postId, userId, content, postSeq.get());
        postRepository.save(post);
        notifyNewPost(userId, postId, content);
        return postId;
    }

    public List<Post> getNewsFeed(String userId, FeedRankingStrategy rankingStrategy) {
        userRepository.findById(userId);
        Set<String> authorIds = new HashSet<>(friendshipRepository.getFriendsOf(userId));
        authorIds.add(userId);
        return rankingStrategy.rank(postRepository.findByAuthors(authorIds));
    }

    private void notifyNewPost(String authorId, String postId, String content) {
        for (FeedListener listener : listeners) {
            listener.onNewPost(authorId, postId, content);
        }
    }

    private void notifyFriendRequestAccepted(String fromUserId, String toUserId) {
        for (FeedListener listener : listeners) {
            listener.onFriendRequestAccepted(fromUserId, toUserId);
        }
    }
}
