package nl.dylanvdbrink.flixtraktor.service.notify;

import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class WebhookNotificationService implements NotificationService {
    private static final String NAME = "WEBHOOK";

    private final String webhookUrl;

    public WebhookNotificationService(@Value("${notification.webhook.url}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public void notify(String message) throws NotificationException {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("message", message);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(response.toString()))
                    .uri(new URI(webhookUrl))
                    .header("Content-Type", "application/json")
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException e) {
            throw new NotificationException("Could not send notification. URI was of an incorrect syntax", e);
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            throw new NotificationException("Could not send notification", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotificationException("Could not send notification", e);
        }
    }
}
