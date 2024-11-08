package net.sf.djdoc.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;
import net.sf.djdoc.bo.Api;
import net.sf.djdoc.bo.Apis;
import net.sf.djdoc.servlet.DjdocRequest;
import net.sf.djdoc.util.IOUtils;

/**
 * The <code>FileAction</code> is an implementation of the {@link Action}
 * interface which simply copies the content of a file into the response.
 */
public class FileAction implements Action {

    /**
   * This method identifies the file to be sent back by the request path and
   * returns a {@link File} object for it. No exception is thrown if the file
   * does not exist.
   *
   * @param req The wrapper around the HTTP request
   * @return The {@link File} object to be copied into the response
   */
    public Object perform(DjdocRequest req) {
        Api api = Apis.getApi(req.getApiName());
        return new File(api.getPath(), req.getFileName());
    }

    /**
   * This method directly copies the content of the file returned by the method
   * {@link #perform(DjdocRequest)} into the HTTP response output stream.
   *
   * @param req The wrapper around the HTTP request
   * @param res The HTTP response connected to the server call
   * @throws IOException If the specified file is not found, an {@link
   * IOException} is thrown.
   */
    public void sendResponse(DjdocRequest req, HttpServletResponse res) throws IOException {
        File file = (File) req.getResult();
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            IOUtils.copy(in, res.getOutputStream());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
