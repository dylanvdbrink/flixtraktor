package nl.dylanvdbrink.flixtraktor.service.scraper;

public class NetflixScrapeException extends Exception {

	public NetflixScrapeException(String message) {
		super(message);
	}
	
	public NetflixScrapeException(Throwable t) {
		super(t);
	}
	
	public NetflixScrapeException(String message, Throwable t) {
		super(message, t);
	}
	
}
