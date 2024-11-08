package com.goodcodeisbeautiful.archtea.web.action;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.goodcodeisbeautiful.archtea.web.form.FolderForm;
import com.goodcodeisbeautiful.archtea.io.data.DataContainer;
import com.goodcodeisbeautiful.archtea.io.data.DataContainerReaderType;
import com.goodcodeisbeautiful.archtea.io.Entry;

/**
 * @author hata
 *
 */
public class FolderAction extends Action {

    /** default content-type */
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * 
     */
    public FolderAction() {
        super();
    }

    /**
     * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res) throws Exception {
        FolderForm f = (FolderForm) form;
        Entry entry = f.getCurrentEntry();
        if (entry instanceof DataContainer) {
            DataContainer container = ((DataContainer) entry);
            String contentType = container.getContentType();
            String charset = container.getCharset();
            if (contentType != null && contentType.startsWith("text/")) {
                res.setContentType(contentType + (charset != null ? "; charset=" + charset : ""));
                writeTextTo(container.getReader(DataContainerReaderType.WEB), res.getWriter());
            } else {
                res.setContentType(contentType != null ? contentType : DEFAULT_CONTENT_TYPE);
                writeBinaryTo(container.getInputStream(), res.getOutputStream());
            }
            return null;
        }
        return mapping.findForward("success");
    }

    private void writeTextTo(Reader reader, PrintWriter writer) throws IOException {
        char[] buff = new char[8192];
        try {
            if (reader == null || writer == null) throw new IOException("Stream is null. reader=" + reader + ", writer = " + writer);
            int len = reader.read(buff, 0, buff.length);
            while (len != -1) {
                writer.write(buff, 0, len);
                len = reader.read(buff, 0, buff.length);
            }
            writer.flush();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void writeBinaryTo(InputStream in, OutputStream out) throws IOException {
        byte[] buff = new byte[8192];
        try {
            if (out == null || in == null) throw new IOException("Stream is null. out=" + out + ", in = " + in);
            int len = in.read(buff);
            while (len != -1) {
                out.write(buff, 0, len);
                len = in.read(buff);
            }
            out.flush();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
