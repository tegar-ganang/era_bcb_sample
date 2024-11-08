package org.ogce.expbuilder.phase;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ogce.expbuilder.bean.WorkflowBean;
import org.ogce.expbuilder.context.ApplicationGlobalContext;
import org.ogce.expbuilder.model.ExpBldrWorkflow;
import org.ogce.expbuilder.tools.XBayaWorkflowUtil;

public class ImagePhaseListener implements PhaseListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = -6427274284596842138L;

    public static final String IMAGE_VIEW_ID = "workflowimage";

    public static final String WORKFLOW_ID = "wf_id";

    public static final String DEFAULT_IMAGE_URL = "/pages/images/workflow-img-na.jpg";

    private ExpBldrWorkflow workflow;

    private static final Log log = LogFactory.getLog(ImagePhaseListener.class);

    public void afterPhase(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        String viewId = context.getViewRoot().getViewId();
        if (viewId.indexOf(IMAGE_VIEW_ID) != -1) {
            log.info("Handling image request");
            handleImageRequest(context);
        }
    }

    public void beforePhase(PhaseEvent event) {
    }

    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    private void handleImageRequest(FacesContext context) {
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        response.setContentType("image/jpeg");
        Application application = context.getApplication();
        ApplicationGlobalContext appContext = (ApplicationGlobalContext) application.createValueBinding("#{applicationGlobalContext}").getValue(context);
        if (appContext == null) {
            String message = "<h1>Your tomcat server.xml must have emptySessionPath=\"true\", please set the property at your non-SSL HTTP/1.1 Connector</h1>";
            try {
                response.getWriter().write(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String workflowID = request.getParameter("id");
        try {
            WorkflowBean bean = new WorkflowBean();
            this.workflow = appContext.getExpBuilderManager().getWorkflow(workflowID);
            bean.setWorkflow(workflow);
            ValueBinding valueBinding = application.createValueBinding("#{workflow}");
            valueBinding.setValue(context, bean);
            BufferedImage buffer = workflow.getWorkflowImage();
            response.setContentType("image/png");
            if (buffer == null) {
                getDefaultImage(context, response);
            }
            OutputStream out = response.getOutputStream();
            ImageIO.write(buffer, "png", out);
            out.close();
        } catch (Exception e) {
            try {
                getDefaultImage(context, response);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        context.responseComplete();
    }

    public void getDefaultImage(FacesContext context, HttpServletResponse response) throws IOException {
        InputStream defaultImage = context.getExternalContext().getResourceAsStream(DEFAULT_IMAGE_URL);
        response.setContentType("image/jpeg");
        OutputStream out = response.getOutputStream();
        ImageIO.write(ImageIO.read(defaultImage), "jpeg", out);
        out.close();
        return;
    }

    public ExpBldrWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(ExpBldrWorkflow workflow) {
        this.workflow = workflow;
    }
}
