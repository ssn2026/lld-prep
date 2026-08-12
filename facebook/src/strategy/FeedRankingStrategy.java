package strategy;

import java.util.List;
import model.Post;

public interface FeedRankingStrategy {
    List<Post> rank(List<Post> posts);
}
