package nl.dylanvdbrink.flixtraktor.parser;

import nl.dylanvdbrink.flixtraktor.pojo.NetflixTitle;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class NetflixViewingActivityCSVParser {

    private NetflixViewingActivityCSVParser() { }

    public List<NetflixTitle> parse(String csv) throws IOException {
        List<NetflixTitle> titles;

        try (BufferedReader b = new BufferedReader(new StringReader(csv))) {
            titles = read(b);
        }

        return titles;
    }

    public List<NetflixTitle> parse(File csv) throws IOException {
        List<NetflixTitle> titles;

        try (BufferedReader b = new BufferedReader(new FileReader(csv))) {
            titles = read(b);
        }

        return titles;
    }

    public List<NetflixTitle> parse(InputStream stream) throws IOException {
        List<NetflixTitle> titles;

        try (BufferedReader b = new BufferedReader(new InputStreamReader(stream))) {
            titles = read(b);
        }

        return titles;
    }

    private List<NetflixTitle> read(BufferedReader br) throws IOException {
        List<NetflixTitle> titles = new ArrayList<>();

        String readLine;
        int linenumber = 0;
        while ((readLine = br.readLine()) != null) {
            linenumber++;
            if (linenumber == 1) {
                continue;
            }

            int dateStart = readLine.lastIndexOf(',') + 1;
            String title = readLine.substring(1, dateStart - 2);
            String date = readLine.substring(dateStart).replace("\"", "");

            NetflixTitle nt = new NetflixTitle();
            nt.setTitle(title);
            nt.setDateStr(date);

            titles.add(nt);
        }
        return titles;
    }

}
