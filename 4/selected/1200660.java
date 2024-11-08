package joggle.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import joggle.data.Manager;
import joggle.data.Song;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author  $Author: mosterme@gmail.com $
 * @version $Revision: 22 $
 */
public class ImageServlet extends HttpServlet {

    private static final long serialVersionUID = 5693065655963128553L;

    private static final Logger log = LoggerFactory.getLogger(ImageServlet.class);

    private static final Manager manager = Manager.getInstance();

    private static final String redirect = manager.getProperty("joggle.image.default");

    private static final String[] suffixes = { ".gif", ".jpe", ".jpeg", ".jpg", ".png" };

    private static final FilenameFilter filter = new SuffixFileFilter(suffixes, IOCase.INSENSITIVE);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long t0 = System.currentTimeMillis();
        String id = request.getRequestURI().split("/")[3];
        Song song = manager.find(id);
        if (song != null) {
            if (song.getArtwork()) {
                if (log.isDebugEnabled()) log.debug("song has embedded artwork");
                try {
                    AudioFile af = AudioFileIO.read(new File(song.getFile()));
                    Tag tag = af.getTag();
                    Artwork aw = tag.getFirstArtwork();
                    byte[] bytes = aw.getBinaryData();
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(aw.getMimeType());
                    response.getOutputStream().write(bytes);
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            } else {
                if (log.isDebugEnabled()) log.debug("search in directory");
                File directory = new File(song.getFile()).getParentFile();
                File[] files = directory.listFiles(filter);
                if (files != null && files.length > 0) {
                    File file = files[0];
                    String type = FilenameUtils.getExtension(file.getName()).toLowerCase();
                    if (type.startsWith("jp")) type = "jpeg";
                    String mime = "image/" + type;
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(mime);
                    FileInputStream stream = new FileInputStream(file);
                    try {
                        IOUtils.copy(stream, response.getOutputStream());
                    } catch (IOException e) {
                        log.warn(e.getMessage());
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                } else {
                    if (log.isDebugEnabled()) log.debug("image not found: " + id + ", sending redirect: " + redirect);
                    response.sendRedirect(redirect);
                }
            }
        } else {
            log.info("song not found: " + id);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        long t1 = System.currentTimeMillis();
        log.info("request: " + id + " duration: " + (t1 - t0) + "ms");
    }
}
