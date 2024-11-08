package net.sf.leechget.webapp.servlet.upload;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.leechget.Leechget;
import net.sf.leechget.webapp.manager.file.FileManager;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.FileObject;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;

/**
 * Servlet implementation class LoginServlet
 */
@RequestScoped
public class UploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final Leechget leechget;

    @Inject
    public UploadServlet(final Leechget leechget) {
        super();
        this.leechget = leechget;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final FileManager fmanager = FileManager.getFileManager(request, leechget);
        ServletFileUpload upload = new ServletFileUpload();
        FileItemIterator iter;
        try {
            iter = upload.getItemIterator(request);
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                InputStream stream = item.openStream();
                if (!item.isFormField()) {
                    final FileObject file = fmanager.getFile(name);
                    if (!file.exists()) {
                        IOUtils.copyLarge(stream, file.getContent().getOutputStream());
                    }
                }
            }
        } catch (FileUploadException e1) {
            e1.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        return;
    }
}
