package nl.dylanvdbrink.flixtraktor.service.trakt;

public class TraktException extends Exception {

    public TraktException(String message) {
        super(message);
    }

    public TraktException(Throwable t) {
        super(t);
    }

    public TraktException(String message, Throwable t) {
        super(message, t);
    }

}
