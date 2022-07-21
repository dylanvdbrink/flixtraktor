package nl.dylanvdbrink.flixtraktor.service.notify;

public interface NotificationService {

    String getName();
    public void notify(String message) throws NotificationException;
}
