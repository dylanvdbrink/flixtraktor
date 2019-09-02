package nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.jobs;

import com.google.gson.Gson;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.jms.WatchedTitleProducer;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.exceptions.NetflixScrapeException;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.pojo.NetflixTitle;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.scraper.NetflixViewingActivityScraper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@CommonsLog
public class QueueViewingActivityJob extends QuartzJobBean {

    private final NetflixViewingActivityScraper scraper;
    private final WatchedTitleProducer producer;

    public QueueViewingActivityJob(NetflixViewingActivityScraper scraper, WatchedTitleProducer producer) {
        this.scraper = scraper;
        this.producer = producer;
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("QueueViewingActivityJob started");

        try {
            List<NetflixTitle> titles = scraper.getViewingActivity();
            Gson gson = new Gson();

            for (NetflixTitle nt : titles) {
                producer.sendMessage(gson.toJson(nt));
            }
        } catch (NetflixScrapeException e) {
            log.error("Could not get viewing activity", e);
        } catch (InterruptedException e) {
            log.error("Could not get viewing activity", e);
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("Could not create webdriver", e);
        }

        log.info("QueueViewingActivityJob done");
    }
}
