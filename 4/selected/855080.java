package org.bitdrive.file.movies.impl.local;

import com.thoughtworks.xstream.XStream;
import org.bitdrive.file.movies.api.Movie;
import org.bitdrive.file.movies.api.MovieMetaDataService;
import org.bitdrive.file.movies.api.Movies;
import java.io.*;

public class LocalMovies extends Movies {

    public LocalMovies() {
    }

    public void fetchMetaData(MovieMetaDataService metaService) {
        for (Movie movie : movies) {
            if (movie.isMetadataPresent()) continue;
            if (movie instanceof LocalMovie) {
                System.out.print(movie.name + "...");
                ((LocalMovie) movie).getFullMetadata(metaService);
            }
        }
    }

    public String toLocalXml() {
        XStream xstream = createXStream();
        xstream.alias("localMovies", LocalMovies.class);
        xstream.alias("file", LocalMovieFile.class);
        xstream.alias("localMovie", LocalMovie.class);
        xstream.omitField(Movies.class, "query");
        xstream.omitField(Movies.class, "totalResults");
        xstream.omitField(LocalMovie.class, "metaService");
        return xstream.toXML(this);
    }

    public String toRemoteXml() throws IOException {
        XStream xstream = createXStream();
        xstream.omitField(LocalMovie.class, "remoteMovieFiles");
        xstream.omitField(LocalMovie.class, "remoteMovieSubtitles");
        xstream.alias("remoteMovies", LocalMovies.class);
        xstream.alias("remoteMovie", LocalMovie.class);
        xstream.alias("file", LocalMovieFile.class);
        xstream.omitField(Movies.class, "query");
        xstream.omitField(Movies.class, "totalResults");
        xstream.omitField(LocalMovie.class, "originalName");
        xstream.omitField(LocalMovie.class, "metaService");
        xstream.omitField(LocalMovieFile.class, "file");
        return xstream.toXML(this);
    }

    public static LocalMovies FromLocalXML(File file) {
        int read;
        byte[] buffer = new byte[512 * 1024];
        FileInputStream fileInputStream;
        ByteArrayOutputStream byteOutputStream;
        try {
            byteOutputStream = new ByteArrayOutputStream();
            fileInputStream = new FileInputStream(file);
            while ((read = fileInputStream.read(buffer)) > 0) {
                byteOutputStream.write(buffer, 0, read);
            }
            fileInputStream.close();
            return FromLocalXML(new String(byteOutputStream.toByteArray()));
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public static LocalMovies FromLocalXML(String xml) {
        XStream xstream = createXStream();
        xstream.alias("localMovies", LocalMovies.class);
        xstream.alias("file", LocalMovieFile.class);
        xstream.alias("localMovie", LocalMovie.class);
        xstream.omitField(Movies.class, "query");
        xstream.omitField(Movies.class, "totalResults");
        xstream.omitField(LocalMovie.class, "metaService");
        return (LocalMovies) xstream.fromXML(xml);
    }
}
