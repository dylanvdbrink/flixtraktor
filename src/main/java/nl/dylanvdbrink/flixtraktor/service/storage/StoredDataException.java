package nl.dylanvdbrink.flixtraktor.service.storage;

public class StoredDataException extends Exception {

    public StoredDataException(String message) {
        super(message);
    }

    public StoredDataException(Throwable t) {
        super(t);
    }

    public StoredDataException(String message, Throwable t) {
        super(message, t);
    }

}
