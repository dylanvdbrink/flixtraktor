package nl.dylanvdbrink.flixtraktor.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class StoredData {
    private int lastSyncedHash;
    private boolean syncRunning;
    private StoredAuthData storedAuthData;

    public static StoredData getEmptyInstance() {
        return new StoredData(-1, false, StoredAuthData.getEmptyInstance());
    }
}
