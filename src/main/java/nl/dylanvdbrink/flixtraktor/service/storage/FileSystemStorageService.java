package nl.dylanvdbrink.flixtraktor.service.storage;

import com.google.gson.Gson;
import nl.dylanvdbrink.flixtraktor.pojo.StoredData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(value="storage.type", havingValue = "file", matchIfMissing = true)
public class FileSystemStorageService implements StorageService {
    private static final String FILE_LOCATION = "./storage.json";
    private final Gson gson = new Gson();

    public StoredData getStoredData() throws IOException {
        Path path = Paths.get(FILE_LOCATION);
        if (!Files.exists(path)) {
            StoredData data = StoredData.getEmptyInstance();
            setStoredData(data);
            return data;
        }

        String json = Files.readString(path);
        return gson.fromJson(json, StoredData.class);
    }

    public void setStoredData(StoredData storedData) throws IOException {
        String json = gson.toJson(storedData);
        Path path = Paths.get(FILE_LOCATION);
        Files.writeString(path, json);
    }

}
