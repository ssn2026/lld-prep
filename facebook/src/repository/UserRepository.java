package repository;

import exceptions.UserNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

public class UserRepository {
    private final Map<String, User> usersById = new LinkedHashMap<>();

    public void save(User user) {
        usersById.put(user.getId(), user);
    }

    public User findById(String userId) {
        User user = usersById.get(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        return user;
    }
}
