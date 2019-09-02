package nl.dylanvdbrink.flixtraktor.watchedtitleenricher;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class NetflixWatchedTitleConsumer {

    @JmsListener(destination = "${activemq.subject}")
    public void receiveMessage() {
        // Try to specify the type of title (movie, tvshow)

        // Call Trakt to search for title and get the result

        // Send enriched title to queue
    }

}
