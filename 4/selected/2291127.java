package cz.fi.muni.xkremser.editor.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.inject.Inject;
import com.google.inject.Injector;
import cz.fi.muni.xkremser.editor.client.util.Constants;
import cz.fi.muni.xkremser.editor.server.config.EditorConfiguration;
import cz.fi.muni.xkremser.editor.server.fedora.utils.IOUtils;
import cz.fi.muni.xkremser.editor.server.fedora.utils.RESTHelper;

/**
 * The Class ThumbnailServiceImpl.
 */
public class ThumbnailServiceImpl extends HttpServlet {

    private static final long serialVersionUID = 5031350967017622089L;

    /** The config. */
    @Inject
    private EditorConfiguration config;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Cache-Control", "max-age=" + Constants.HTTP_CACHE_SECONDS);
        String uuid = req.getRequestURI().substring(req.getRequestURI().indexOf(Constants.SERVLET_THUMBNAIL_PREFIX) + Constants.SERVLET_THUMBNAIL_PREFIX.length() + 1);
        if (uuid != null && !"".equals(uuid)) {
            resp.setContentType("image/jpeg");
            StringBuffer sb = new StringBuffer();
            sb.append(config.getFedoraHost()).append("/objects/").append(uuid).append("/datastreams/IMG_THUMB/content");
            InputStream is = null;
            if (!Constants.MISSING.equals(uuid)) {
                is = RESTHelper.get(sb.toString(), config.getFedoraLogin(), config.getFedoraPassword(), true);
            } else {
                is = new FileInputStream(new File("images/other/file_not_found.png"));
            }
            if (is == null) {
                return;
            }
            ServletOutputStream os = resp.getOutputStream();
            try {
                IOUtils.copyStreams(is, os);
            } catch (IOException e) {
            } finally {
                os.flush();
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    } finally {
                        is = null;
                    }
                }
            }
            resp.setStatus(200);
        } else {
            resp.setStatus(404);
        }
    }

    /**
     * Gets the config.
     * 
     * @return the config
     */
    public EditorConfiguration getConfig() {
        return config;
    }

    /**
     * Sets the config.
     * 
     * @param config
     *        the new config
     */
    public void setConfig(EditorConfiguration config) {
        this.config = config;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        Injector injector = getInjector();
        injector.injectMembers(this);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Injector injector = getInjector();
        injector.injectMembers(this);
    }

    /**
     * Gets the injector.
     * 
     * @return the injector
     */
    protected Injector getInjector() {
        return (Injector) getServletContext().getAttribute(Injector.class.getName());
    }
}
