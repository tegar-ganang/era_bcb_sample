package servlet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import Utils.Config;
import Utils.Debug;
import Utils.JavaUtil;
import Utils.Project;
import Utils.UserInfo;
import Engine.DetectEngine;

public class UploadFile extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
	 * Constructor of the object.
	 */
    public UploadFile() {
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
        processSingleFile(request, response);
    }

    private void processSingleFile(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (ServletFileUpload.isMultipartContent(request)) {
            UserInfo userInfo = (UserInfo) request.getSession().getAttribute("userInfo");
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List items = upload.parseRequest(request);
                String fileName = ((FileItem) (items.get(0))).getFieldName();
                String sessionId = request.getSession().getId();
                File dir = new File("tempFiles\\" + sessionId);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File("tempFiles\\" + sessionId + "\\" + fileName);
                if (file.exists()) {
                    file.delete();
                }
                try {
                    ((FileItem) (items.get(0))).write(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String classFileName = getClassFileName("tempFiles\\" + sessionId + "\\" + fileName);
                String tempFilePath = "tempFiles\\" + sessionId + "\\" + classFileName + ".java";
                File file2 = new File(tempFilePath);
                FileUtils.copyFile(file, file2);
                JavaUtil.getInstance().compile(tempFilePath);
                System.out.println(JavaUtil.getInstance().getResult());
                DetectEngine engine = new DetectEngine();
                engine.reportFromFile("tempFiles\\" + sessionId + "\\" + classFileName + ".java", userInfo);
                HttpSession session = request.getSession();
                session.setAttribute("reports", engine.getReports());
                RequestDispatcher dispatcher = request.getRequestDispatcher("/showReports.jsp");
                dispatcher.forward(request, response);
            } catch (FileUploadException e) {
                e.printStackTrace();
            }
        }
    }

    private String getClassFileName(String string) {
        try {
            Scanner input = new Scanner(new File(string));
            while (input.hasNextLine()) {
                String line = input.nextLine();
                if (line.startsWith("public class ")) {
                    String className = "";
                    className = line.substring(13, line.indexOf("{")).trim();
                    return className;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
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
        doGet(request, response);
    }

    /**
	 * Initialization of the servlet. <br>
	 * 
	 * @throws ServletException
	 *             if an error occurs
	 */
    public void init() throws ServletException {
    }
}
