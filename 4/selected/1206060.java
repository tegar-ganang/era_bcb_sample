package org.foafrealm.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.foafrealm.beans.ConfigKeeper;
import org.foafrealm.beans.Context;

/**
 * 
 * 
 * @author Sebastian Ryszard Kruk,
 * @created 15.11.2005
 */
public class ShowPicture extends HttpServlet {

    private static final Logger log = Logger.getLogger(ShowPicture.class.getName());

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
	 * Constructor of the object.
	 */
    public ShowPicture() {
        super();
    }

    /**
	 * Destruction of the servlet. <br>
	 */
    public void destroy() {
        super.destroy();
    }

    /**
	 * The doGet method of the servlet. <br>
	 * 
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
	 * The doPost method of the servlet. <br>
	 * 
	 * This method is called when a form has its tag value method equals to
	 * post.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
	 * Initialization of the servlet. <br>
	 * 
	 * @throws ServletException
	 *             if an error occure
	 */
    public void init() throws ServletException {
    }

    /**
	 * 
	 * @param request
	 *            the request should contain pic field, which is the name of the
	 *            picture
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getParameter("pic");
        if (name != null && validate(name) == true) {
            if (name.indexOf('.') == -1) name += ".jpg";
            if (name.substring(name.indexOf('.')).equals("gif")) response.setContentType("image/gif"); else if (name.substring(name.indexOf('.')).equals("png")) response.setContentType("image/png"); else if (name.substring(name.indexOf('.')).equals("tiff")) response.setContentType("image/tiff"); else response.setContentType("image/jpeg");
            OutputStream os = response.getOutputStream();
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                File f = new File(ConfigKeeper.getInstallDir() + ConfigKeeper.getStoragePath() + name);
                FileInputStream fis = new FileInputStream(f);
                byte buff[] = new byte[100000];
                int n;
                while ((n = fis.read(buff, 0, 100000)) > 0) baos.write(buff, 0, n);
                fis.close();
                baos.writeTo(os);
                baos.close();
            } catch (FileNotFoundException ex) {
                log.warning(ex.toString());
            } catch (Exception e) {
                log.log(Level.SEVERE, e.toString(), e);
            }
            os.close();
        }
    }

    public static boolean validate(String in) {
        if (in.indexOf('<') != -1) {
            return false;
        }
        if (in.indexOf('>') != -1) {
            return false;
        }
        if (in.indexOf('\'') != -1) {
            return false;
        }
        if (in.indexOf('\"') != -1) {
            return false;
        }
        if (in.indexOf('&') != -1) {
            return false;
        }
        return true;
    }
}
