package model;

public class Post {
    private final String id;
    private final String authorId;
    private final String content;
    private final int sequence;

    public Post(String id, String authorId, String content, int sequence) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.sequence = sequence;
    }

    public String getId() {
        return id;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public int getSequence() {
        return sequence;
    }
}
