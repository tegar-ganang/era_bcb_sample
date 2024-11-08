package com.creawor.hz_market.t_attach;

import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.creawor.hz_market.servlet.LoadMapInfoAjax;
import com.creawor.hz_market.t_infor_review.t_infor_review;
import com.creawor.hz_market.t_infor_review.t_infor_review_Form;
import com.creawor.hz_market.t_infor_review.t_infor_review_QueryMap;
import com.creawor.km.util.DownType;

public class AttachServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(LoadMapInfoAjax.class);

    /**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String act = request.getParameter("act");
            if (null == act) {
            } else if ("down".equalsIgnoreCase(act)) {
                String vest = request.getParameter("vest");
                String id = request.getParameter("id");
                if (null == vest) {
                    t_attach_Form attach = null;
                    t_attach_QueryMap query = new t_attach_QueryMap();
                    attach = query.getByID(id);
                    if (null != attach) {
                        String filename = attach.getAttach_name();
                        String fullname = attach.getAttach_fullname();
                        response.addHeader("Content-Disposition", "attachment;filename=" + filename + "");
                        File file = new File(fullname);
                        if (file.exists()) {
                            java.io.FileInputStream in = new FileInputStream(file);
                            org.apache.commons.io.IOUtils.copy(in, response.getOutputStream());
                        }
                    }
                } else if ("review".equalsIgnoreCase(vest)) {
                    t_infor_review_QueryMap reviewQuery = new t_infor_review_QueryMap();
                    t_infor_review_Form review = reviewQuery.getByID(id);
                    String seq = request.getParameter("seq");
                    String name = null, fullname = null;
                    if ("1".equals(seq)) {
                        name = review.getAttachname1();
                        fullname = review.getAttachfullname1();
                    } else if ("2".equals(seq)) {
                        name = review.getAttachname2();
                        fullname = review.getAttachfullname2();
                    } else if ("3".equals(seq)) {
                        name = review.getAttachname3();
                        fullname = review.getAttachfullname3();
                    }
                    String downTypeStr = DownType.getInst().getDownTypeByFileName(name);
                    logger.debug("filename=" + name + " downtype=" + downTypeStr);
                    response.setContentType(downTypeStr);
                    response.addHeader("Content-Disposition", "attachment;filename=" + name + "");
                    File file = new File(fullname);
                    if (file.exists()) {
                        java.io.FileInputStream in = new FileInputStream(file);
                        org.apache.commons.io.IOUtils.copy(in, response.getOutputStream());
                        in.close();
                    }
                }
            } else if ("upload".equalsIgnoreCase(act)) {
                String infoId = request.getParameter("inforId");
                logger.debug("infoId=" + infoId);
            }
        } catch (Exception e) {
        }
    }
}
