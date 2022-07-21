package nl.dylanvdbrink.flixtraktor.service.notify;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@CommonsLog
public class NotificationServiceFactory {
    private final boolean notificationEnabled;
    private final String notificationType;
    private final List<NotificationService> notificationServices;

    public NotificationServiceFactory(
            @Value("${notification.enabled}") boolean notificationEnabled,
            @Value("${notification.type}") String notificationType,
            List<NotificationService> notificationServices) {
        this.notificationEnabled = notificationEnabled;
        this.notificationType = notificationType;
        this.notificationServices = notificationServices;
    }

    public Optional<NotificationService> get() {
        Optional<NotificationService> result = Optional.empty();
        if (notificationEnabled) {
            result = notificationServices.stream()
                    .filter(ns -> notificationType.trim().equalsIgnoreCase(ns.getName()))
                    .findFirst();
            if (result.isPresent()) {
                log.debug("Found notificationservice: " + result.get().getClass().getSimpleName());
            } else {
                log.debug("Could not find notificationservice of type: " + notificationType.trim());
            }
        }
        return result;
    }
}
