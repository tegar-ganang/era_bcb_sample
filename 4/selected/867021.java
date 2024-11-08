package edu.ucla.mbi.curator.actions.curator.interaction;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import edu.ucla.mbi.curator.webutils.session.SessionManager;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.InteractionBuilder;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.Interaction;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.Experiment;

/**
 * Created by IntelliJ IDEA.
 * User: jason
 * Date: Mar 3, 2006
 * Time: 3:38:38 PM
 */
public class RemoveExperimentFromInteraction extends Action {

    private Log log = LogFactory.getLog(AddExperimentsToInteraction.class);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        SessionManager sessionManager = SessionManager.getSessionManager(request);
        InteractionBuilder interactionBuilder = sessionManager.getFileFactory().getInteractionBuilder();
        Interaction interaction = sessionManager.getInteractionModel().getInteractionByInternalId(Integer.valueOf(request.getParameter("interactionId")));
        interactionBuilder.setInteraction(interaction);
        Experiment experiment = sessionManager.getExperimentModel().getExperimentByInternalId(Integer.valueOf(request.getParameter("experimentId")));
        interactionBuilder.removeExperiment(experiment);
        return null;
    }

    private void sendResponse(HttpServletResponse response, String responseString) throws Exception {
        OutputStream out = response.getOutputStream();
        responseString = stripNewlinesAndTabs(responseString);
        StringBufferInputStream in = new StringBufferInputStream(responseString);
        response.setContentType("text/xml;charset=utf-8");
        byte[] buffer = new byte[2048];
        int count = 0;
        while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
        out.close();
    }

    private String stripNewlinesAndTabs(String str) {
        char[] buf = str.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : buf) {
            if (c != '\t' && c != '\n') sb.append(c);
        }
        return sb.toString();
    }
}
