package nl.dylanvdbrink.flixtraktor.service.trakt;

import com.uwetrottmann.trakt5.entities.AccessToken;
import com.uwetrottmann.trakt5.entities.DeviceCode;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.exceptions.TraktException;
import nl.dylanvdbrink.flixtraktor.pojo.StoredAuthData;
import nl.dylanvdbrink.flixtraktor.pojo.StoredData;
import nl.dylanvdbrink.flixtraktor.service.storage.StorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@CommonsLog
public class TraktService {
    private final StorageService storageService;
    private final TraktClient traktClient;

    public TraktService(StorageService storageService, TraktClient traktClient) {
        this.storageService = storageService;
        this.traktClient = traktClient;
    }

    public void ensureAuthorized() throws IOException, TraktException, InterruptedException {
        StoredAuthData storedAuthData = storageService.getStoredData().getStoredAuthData();

        if (storedAuthData.getTraktAccessToken().isEmpty()) {
            log.info("No Trakt access token found, authenticating...");
            DeviceCode deviceCode = traktClient.getDeviceCode();
            log.info("Authorize the application with the following link: " + deviceCode.verification_url + " and code: " + deviceCode.user_code);

            int waitMillis = deviceCode.interval * 1000;
            int maxAttempts = deviceCode.expires_in / deviceCode.interval;

            boolean userAuthorized = false;
            int attempt = 1;
            while (!userAuthorized && attempt < maxAttempts) {
                log.debug("Attempt #" + attempt);
                AccessToken accessToken = traktClient.getToken(deviceCode);
                try {
                    saveAccessToken(accessToken);
                    userAuthorized = true;
                } catch (IOException e) {
                    Thread.sleep(waitMillis);
                    attempt++;
                }
            }
            log.info("Application was authorized!");
        } else {
            log.info("Already have an access code, checking expiration...");
            long exp = storedAuthData.getTraktTokenExpiresAt() * 1000;
            ZonedDateTime nowZdt = ZonedDateTime.now(ZoneId.of("GMT"));
            long now = nowZdt.toInstant().toEpochMilli();
            long daysBeforeExp = exp - 1000 * 60 * 60 * 24 * 2;

            if (log.isDebugEnabled()) {
                ZonedDateTime expZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(exp), ZoneId.of("GMT"));
                ZonedDateTime daysBeforeZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(daysBeforeExp), ZoneId.of("GMT"));

                log.debug("Token expires on: " + expZdt.toString());
                log.debug("Current date is: " + nowZdt.toString());
                log.debug("2 days before expiration is: " + daysBeforeZdt.toString());
            }

            if (now > daysBeforeExp) {
                log.info("Refreshing access token because token will expire in 2 days.");
                AccessToken accessToken = traktClient.refreshAuthorization();
                saveAccessToken(accessToken);
            } else {
                log.info("Access token is still valid.");
            }
        }
    }

    private void saveAccessToken(AccessToken accessToken) throws IOException {
        if (accessToken != null && accessToken.created_at != null && accessToken.expires_in != null) {
            StoredAuthData storedAuthData = new StoredAuthData(accessToken.access_token, accessToken.refresh_token, accessToken.created_at,
                    (long) accessToken.created_at + (long) accessToken.expires_in);
            StoredData storedData = StoredData.getEmptyInstance();
            storedData.setStoredAuthData(storedAuthData);
            storageService.setStoredData(storedData);
        } else {
            throw new IOException("Access token was null");
        }
    }

}
