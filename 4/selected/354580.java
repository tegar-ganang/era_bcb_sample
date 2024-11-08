package org.ogce.expbuilder.servlets;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import javax.portlet.PortletRequest;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.ogce.expbuilder.context.ApplicationGlobalContext;
import org.ogce.expbuilder.model.ExpBldrWorkflow;
import org.ogce.expbuilder.tools.XBayaWorkflowUtil;
import xportlets.proxymanager.ProxyManager;

public class WorkflowImageLoaderServlet extends HttpServlet {

    /**
     * Request parameter.
     */
    public static final String IMAGE_TYPE = "image_type";

    /**
     * Request parameter.
     */
    public static final String WORKFLOW_URL_CONTEXT_TYPE = "workflow_url_context_type";

    /**
     * Request parameter.
     */
    public static final String WORKFLOW_ID = "wf_id";

    /**
     * {@link #IMAGE_TYPE} option.
     */
    public static final String PNG_IMAGE_TYPE = "png";

    public static final String DEFAULT_IMAGE_URL = "/pages/images/workflow-img-na.jpg";

    public static final String WF_TEMPLATES_CACHE = "WF_TEMPLATES_CACHE";

    public static final String WORKFLOW_MANAGER_ATTR = "WORKFLOW_MANAGER_ATTR";

    public static final String WORKSPACE_MANAGER_ATTR = "WORKSPACE_MANAGER_ATTR";

    private ExpBldrWorkflow workflow;

    public static final Logger log = Logger.getLogger(WorkflowImageLoaderServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("query string = " + request.getQueryString());
        }
        String workflowId = request.getParameter(WORKFLOW_ID);
        try {
            ApplicationGlobalContext context = new ApplicationGlobalContext();
            FileInputStream fis = new FileInputStream(context.getCredentials().getHostcertsKeyFile());
            GlobusCredential globusCred = new GlobusCredential(fis);
            context.setGssCredential(new GlobusGSSCredentialImpl(globusCred, GSSCredential.INITIATE_AND_ACCEPT));
            XBayaWorkflowUtil xRegistryUtil = new XBayaWorkflowUtil();
            this.workflow = xRegistryUtil.getWorkflowFromRegistry(workflowId, context);
            BufferedImage buffer = workflow.getWorkflowImage();
            if (buffer == null) {
                InputStream defaultImage = getServletContext().getResourceAsStream(DEFAULT_IMAGE_URL);
                response.setContentType("image/jpeg");
                OutputStream out = response.getOutputStream();
                ImageIO.write(ImageIO.read(defaultImage), "jpeg", out);
                out.close();
                return;
            }
            response.setContentType("image/png");
            if (buffer == null) {
                InputStream defaultImage = getServletContext().getResourceAsStream(DEFAULT_IMAGE_URL);
                response.setContentType("image/jpeg");
                OutputStream out = response.getOutputStream();
                ImageIO.write(ImageIO.read(defaultImage), "jpeg", out);
                out.close();
            }
            OutputStream out = response.getOutputStream();
            ImageIO.write(buffer, "png", out);
            out.close();
        } catch (Exception e) {
            System.out.println("Default Image");
            InputStream defaultImage = getServletContext().getResourceAsStream(DEFAULT_IMAGE_URL);
            response.setContentType("image/jpeg");
            OutputStream out = response.getOutputStream();
            ImageIO.write(ImageIO.read(defaultImage), "jpeg", out);
            out.close();
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
