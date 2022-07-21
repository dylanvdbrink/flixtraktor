package nl.dylanvdbrink.flixtraktor;

import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.service.trakt.TraktException;
import nl.dylanvdbrink.flixtraktor.service.trakt.TraktAuthenticationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
@CommonsLog
public class FlixtraktorApplication implements CommandLineRunner {
	private final TraktAuthenticationService traktAuthenticationService;

	public FlixtraktorApplication(TraktAuthenticationService traktAuthenticationService) {
		this.traktAuthenticationService = traktAuthenticationService;
	}

	public static void main(String[] args) {
		SpringApplication.run(FlixtraktorApplication.class, args);
	}

	public void run(String... args) throws IOException, TraktException, InterruptedException {
		traktAuthenticationService.ensureAuthorized();
	}

}
