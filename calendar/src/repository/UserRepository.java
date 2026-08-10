package repository;

import exceptions.UserNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

public class UserRepository {

    private final Map<String, User> usersById = new LinkedHashMap<>();

    public void save(User user) {
        usersById.put(user.getUserId(), user);
    }

    public User findByUserId(String userId) {
        User user = usersById.get(userId);
        if (user == null) {
            throw new UserNotFoundException("No user with id " + userId);
        }
        return user;
    }
}
