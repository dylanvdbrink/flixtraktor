package nl.dylanvdbrink.flixtraktor.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetflixTitle {

    private String title;
    private String videoTitle;
    private long movieID;
    private String country;
    private int bookmark;
    private long date;
    private int deviceType;
    private String dateStr;
    private int index;
    private String topNodeId;
    private long series;
    private String seriesTitle;
    private String seasonDescriptor;
    private String episodeTitle;
    private String estRating;

    public int hashCode() {
        return Objects.hash(title, date);
    }

    public boolean equals(Object anotherObject) {
        if (this == anotherObject) {
            return true;
        }

        if (anotherObject instanceof NetflixTitle netflixTitle) {
            return Objects.equals(netflixTitle.getTitle(), this.getTitle()) && Objects.equals(netflixTitle.getDate(), this.getDate());
        }

        return false;
    }

}

