package edu.ucla.mbi.curator.actions.curator.experiment;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import edu.ucla.mbi.curator.webutils.session.SessionManager;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.ExperimentBuilder;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.Experiment;

/**
 * Created by IntelliJ IDEA.
 * User: jason
 * Date: Feb 28, 2006
 * Time: 5:28:15 PM
 */
public class CreateExperiment extends Action {

    private Log log = LogFactory.getLog(CreateExperiment.class);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        SessionManager sessionManager = SessionManager.getSessionManager(request);
        ExperimentBuilder experimentBuilder = sessionManager.getFileFactory().getExperimentBuilder();
        experimentBuilder.createExperiment();
        String name = "Expt " + sessionManager.getNextExptIndex();
        experimentBuilder.setFullName(name);
        Experiment experiment = experimentBuilder.getExperiment();
        if (sessionManager.getDefaultPubmedId() != null && sessionManager.getDefaultPubmedId().trim().length() > 0) experimentBuilder.setPubmedBibref(sessionManager.getDefaultPubmedId());
        sessionManager.getExperimentModel().addExperiment(experiment);
        sessionManager.setRecentExperiment(experiment);
        String responseString = "";
        responseString += "\n<response>\n" + "\t<newExperiment>\n" + "\t\t<experimentId>" + experiment.getInternalReference().getReference() + "</experimentId>\n" + "\t\t<name>" + name + "</name>\n" + "\t</newExperiment>\n" + "</response>\n";
        sendResponse(response, responseString);
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
