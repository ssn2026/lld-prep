package services;

import decorator.NotificationContent;
import decorator.PlainContent;
import decorator.SignatureDecorator;
import decorator.UrgentPrefixDecorator;
import model.ChannelType;
import model.Notification;
import model.NotificationPriority;
import observer.EmailChannel;
import observer.NotificationChannel;
import observer.PushChannel;
import observer.SmsChannel;
import repository.SubscriptionRegistry;

/**
 * The single public entry point. Owns per-user channel subscriptions
 * (Observer) and decorates each notification's rendered content
 * (Decorator) before fanning it out.
 */
public class NotificationService {
    private final SubscriptionRegistry registry = new SubscriptionRegistry();

    public void subscribe(String userId, ChannelType type) {
        registry.subscribe(userId, type, newChannel(type));
    }

    public boolean unsubscribe(String userId, ChannelType type) {
        return registry.unsubscribe(userId, type);
    }

    public int send(String userId, Notification notification) {
        NotificationContent content = new PlainContent(notification);
        content = new SignatureDecorator(content);
        if (notification.getPriority() == NotificationPriority.HIGH) {
            content = new UrgentPrefixDecorator(content);
        }
        String rendered = content.render();

        int count = 0;
        for (NotificationChannel channel : registry.getChannelsFor(userId)) {
            channel.send(userId, rendered);
            count++;
        }
        return count;
    }

    private NotificationChannel newChannel(ChannelType type) {
        return switch (type) {
            case EMAIL -> new EmailChannel();
            case SMS -> new SmsChannel();
            case PUSH -> new PushChannel();
        };
    }
}
