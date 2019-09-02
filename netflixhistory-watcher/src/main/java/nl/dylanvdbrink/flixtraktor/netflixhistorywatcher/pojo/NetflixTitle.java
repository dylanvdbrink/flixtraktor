package nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetflixTitle {

    private String title;
    private LocalDate watchDate;

}

