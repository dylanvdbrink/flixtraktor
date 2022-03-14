package nl.dylanvdbrink.flixtraktor.service.trakt;

import com.google.common.collect.Lists;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.*;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.Type;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.exceptions.TraktException;
import nl.dylanvdbrink.flixtraktor.pojo.NetflixTitle;
import nl.dylanvdbrink.flixtraktor.pojo.StoredAuthData;
import nl.dylanvdbrink.flixtraktor.service.storage.StorageService;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import retrofit2.Response;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@CommonsLog
public class TraktClient {
    private final StorageService storageService;

    private final TraktV2 traktApi;
    private final ClientId clientId;
    private final String clientSecret;

    public TraktClient(StorageService storageService, @Value("${trakt.client-id}") String clientId,
                       @Value("${trakt.client-secret}") String clientSecret) {
        this.storageService = storageService;

        traktApi = new CustomTraktV2(clientId, clientSecret);
        this.clientId = new ClientId();
        this.clientId.client_id = clientId;
        this.clientSecret = clientSecret;
    }

    public AccessToken refreshAuthorization() throws TraktException {
        try {
            StoredAuthData storedAuthData = storageService.getStoredData().getStoredAuthData();
            return traktApi.refreshAccessToken(storedAuthData.getTraktRefreshToken()).body();
        } catch (IOException e) {
            throw new TraktException("Could not retrieve settings", e);
        }
    }

    public DeviceCode getDeviceCode() throws TraktException {
        try {
            Response<DeviceCode> response = traktApi.authentication().generateDeviceCode(this.clientId).execute();
            return response.body();
        } catch (IOException e) {
            throw new TraktException("Could not retrieve devicecode", e);
        }
    }

    public AccessToken getToken(DeviceCode deviceCode) throws TraktException {
        DeviceCodeAccessTokenRequest deviceCodeAccessTokenRequest = new DeviceCodeAccessTokenRequest();
        deviceCodeAccessTokenRequest.code = deviceCode.device_code;
        deviceCodeAccessTokenRequest.client_id = clientId.client_id;
        deviceCodeAccessTokenRequest.client_secret = clientSecret;

        try {
            Response<AccessToken> response = traktApi.authentication().exchangeDeviceCodeForAccessToken(deviceCodeAccessTokenRequest).execute();
            return response.body();
        } catch (IOException e) {
            throw new TraktException("Could not get device code.", e);
        }
    }

