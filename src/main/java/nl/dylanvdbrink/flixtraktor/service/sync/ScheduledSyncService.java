package nl.dylanvdbrink.flixtraktor.service.sync;

import com.google.gson.Gson;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.exceptions.NetflixScrapeException;
import nl.dylanvdbrink.flixtraktor.pojo.NetflixTitle;
import nl.dylanvdbrink.flixtraktor.pojo.StoredData;
import nl.dylanvdbrink.flixtraktor.service.scraper.NetflixViewingActivityService;
import nl.dylanvdbrink.flixtraktor.service.storage.StorageService;
import nl.dylanvdbrink.flixtraktor.service.trakt.TraktClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@CommonsLog
@Component
public class ScheduledSyncService {
    private final NetflixViewingActivityService netflixViewingActivityService;
    private final StorageService storageService;
    private final TraktClient traktClient;

    private final int pageSize;
    private final boolean syncAllOnInit;

    public ScheduledSyncService(NetflixViewingActivityService netflixViewingActivityService,
                                StorageService storageService,
                                TraktClient traktClient,
                                @Value("${sync.sync-all-on-init:false}") boolean syncAllOnInit,
                                @Value("${sync.page-size:50}") int pageSize) {
        this.netflixViewingActivityService = netflixViewingActivityService;
        this.storageService = storageService;
        this.traktClient = traktClient;
        this.pageSize = pageSize;
        this.syncAllOnInit = syncAllOnInit;
    }

    @Scheduled(cron = "${sync.cron}")
    public void syncViewingActivity() throws InterruptedException {
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

        try {
            int lastSyncedHash = storedData.getLastSyncedHash();
            if (lastSyncedHash < 1 && syncAllOnInit) {
                // No last synced hash, retrieve everything
                log.info("Syncing complete viewing activity, this might take a while...");
                traktSync(Integer.MAX_VALUE, storedData);
                log.info("Done syncing complete viewing activity");
                return;
            }

            traktSync(pageSize, storedData);
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

    private void traktSync(int pageSize, StoredData storedData) throws NetflixScrapeException, IOException, InterruptedException {
        log.info("Starting sync");
        storedData.setSyncRunning(true);
        storageService.setStoredData(storedData);

        List<NetflixTitle> titles = netflixViewingActivityService.getViewingActivity(pageSize);
        log.info("Syncing with Trakt");
        traktClient.addToCollection(titles);

        int lastSyncedHash = 0;
        if (!titles.isEmpty()) {
            NetflixTitle lastTitle = titles.get(titles.size() - 1);
            lastSyncedHash = lastTitle.hashCode();
        }
        log.debug("Setting last synced hash to " + lastSyncedHash);
        storedData.setLastSyncedHash(lastSyncedHash);

        storageService.setStoredData(storedData);
        log.info("Sync is done");
    }

}
