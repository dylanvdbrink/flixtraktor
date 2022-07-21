package nl.dylanvdbrink.flixtraktor.service.storage;

import com.google.gson.Gson;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.pojo.StoredData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(value="storage.type", havingValue = "file", matchIfMissing = true)
@CommonsLog
public class FileSystemStorageService implements StorageService {
    private static final String FILE_NAME = "storage.json";
    private final Path filePath;
    private final Gson gson = new Gson();

    public FileSystemStorageService(@Value("${storage.file.location}") String fileDestination) {
        this.filePath = Paths.get(fileDestination, FILE_NAME);
    }

    public StoredData getStoredData() throws IOException {
        if (!Files.exists(filePath)) {
            log.debug("StoredData file does not exist yet, creating one...");
            StoredData data = StoredData.getEmptyInstance();
            setStoredData(data);
            return data;
        }

        String json = Files.readString(filePath);
        return gson.fromJson(json, StoredData.class);
    }

    public void setStoredData(StoredData storedData) throws IOException {
        String json = gson.toJson(storedData);
        log.debug("Writing storedData to file: " + json);
        Files.writeString(filePath, json);
        log.debug("Wrote storedData to " + filePath.toAbsolutePath());
    }

}
