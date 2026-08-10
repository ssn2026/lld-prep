package repository;

import exceptions.UserNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import model.Calendar;

public class CalendarRepository {

    private final Map<String, Calendar> calendarsByOwnerId = new LinkedHashMap<>();

    public void save(Calendar calendar) {
        calendarsByOwnerId.put(calendar.getOwnerId(), calendar);
    }

    /** Every registered user has exactly one calendar, so a miss here means an unknown user. */
    public Calendar findByOwnerId(String ownerId) {
        Calendar calendar = calendarsByOwnerId.get(ownerId);
        if (calendar == null) {
            throw new UserNotFoundException("No user (or calendar) with id " + ownerId);
        }
        return calendar;
    }

    public boolean exists(String ownerId) {
        return calendarsByOwnerId.containsKey(ownerId);
    }
}
