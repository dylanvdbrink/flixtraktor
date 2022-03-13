package nl.dylanvdbrink.flixtraktor.service.storage;

import nl.dylanvdbrink.flixtraktor.pojo.StoredData;

import java.io.IOException;

public interface StorageService {

    StoredData getStoredData() throws IOException;
    void setStoredData(StoredData storedData) throws IOException;

}
