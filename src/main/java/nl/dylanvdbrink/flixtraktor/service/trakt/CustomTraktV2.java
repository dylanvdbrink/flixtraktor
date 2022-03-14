package nl.dylanvdbrink.flixtraktor.service.trakt;

import com.uwetrottmann.trakt5.TraktV2;
import okhttp3.OkHttpClient;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public class CustomTraktV2 extends TraktV2 {
    private static final long TIMEOUT_SECONDS = 300;

    public CustomTraktV2(String apiKey, String clientSecret) {
        super(apiKey, clientSecret, "");
    }

    @Override
    protected void setOkHttpClientDefaults(@Nonnull OkHttpClient.Builder builder) {
        super.setOkHttpClientDefaults(builder);
        builder.callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        builder.connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        builder.readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        builder.writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
