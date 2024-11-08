package ro.gateway.aida.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import ro.gateway.aida.utils.HttpUtils;
import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.Part;

/**
 * <p>Title: Romanian AIDA</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (comparator) 2003</p>
 * <p>Company: ro-gateway</p>
 * @author Mihai Popoaei, mihai_popoaei@yahoo.com, smike@intellisource.ro
 * @version 1.0-* @version $Id: UploadServlet.java,v 1.1 2004/10/24 23:37:16 mihaipostelnicu Exp $
 */
public class UploadServlet extends HttpServlet {

    /**
       *
       */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String result_bean_name = HttpUtils.getValidTrimedString(request, "bean", null);
        if (result_bean_name == null) {
            request.setAttribute(SCREEN, SCREEN_OVER);
            request.getRequestDispatcher("/misc/upload.jsp").forward(request, response);
            return;
        }
        HttpSession session = request.getSession();
        session.setAttribute(RESULT_BEAN_NAME, result_bean_name);
        request.setAttribute(SCREEN, SCREEN_UPLOAD);
        request.getRequestDispatcher("/misc/upload.jsp").forward(request, response);
    }

    /**
       *
       */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();
        synchronized (session) {
            MultipartParser parser = null;
            try {
                parser = new MultipartParser(request, 10 * 1024 * 1024);
            } catch (IOException ioEx) {
                return;
            }
            File file = null;
            Part part = null;
            FilePart filePart = null;
            while ((part = parser.readNextPart()) != null) {
                if (part.isParam()) {
                } else if (part.isFile()) {
                    filePart = (FilePart) part;
                    file = File.createTempFile("AIDA-", ".tmp");
                    filePart.writeTo(file);
                    if (file.length() == 0) {
                        file = null;
                        filePart = null;
                    } else {
                        break;
                    }
                }
            }
            if (filePart != null) {
                Hashtable result = new Hashtable();
                result.put(RESULT_FILE, file);
                result.put(RESULT_FILE_NAME, filePart.getFileName());
                String res_para_name = (String) session.getAttribute(RESULT_BEAN_NAME);
                if (res_para_name != null) {
                    session.setAttribute(res_para_name, result);
                    response.sendRedirect("upload?done=true");
                    return;
                }
            }
            response.sendRedirect("upload");
        }
    }

    public static void copy(File src, File dest, boolean overwrite) throws IOException {
        if (!src.exists()) throw new IOException("File source does not exists");
        if (dest.exists()) {
            if (!overwrite) throw new IOException("File destination already exists");
            dest.delete();
        } else {
            dest.createNewFile();
        }
        InputStream is = new FileInputStream(src);
        OutputStream os = new FileOutputStream(dest);
        byte[] buffer = new byte[1024 * 4];
        int len = 0;
        while ((len = is.read(buffer)) > 0) {
            os.write(buffer, 0, len);
        }
        os.close();
        is.close();
    }

    public static final String RESULT_BEAN_NAME = "__upload_result___";

    public static final String RESULT_FILE = "__upload_result___f";

    public static final String RESULT_FILE_NAME = "__upload_result___fn";

    public static final String SCREEN = "scr";

    public static final String SCREEN_OVER = "scrover";

    public static final String SCREEN_UPLOAD = "scrupload";
}
