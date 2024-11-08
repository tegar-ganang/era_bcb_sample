package net.sf.moviekebab.toolset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import quicktime.QTException;
import quicktime.QTSession;
import quicktime.std.StdQTConstants;
import quicktime.std.StdQTConstants4;
import quicktime.std.movies.AtomContainer;
import quicktime.std.movies.Movie;
import quicktime.std.movies.media.DataRef;
import quicktime.std.qtcomponents.MovieExporter;
import quicktime.util.QTHandle;

/**
 * Displays export settings and saves user-selected settings into a file.
 * This class is supposed to be used at development time only for defining
 * new supported formats, so it makes sense to exclude it from the final build.
 *
 * @author Laurent Caillette
 */
public final class QuickTimeFormatGenerator {

    private QuickTimeFormatGenerator() {
    }

    /**
   * Displays export settings dialog.
   * TODO: support more than .mov format in exporter.
   */
    private static AtomContainer askForMovieSettings() throws IOException, QTException {
        final InputStream inputStream = QuickTimeFormatGenerator.class.getResourceAsStream(REFERENCE_MOVIE_RESOURCE);
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream(1024 * 100);
        IOUtils.copy(inputStream, byteArray);
        final byte[] movieBytes = byteArray.toByteArray();
        final QTHandle qtHandle = new QTHandle(movieBytes);
        final DataRef dataRef = new DataRef(qtHandle, StdQTConstants.kDataRefFileExtensionTag, ".mov");
        final Movie movie = Movie.fromDataRef(dataRef, StdQTConstants.newMovieActive | StdQTConstants4.newMovieAsyncOK);
        final MovieExporter exporter = new MovieExporter(StdQTConstants.kQTFileTypeMovie);
        exporter.doUserDialog(movie, null, 0, movie.getDuration());
        return exporter.getExportSettingsFromAtomContainer();
    }

    private static void saveSettings(String destinationFileName, byte[] content) throws IOException {
        final FileOutputStream fileOutputStream = new FileOutputStream(destinationFileName);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
        IOUtils.copy(byteArrayInputStream, fileOutputStream);
    }

    /**
   * Must be some content that QuickTime is able to open.
   * Format doesn't seem to matter but must be consistent with
   * {@link #askForMovieSettings()}.
   */
    private static final String REFERENCE_MOVIE_RESOURCE = "/movies/Foo.mov";

    private static void printUsage() {
        System.out.println("QuickFormatTools <outputfile>");
    }

    public static void main(String[] args) throws IOException, QTException {
        if (1 != args.length) {
            printUsage();
            System.exit(-1);
        }
        QTSession.open();
        try {
            final String outputFileName = args[0];
            final AtomContainer settings = askForMovieSettings();
            saveSettings(outputFileName, settings.getBytes());
            System.out.println("Wrote settings to " + outputFileName);
        } finally {
            QTSession.close();
        }
    }
}
