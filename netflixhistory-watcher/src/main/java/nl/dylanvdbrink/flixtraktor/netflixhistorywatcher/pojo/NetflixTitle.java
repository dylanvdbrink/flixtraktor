package nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

}

