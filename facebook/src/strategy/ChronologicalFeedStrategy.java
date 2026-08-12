package strategy;

import java.util.Comparator;
import java.util.List;
import model.Post;

public class ChronologicalFeedStrategy implements FeedRankingStrategy {
    @Override
    public List<Post> rank(List<Post> posts) {
        return posts.stream()
                .sorted(Comparator.comparingInt(Post::getSequence).reversed())
                .toList();
    }
}
