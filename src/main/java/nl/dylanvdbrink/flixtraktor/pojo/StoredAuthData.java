package nl.dylanvdbrink.flixtraktor.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class StoredAuthData {
    private String traktAccessToken;
    private String traktRefreshToken;
    private long traktTokenIssuedAt;
    private long traktTokenExpiresAt;

    public static StoredAuthData getEmptyInstance() {
        return new StoredAuthData("", "", 0, 0);
    }

}
