package net.chowda.castcluster;

import org.apache.log4j.Logger;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.List;
import net.chowda.castcluster.util.LogUtil;
import net.chowda.castcluster.util.Base64Util;
import net.chowda.castcluster.util.CastClusterConfig;
import net.chowda.castcluster.playlist.PlayListManager;

/**
 *
 * this expects a url param.. videoUrl...it should be Base64 encoded.
 *
 * itunes needs .mov (or some other itunes friendly extension) in the file name...
 * so we map this servlet to that extension and feed whatever file we want as a response.
 *
 * TODO - very basic.. probably a number of ways to do this better. -DAB070314
 */
public class MovieServlet extends HttpServlet {

    private static final Logger LOG = LogUtil.getLogger(MovieServlet.class);

    public void init(ServletConfig servletConfig) throws ServletException {
        try {
            PlayListManager.getAllPlayLists();
            servletConfig.getServletContext().setAttribute("ccConfig", CastClusterConfig.getInstance());
        } catch (Exception e) {
            LOG.error("couldn't load playlists!", e);
        }
        LOG.info("movie servlet started");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String videoUrl = request.getParameter("videoUrl");
        videoUrl = Base64Util.decodeS(videoUrl);
        LOG.info("movie download request: " + videoUrl);
        List<Video> list = VideoProvider.getVideos(videoUrl);
        if (list == null || list.size() == 0) {
            throw new IllegalArgumentException("could not find video: " + videoUrl);
        }
        if (list.size() > 1) {
            throw new IllegalArgumentException("found more than one video... " + videoUrl);
        }
        String filePath = VideoProvider.fetchVideo(videoUrl);
        response.setContentType(list.get(0).getVidType());
        response.setHeader("Content-Length", String.valueOf(new File(filePath).length()));
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            ServletOutputStream out = response.getOutputStream();
            byte[] buf = new byte[5048];
            int read;
            while ((read = inputStream.read(buf)) > 0) {
                out.write(buf, 0, read);
            }
            out.flush();
            inputStream.close();
            VideoProvider.cleanupAfterFetch(videoUrl);
            LOG.info("movie download request complete: " + videoUrl);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
