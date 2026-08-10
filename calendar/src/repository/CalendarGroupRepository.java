package repository;

import composite.CalendarGroup;
import exceptions.CalendarGroupNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CalendarGroupRepository {

    private final Map<String, CalendarGroup> groupsById = new LinkedHashMap<>();

    public void save(CalendarGroup group) {
        groupsById.put(group.getGroupId(), group);
    }

    public boolean exists(String groupId) {
        return groupsById.containsKey(groupId);
    }

    public CalendarGroup findByGroupId(String groupId) {
        CalendarGroup group = groupsById.get(groupId);
        if (group == null) {
            throw new CalendarGroupNotFoundException("No calendar group with id " + groupId);
        }
        return group;
    }
}
