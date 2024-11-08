package in.raster.oviyam.servlet;

import in.raster.oviyam.config.ServerConfiguration;
import in.raster.oviyam.config.ServerXmlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * Servlet class returns the JPG image as response.
 * 
 * @author bharathi
 * @version 0.7
 * 
 */
public class DcmImage extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(DcmImage.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String imageURL = "";
        ServerXmlConfiguration sxc = new ServerXmlConfiguration();
        ServerConfiguration sc = sxc.getElementValues();
        String study = request.getParameter("study");
        String series = request.getParameter("series");
        String object = request.getParameter("object");
        String frameNo = request.getParameter("frameNumber");
        String contentType = request.getParameter("contentType");
        String rows = request.getParameter("rows");
        String windowCenter = request.getParameter("windowCenter");
        String windowWidth = request.getParameter("windowWidth");
        imageURL = "http://" + sc.getHostName() + ":" + sc.getWadoPort() + "/wado?requestType=WADO&studyUID=" + study + "&seriesUID=" + series + "&objectUID=" + object;
        if (request.getParameter("frameNumber") != null) {
            imageURL += "&frameNumber=" + frameNo;
        }
        if (contentType != null) {
            imageURL += "&contentType=" + contentType;
            response.setContentType(contentType);
        }
        if (rows != null) {
            imageURL += "&rows=" + rows;
        }
        if (windowCenter != null) {
            imageURL += "&windowCenter=" + windowCenter;
        }
        if (windowWidth != null) {
            imageURL += "&windowWidth=" + windowWidth;
        }
        InputStream resultInStream = null;
        OutputStream resultOutStream = response.getOutputStream();
        try {
            URL imageUrl = new URL(imageURL);
            if (object != null && !object.equalsIgnoreCase("")) {
                resultInStream = imageUrl.openStream();
                byte[] buffer = new byte[4096];
                int bytes_read;
                while ((bytes_read = resultInStream.read(buffer)) != -1) {
                    resultOutStream.write(buffer, 0, bytes_read);
                }
            }
            resultOutStream.flush();
        } catch (Exception e) {
            log.error("Unable to read and write the image from http://" + sc.getHostName() + ":" + sc.getWadoPort(), e);
        } finally {
            if (resultOutStream != null) {
                try {
                    resultOutStream.close();
                } catch (Exception ignore) {
                }
            }
            if (resultInStream != null) {
                try {
                    resultInStream.close();
                } catch (Exception ignore) {
                }
            }
        }
    }
}
