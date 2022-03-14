package nl.dylanvdbrink.flixtraktor;

import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.exceptions.NetflixScrapeException;
import nl.dylanvdbrink.flixtraktor.exceptions.TraktException;
import nl.dylanvdbrink.flixtraktor.pojo.NetflixTitle;
import nl.dylanvdbrink.flixtraktor.service.scraper.NetflixViewingActivityService;
import nl.dylanvdbrink.flixtraktor.service.trakt.TraktService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
@CommonsLog
public class FlixtraktorApplication implements CommandLineRunner {
	private final TraktService traktService;

	public FlixtraktorApplication(TraktService traktService) {
		this.traktService = traktService;
	}

	public static void main(String[] args) {
		SpringApplication.run(FlixtraktorApplication.class, args);
	}

	public void run(String... args) throws IOException, TraktException, InterruptedException {
		traktService.ensureAuthorized();
	}

}
