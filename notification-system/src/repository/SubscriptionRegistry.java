package repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import model.ChannelType;
import observer.NotificationChannel;

/** userId -> (channelType -> the observer instance that channel subscribes as). */
public class SubscriptionRegistry {
    private final Map<String, Map<ChannelType, NotificationChannel>> subscriptionsByUser = new LinkedHashMap<>();

    public void subscribe(String userId, ChannelType type, NotificationChannel channel) {
        subscriptionsByUser.computeIfAbsent(userId, id -> new LinkedHashMap<>()).put(type, channel);
    }

    public boolean unsubscribe(String userId, ChannelType type) {
        Map<ChannelType, NotificationChannel> channels = subscriptionsByUser.get(userId);
        return channels != null && channels.remove(type) != null;
    }

    public Collection<NotificationChannel> getChannelsFor(String userId) {
        Map<ChannelType, NotificationChannel> channels = subscriptionsByUser.get(userId);
        return channels == null ? java.util.List.of() : channels.values();
    }
}
