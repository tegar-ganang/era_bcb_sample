package com.googlecode.webduff.methods;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.googlecode.webduff.WebdavStatus;
import com.googlecode.webduff.exceptions.MethodResponseError;
import com.googlecode.webduff.io.URI;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DoGet extends DoHead {

    protected void doBody(URI uri, MethodResponse response) throws IOException {
        response.setContentType(store.getMimeType(uri));
        response.setContentLength(store.getResourceLength(uri));
        OutputStream out = response.getOutputStream();
        InputStream in = store.getResourceContent(uri);
        try {
            int read = -1;
            byte[] copyBuffer = new byte[BUF_SIZE];
            while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                out.write(copyBuffer, 0, read);
            }
        } finally {
            in.close();
        }
    }

    protected void folderBody(URI uri, HttpServletRequest req, HttpServletResponse resp) throws IOException, MethodResponseError, ServletException {
        if (store.isFolder(uri)) {
            RequestDispatcher theDispatcher = req.getRequestDispatcher("/WEB-INF/jsp/ListFolder.jsp");
            req.setAttribute("ListFolder.store", store);
            req.setAttribute("ListFolder.uri", uri);
            req.setAttribute("ListFolder.requestUri", new URI(req.getRequestURI()));
            theDispatcher.forward(req, resp);
        } else {
            if (!store.objectExists(uri)) {
                throw new MethodResponseError(WebdavStatus.SC_NOT_FOUND, req.getRequestURI());
            }
        }
    }
}
