package org.springframework.samples.jpetstore.web.struts;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class DownloadAction extends Action {

    private static final Log logger = LogFactory.getLog(DownloadAction.class);

    public static final String FILE_BYTE_KEY = "file-byte";

    public DownloadAction() {
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("request=" + request);
            logger.debug("response=" + response);
        }
        String filePath = servlet.getServletContext().getRealPath("WEB-INF/test.txt");
        BufferedInputStream in = null;
        int readData = -1;
        byte[] buffer = new byte[8192];
        try {
            in = new BufferedInputStream(new FileInputStream(new File(filePath)));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((readData = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, readData);
            }
            request.getSession().setAttribute(FILE_BYTE_KEY, out.toByteArray());
            out.close();
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return mapping.findForward("toServlet");
    }
}
