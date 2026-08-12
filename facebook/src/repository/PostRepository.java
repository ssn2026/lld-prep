package repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import model.Post;

public class PostRepository {
    private final List<Post> posts = new ArrayList<>();

    public void save(Post post) {
        posts.add(post);
    }

    public List<Post> findByAuthors(Collection<String> authorIds) {
        return posts.stream().filter(p -> authorIds.contains(p.getAuthorId())).toList();
    }
}
