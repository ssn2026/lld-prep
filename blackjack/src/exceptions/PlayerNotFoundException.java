package exceptions;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String name) {
        super("No player found with name: " + name);
    }
}
