package nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.exceptions;

public class NetflixScrapeException extends Exception {

	private static final long serialVersionUID = 8308684830992740855L;

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