    public void addToCollection(List<NetflixTitle> titles) throws InterruptedException {
        List<SyncMovie> syncMovies = new ArrayList<>();
        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        log.info("Starting title search");
        for (NetflixTitle netflixTitle : titles) {
            try {
                if (netflixTitle.getSeries() == 0) {
                    // Movie
                    Movie m = searchMovie(netflixTitle);

                    OffsetDateTime watchedAt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(netflixTitle.getDate()), ZoneId.of("GMT"));

                    SyncMovie syncMovie = new SyncMovie()
                            .watchedAt(watchedAt)
                            .id(MovieIds.trakt(m.ids.trakt));
                    log.debug(MessageFormat.format("Found result. Movie: {0}. Date: {1}", m.title, watchedAt));
                    syncMovies.add(syncMovie);
                } else {
                    // Episode
                    SearchResult searchResult = searchEpisode(netflixTitle);
                    Episode foundEpisode = searchResult.episode;
                    Show show = searchResult.show;

                    OffsetDateTime watchedAt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(netflixTitle.getDate()), ZoneId.of("GMT"));

                    SyncEpisode syncEpisode = new SyncEpisode()
                            .watchedAt(watchedAt)
                            .id(EpisodeIds.trakt(foundEpisode.ids.trakt));
                    log.debug(MessageFormat.format("Found result. Show: {0}. Episode: {1}. Date: {2}", show.title, foundEpisode.title, watchedAt.toString()));
                    syncEpisodes.add(syncEpisode);
                }
            } catch (TraktException e) {
                String name = Optional.ofNullable(netflixTitle.getSeriesTitle()).orElse("") + " " + netflixTitle.getTitle();
                log.debug("Error searching title", e);
                errors.add(name.trim());
            }
        }

        log.info(MessageFormat.format("Title search done. Found {0} movies and {1} episodes. {2} errors", syncMovies.size(),
                syncEpisodes.size(), errors.size()));
        if (!errors.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder("Error list: \n");
            for (String error : errors) {
                stringBuilder
                        .append("\t")
                        .append(error)
                        .append("\n");
            }
            log.warn(stringBuilder.toString());
        }

        try {
            StoredAuthData storedAuthData = storageService.getStoredData().getStoredAuthData();
            traktApi.accessToken(storedAuthData.getTraktAccessToken());

            List<List<SyncEpisode>> episodePartitions = Lists.partition(syncEpisodes, 200);
            for (List<SyncEpisode> partition : episodePartitions) {
                SyncItems syncItems = new SyncItems();
                syncItems.episodes = partition;

                Response<SyncResponse> response = traktApi.sync().addItemsToWatchedHistory(syncItems).execute();
                if (!response.isSuccessful()) {
                    throw new TraktException("Trakt sync was unsuccesful: " + response.errorBody().string());
                } else {
                    log.info(MessageFormat.format("Synced {0} episodes", episodePartitions.size()));
                }
                Thread.sleep(1000);
            }
            log.info(MessageFormat.format("Movies sync done. Synced {0} movies.", syncMovies.size()));

            List<List<SyncMovie>> moviePartitions = Lists.partition(syncMovies, 200);
            for (List<SyncMovie> partition : moviePartitions) {
                SyncItems syncItems = new SyncItems();
                syncItems.movies = partition;

                Response<SyncResponse> response = traktApi.sync().addItemsToWatchedHistory(syncItems).execute();
                if (!response.isSuccessful()) {
                    throw new TraktException("Trakt sync was unsuccesful: " + response.errorBody().string());
                } else {
                    log.info(MessageFormat.format("Synced {0} movies", moviePartitions.size()));
                }
                Thread.sleep(1000);
            }
            log.info(MessageFormat.format("Episode sync done. Synced {0} episodes.", syncEpisodes.size()));
        } catch (IOException | TraktException e) {
            log.error("Could not sync with Trakt", e);
        }
    }

    public SearchResult searchEpisode(NetflixTitle netflixTitle) throws TraktException {
        String query = normalizeTitle(netflixTitle.getSeriesTitle() + " " + netflixTitle.getEpisodeTitle());
        log.debug("Searching show: " + query);
        Response<List<SearchResult>> results;
        try {
            results = traktApi
                    .search()
                    .textQuery(Type.EPISODE, query, null, null, null, null, null,null, null, 1, 1)
                    .execute();
        } catch (IOException e) {
            throw new TraktException(e);
        }

        SearchResult foundEpisode = null;

        String incorrectReason = null;
        if (!results.isSuccessful() || results.body() == null || results.body().isEmpty() || results.body().get(0).episode == null || results.body().get(0).show == null) {
            incorrectReason = "Could not find episode";
        } else {
            foundEpisode = results.body().get(0);
        }

        if (foundEpisode != null &&
                new LevenshteinDistance(10).apply(normalizeTitle(netflixTitle.getSeriesTitle()), normalizeTitle(foundEpisode.show.title)) > 5) {
            incorrectReason = "Found an episode but the title of the show did not match";
        }

        if (incorrectReason != null) {
            // One last attempt for shows that have the episode number as the episode title.
            Show s = searchShow(netflixTitle);
            if (new LevenshteinDistance(10).apply(normalizeTitle(netflixTitle.getSeriesTitle()), normalizeTitle(s.title)) > 5) {
                throw new TraktException("Show name did not match Netflix title");
            }

            try {
                int seasonNumber = retrieveIndexFromDescription(netflixTitle.getSeasonDescriptor());
                int episodeNumber = retrieveIndexFromDescription(netflixTitle.getEpisodeTitle());
                List<Season> seasons = getSeasonsForShow(s.ids.trakt);
                Episode e = seasons.get(seasonNumber - 1).episodes.get(episodeNumber - 1);
                foundEpisode = new SearchResult();
                foundEpisode.episode = e;
                foundEpisode.show = s;
            } catch (NumberFormatException | NullPointerException | IndexOutOfBoundsException e) {
                throw new TraktException("Could not get episode with the episodeIndex retrieve method", e);
            }
        }

        return foundEpisode;
    }

    public Show searchShow(NetflixTitle netflixTitle) throws TraktException {
        String query = normalizeTitle(netflixTitle.getSeriesTitle());
        log.debug("Searching show: " + query);
        Response<List<SearchResult>> results;
        try {
            results = traktApi
                    .search()
                    .textQueryShow(query, null, null, null, null, null, null, null,
                            null, null, Extended.METADATA, 1, 1)
                    .execute();
        } catch (IOException e) {
            throw new TraktException(e);
        }

        if (!results.isSuccessful() || results.body() == null || results.body().isEmpty() || results.body().get(0).show == null) {
            throw new TraktException("Could not find show");
        }

        return results.body().get(0).show;
    }

    public List<Season> getSeasonsForShow(int traktId) throws TraktException {
        log.debug("Getting seasons for show: " + traktId);
        Response<List<Season>> results;
        try {
            results = traktApi
                    .seasons()
                    .summary(String.valueOf(traktId), Extended.EPISODES)
                    .execute();
        } catch (IOException e) {
            throw new TraktException(e);
        }

        if (!results.isSuccessful() || results.body() == null || results.body().isEmpty()) {
            throw new TraktException("Could not find seasons for show");
        }

        return results.body();
    }

    public Movie searchMovie(NetflixTitle netflixTitle) throws TraktException {
        String query = normalizeTitle(netflixTitle.getTitle());
        log.debug("Searching movie: " + query);
        Response<List<SearchResult>> results;
        try {
            results = traktApi
                    .search()
                    .textQueryMovie(query, null, null, null, null, null, null, null, Extended.FULL, 1, 1)
                    .execute();
        } catch (IOException e) {
            throw new TraktException(e);
        }

        if (!results.isSuccessful() || results.body() == null || results.body().isEmpty() || results.body().get(0).movie == null) {
            throw new TraktException("Could not find episode");
        }

        return results.body().get(0).movie;
    }

    private String normalizeTitle(String strData) {
        if (strData == null) {
            return "";
        }

        return strData
                .replace("&lt;", "")
                .replace("&gt;", "")
                .replace("&apos;", "")
                .replace("&quot;", "")
                .replace("&amp;", "")
                .replace("?", "")
                .replace(":", "")
                .toLowerCase();
    }

    private int retrieveIndexFromDescription(String description) {
        return Integer.parseInt(description.replaceAll("\\D+", ""));
    }

}
