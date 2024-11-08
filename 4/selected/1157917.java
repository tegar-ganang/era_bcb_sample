package joggle.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import joggle.data.Manager;
import joggle.data.Scanner;
import joggle.data.Song;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author  $Author: mosterme@gmail.com $
 * @version $Revision: 22 $
 */
public class StreamServlet extends HttpServlet {

    private static final long serialVersionUID = 7851993079862260727L;

    private static final Logger log = LoggerFactory.getLogger(StreamServlet.class);

    private static final Manager manager = Manager.getInstance();

    @Override
    public void init() throws ServletException {
        String directory = manager.getProperty("joggle.music.directory");
        Scanner scanner = new Scanner();
        scanner.scan(directory);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getRequestURI().split("/")[3];
        if (log.isDebugEnabled()) log.debug("request: " + id + " from: " + request.getRemoteHost());
        Song song = manager.find(id);
        if (song != null) {
            File file = new File(song.getFile());
            if (file.exists()) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("audio/" + song.getType());
                response.setContentLength((int) file.length());
                FileInputStream stream = new FileInputStream(file);
                try {
                    IOUtils.copy(stream, response.getOutputStream());
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            } else {
                log.warn("file not found: " + file);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            log.info("song not found: " + id);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
