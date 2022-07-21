package nl.dylanvdbrink.flixtraktor.service.sync;

import com.google.gson.Gson;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncMovie;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.service.scraper.NetflixScrapeException;
import nl.dylanvdbrink.flixtraktor.pojo.NetflixTitle;
import nl.dylanvdbrink.flixtraktor.pojo.StoredData;
import nl.dylanvdbrink.flixtraktor.service.notify.NotificationException;
import nl.dylanvdbrink.flixtraktor.service.notify.NotificationService;
import nl.dylanvdbrink.flixtraktor.service.notify.NotificationServiceFactory;
import nl.dylanvdbrink.flixtraktor.service.scraper.NetflixViewingActivityService;
import nl.dylanvdbrink.flixtraktor.service.storage.StorageService;
import nl.dylanvdbrink.flixtraktor.service.trakt.TraktClient;
import org.javatuples.Triplet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

@CommonsLog
@Component
public class ScheduledSyncService {
    private final NetflixViewingActivityService netflixViewingActivityService;
    private final StorageService storageService;
    private final TraktClient traktClient;
    private final Optional<NotificationService> notificationService;

    private final int pageSize;
    private final boolean syncAllOnInit;

    public ScheduledSyncService(NetflixViewingActivityService netflixViewingActivityService,
                                StorageService storageService,
                                TraktClient traktClient,
                                NotificationServiceFactory notificationServiceFactory,
                                @Value("${sync.sync-all-on-init:false}") boolean syncAllOnInit,
                                @Value("${sync.page-size:100}") int pageSize) {
        this.netflixViewingActivityService = netflixViewingActivityService;
        this.storageService = storageService;
        this.traktClient = traktClient;
        this.notificationService = notificationServiceFactory.get();
        this.pageSize = pageSize;
        this.syncAllOnInit = syncAllOnInit;
    }

    @Scheduled(cron = "${sync.cron}")
    public void syncViewingActivity() throws InterruptedException, IOException {
        StoredData storedData;
        try {
            storedData = storageService.getStoredData();
        } catch (IOException e) {
            log.error("Could not get stored data", e);
            return;
        }

        if (storedData.isSyncRunning()) {
            log.warn("Previous sync seems to be running still. If this is incorrect, change the syncRunning property in storage.");
            return;
        }

        log.info("Starting sync");
        storedData.setSyncRunning(true);
        storageService.setStoredData(storedData);

        try {
            int lastSyncedHash = storedData.getLastSyncedHash();
            if (lastSyncedHash < 1 && syncAllOnInit) {
                // If there hasn't been a sync yet and syncAllOnInit has been configured. Sync everything.
                log.info("Retrieving complete viewing activity, this might take a while...");
                List<NetflixTitle> titles = netflixViewingActivityService.getViewingActivity(Integer.MAX_VALUE);
                log.info("Done retrieving complete viewing activity");
                traktSync(titles, storedData);
            } else {
                // Either there has already been synced or syncAllOnInit is disabled.
                // Only sync items until we either find the last synced item or until we reach the pageSize
                List<NetflixTitle> titles = netflixViewingActivityService.getViewingActivity(pageSize);

                // Only sync every title newer than the last synced hash
                int lastSyncedIndex = -1;
                for (NetflixTitle title : titles) {
                    if (title.hashCode() == lastSyncedHash) {
                        lastSyncedIndex = titles.indexOf(title);
                    }
                }

                if (lastSyncedIndex == -1) {
                    log.warn("Could not find the last synced title in the retrieved activity. This could mean that the sync is missing titles. " +
                            "Make sure the pageSize and sync cron properties are adjusted accordingly to sync all your history.");
                    titles = titles.subList(0, pageSize);
                } else if (lastSyncedIndex == 0) {
                    log.info("The last synced title was the first in the retrieved activity. Nothing to sync.");
                    return;
                } else {
                    titles = titles.subList(0, lastSyncedIndex - 1);
                }

                traktSync(titles, storedData);
            }
        } catch (NetflixScrapeException | IOException e) {
            log.error("Could not sync viewing history", e);
        } finally {
            try {
                storedData.setSyncRunning(false);
                storageService.setStoredData(storedData);
            } catch (IOException e) {
                log.error("Could not save stored data: " + new Gson().toJson(storedData), e);
            }
        }
    }

    private void traktSync(List<NetflixTitle> titles, StoredData storedData) throws NetflixScrapeException, IOException, InterruptedException {
        log.info(MessageFormat.format("Syncing {0} titles with Trakt", titles.size()));
        Triplet<List<SyncEpisode>, List<SyncMovie>, List<String>> results = traktClient.addToCollection(titles);

        int lastSyncedHash = 0;
        if (!titles.isEmpty()) {
            NetflixTitle lastTitle = titles.get(0);
            lastSyncedHash = lastTitle.hashCode();
        }
        log.debug("Setting last synced hash to " + lastSyncedHash);
        storedData.setLastSyncedHash(lastSyncedHash);

        List<String> errors = results.getValue2();
        if (notificationService.isPresent() && !errors.isEmpty()) {
            try {
                StringBuilder sb = new StringBuilder("The following titles were not synced to Trakt:");
                for (String error : errors) {
                    sb.append("\n- ").append(error);
                }
                log.debug("Sending notification");
                notificationService.get().notify(sb.toString());
            } catch (NotificationException e) {
                log.error("Could not send notification", e);
            }
        } else if (notificationService.isEmpty()) {
            log.debug("Notifications are either disabled or the specified notification type is unsupported.");
        } else {
            log.debug("There were no errors, not sending a notification");
        }

        storageService.setStoredData(storedData);
        log.info("Sync is done");
    }
}
