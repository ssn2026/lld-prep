package repository;

import exceptions.UserNotFoundException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

public class UserRepository {

    private final Map<String, User> usersById = new LinkedHashMap<>();

    public User save(User user) {
        usersById.put(user.getId(), user);
        return user;
    }

    public User findById(String userId) {
        User user = usersById.get(userId);
        if (user == null) {
            throw new UserNotFoundException("No user with id " + userId);
        }
        return user;
    }

    public Collection<User> findAll() {
        return usersById.values();
    }
}
