package org.monet.docservice.docprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.monet.docservice.core.exceptions.ApplicationException;
import org.monet.docservice.core.log.Logger;
import org.monet.docservice.core.util.Resources;
import org.monet.docservice.docprocessor.data.Repository;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class Download extends HttpServlet {

    private static final long serialVersionUID = -5072357058374040174L;

    private Logger logger;

    private Provider<Repository> repositoryProvider;

    @Inject
    public void injectLogger(Logger logger) {
        this.logger = logger;
    }

    @Inject
    public void injectRepositoryProvider(Provider<Repository> repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("doPost(%s, %s)", req, resp);
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("doGet(%s, %s)", req, resp);
        String sDocumentId = req.getParameter("id");
        int iPage = -1;
        boolean isThumb = false;
        if (sDocumentId != null && sDocumentId.length() != 0) {
            getDownloadData(resp, sDocumentId, iPage, isThumb);
        } else {
            resp.getWriter().println("Invalid query string");
            return;
        }
    }

    private void getDownloadData(HttpServletResponse resp, String sDocumentId, int iPage, boolean isThumb) {
        Repository repository = repositoryProvider.get();
        try {
            if (!repository.existsDocument(sDocumentId)) {
                resp.setStatus(404);
                resp.setContentType("image/png");
                InputStream imageStream = Resources.getAsStream("/not_found.gif");
                resp.setContentLength(imageStream.available());
                copyData(imageStream, resp.getOutputStream());
                return;
            }
            String sContentType = repository.getDocumentDataContentType(sDocumentId);
            resp.setContentType(sContentType);
            resp.setHeader("Content-Disposition", String.format("attachment; filename=%s.pdf", sDocumentId));
            repository.readDocumentData(sDocumentId, resp.getOutputStream());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ApplicationException("Error");
        }
    }

    private void copyData(InputStream input, OutputStream output) throws IOException {
        logger.debug("copyData(%s, %s)", input, output);
        int len;
        byte[] buff = new byte[4096];
        while ((len = input.read(buff)) > 0) output.write(buff, 0, len);
    }
}
