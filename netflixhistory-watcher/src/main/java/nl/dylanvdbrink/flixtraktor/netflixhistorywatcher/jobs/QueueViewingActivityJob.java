package nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.jobs;

import com.google.gson.Gson;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.jms.WatchedTitleProducer;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.exceptions.NetflixScrapeException;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.pojo.NetflixTitle;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.scraper.NetflixViewingActivityService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@CommonsLog
public class QueueViewingActivityJob extends QuartzJobBean {

    private final NetflixViewingActivityService viewingactivityService;
    private final WatchedTitleProducer producer;

    @Value("${maxbatchsize:0}")
    private int maxBatchSize;

    public QueueViewingActivityJob(NetflixViewingActivityService viewingactivityService, WatchedTitleProducer producer) {
        this.viewingactivityService = viewingactivityService;
        this.producer = producer;
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("QueueViewingActivityJob started");

        try {
            List<NetflixTitle> titles = viewingactivityService.getViewingActivity();
            Collections.reverse(titles);
            Gson gson = new Gson();

            if (maxBatchSize != 0) {
                titles = titles.subList(titles.size() - maxBatchSize, titles.size());
            }

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
