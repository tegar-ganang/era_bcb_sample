package edu.ucla.mbi.curator.actions.curator.interaction;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.ucla.mbi.curator.webutils.model.InteractionModel;
import edu.ucla.mbi.curator.webutils.session.SessionManager;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.Interaction;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.Experiment;
import edu.ucla.mbi.xml.MIF.elements.MIFElement;
import edu.ucla.mbi.xml.MIF.elements.controlledVocabularies.HostOrganism;
import edu.ucla.mbi.xml.MIF.elements.referencing.InternalReference;
import edu.ucla.mbi.xml.MIF.elements.interactionElements.Participant;
import edu.ucla.mbi.xml.MIF.elements.interactionElements.ParticipantList;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: jason
 * Date: Mar 1, 2006
 * Time: 10:30:32 AM
 */
public class GetIncludedExperimentsForInteraction extends Action {

    private Log log = LogFactory.getLog(GetIncludedExperimentsForInteraction.class);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        InteractionModel interactionModel = SessionManager.getInteractionModel(mapping, form, request, response);
        Integer interactionId = Integer.valueOf(request.getParameter("interactionId"));
        Interaction interaction = interactionModel.getInteractionByInternalId(interactionId);
        String responseString = "\n<response>\n" + "\t<elementId>experiments</elementId>\n" + "\t<includedExperiments>\n";
        ParticipantList participantList = new ParticipantList();
        participantList.addAll(interaction.getParticipants());
        for (MIFElement mifElement : interaction.getExperimentAndInternalReferenceList()) {
            Experiment experiment = (mifElement instanceof Experiment) ? (Experiment) mifElement : (Experiment) ((InternalReference) mifElement).getReferent();
            String idm = (experiment.getInteractionDetectionMethod() != null) ? experiment.getInteractionDetectionMethod().getTerm() : getResources(request).getMessage("none");
            responseString += "\t\t<experiment>\n" + "\t\t\t<name>" + experiment.toString() + "</name>\n" + "\t\t\t<experimentId>" + experiment.getInternalReference().getReference() + "</experimentId>\n" + "\t\t\t<numHostOrgs>" + experiment.getHostOrganismList().size() + "</numHostOrgs>\n" + "\t\t\t<interactionDetectionMethod>" + idm + "</interactionDetectionMethod>\n" + "\t\t\t<hostOrganisms>" + getHostOrganismsResponse(experiment) + "</hostOrganisms>\n" + "\t\t\t<removeTerm>" + getResources(request).getMessage("experimentswidget.label.remove") + "</removeTerm>\n" + "\t\t\t<participants>" + participantList.getRelativeParticipantList(experiment) + "</participants>\n" + "\t\t</experiment>\n";
        }
        responseString += "\t</includedExperiments>\n</response>\n";
        sendResponse(response, responseString);
        return null;
    }

    private String getHostOrganismsResponse(Experiment experiment) {
        String r = "";
        for (Object hostOrganism : experiment.getHostOrganismList()) {
            r += "\t\t\t\t<organism>\n" + "\t\t\t\t\t<name>" + hostOrganism.toString() + "</name>\n" + "\t\t\t\t\t<taxId>" + ((HostOrganism) hostOrganism).getNcbiTaxId() + "</taxId>" + "\t\t\t\t</organism>\n";
        }
        return r;
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
