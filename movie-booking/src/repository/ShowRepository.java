package repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Show;

public class ShowRepository {

    private final Map<String, Show> showsById = new LinkedHashMap<>();

    public void save(Show show) {
        showsById.put(show.getShowId(), show);
    }

    public Show findById(String showId) {
        return showsById.get(showId);
    }

    public List<Show> findByMovieTitleAndCity(String movieTitle, String city) {
        List<Show> matches = new ArrayList<>();
        for (Show show : showsById.values()) {
            boolean titleMatches = show.getMovie().getTitle().equalsIgnoreCase(movieTitle);
            boolean cityMatches = show.getTheater().getCity().equalsIgnoreCase(city);
            if (titleMatches && cityMatches) {
                matches.add(show);
            }
        }
        return matches;
    }
}
