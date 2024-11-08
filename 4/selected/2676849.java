package edu.ucla.mbi.curator.actions.curator.interaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.Action;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.ucla.mbi.curator.webutils.model.InteractionModel;
import edu.ucla.mbi.curator.webutils.model.ExperimentModel;
import edu.ucla.mbi.curator.webutils.session.SessionManager;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.Interaction;
import edu.ucla.mbi.xml.MIF.elements.topLevelElements.Experiment;
import edu.ucla.mbi.xml.MIF.elements.MIFElement;
import edu.ucla.mbi.xml.MIF.elements.controlledVocabularies.MolecularInteractions.ParticipantIdentificationMethod;
import edu.ucla.mbi.xml.MIF.elements.controlledVocabularies.ControlledVocabularyTerm;
import edu.ucla.mbi.xml.MIF.elements.FeatureDescriptionElements.Feature;
import edu.ucla.mbi.xml.MIF.elements.referencing.InternalReference;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: jason
 * Date: Mar 1, 2006
 * Time: 10:42:42 AM
 */
public class GetAvailableExperimentsForInteraction extends Action {

    private Log log = LogFactory.getLog(GetAvailableExperimentsForInteraction.class);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ExperimentModel experimentModel = SessionManager.getExperimentModel(mapping, form, request, response);
        InteractionModel interactionModel = SessionManager.getInteractionModel(mapping, form, request, response);
        Integer interactionId = Integer.valueOf(request.getParameter("interactionId"));
        Interaction interaction = interactionModel.getInteractionByInternalId(interactionId);
        ArrayList<Experiment> available = new ArrayList<Experiment>();
        available.addAll(experimentModel.getExperiments());
        for (MIFElement mifElement : interaction.getExperimentAndInternalReferenceList()) {
            Experiment experiment = (mifElement instanceof Experiment) ? (Experiment) mifElement : (Experiment) ((InternalReference) mifElement).getReferent();
            available.remove(experiment);
        }
        String responseString = "\n<response>\n" + "\t<elementId>experiments</elementId>\n" + "\t<availableExperiments>\n";
        for (Experiment experiment : available) {
            String idm = (experiment.getInteractionDetectionMethod() != null) ? experiment.getInteractionDetectionMethod().getTerm() : getResources(request).getMessage("none");
            responseString += "\t\t<listItem>\n" + "\t\t\t<experiment>" + "\t\t\t\t<name>" + experiment.toString() + "</name>\n" + "\t\t\t\t<experimentId>" + experiment.getInternalReference().getReference() + "</experimentId>\n" + "\t\t\t\t<interactionDetectionMethod>" + idm + "</interactionDetectionMethod>\n" + "\t\t\t</experiment>\n" + "\t\t</listItem>\n";
        }
        responseString += "\t</availableExperiments>\n</response>\n";
        sendResponse(response, responseString);
        return null;
    }

    private String getRelatedExperimentalFeatures(Interaction interaction, Experiment experiment, ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String str = "";
        TreeSet<Feature> relatedFeatures = new TreeSet<Feature>(new FeatureComparator());
        if (relatedFeatures.size() > 0) {
            str += "<relatedfeatures>";
            for (Feature feature : relatedFeatures) {
                str += "<feature><name>" + feature.toString() + "</name><featureId>" + feature.getInternalReference().getReference() + "</featureId></feature>";
            }
            str += "</relatedfeatures>";
        }
        return str;
    }

    private String getRelatedParticipantIdentificationMethods(Interaction interaction, Experiment experiment) {
        String str = "";
        TreeSet<ParticipantIdentificationMethod.ParticipantIdentificationMethodTerm> relatedFeatures = new TreeSet<ParticipantIdentificationMethod.ParticipantIdentificationMethodTerm>(new FeatureComparator());
        if (relatedFeatures.size() > 0) {
            str = "<relatedParticipantIdentificationMethods>";
            for (ParticipantIdentificationMethod.ParticipantIdentificationMethodTerm pidmTerm : relatedFeatures) {
                str += "<participantIdentificationMethods><term>" + pidmTerm.getTerm() + "</term><termId>" + pidmTerm.getTermId() + "</termId></participantIdentificationMethods>";
            }
            str += "</relatedParticipantIdentificationMethods>";
        }
        return str;
    }

    private void sendResponse(HttpServletResponse response, String responseString) throws Exception {
        OutputStream out = response.getOutputStream();
        responseString = stripNewlinesAndTabs(responseString);
        StringBufferInputStream in = new StringBufferInputStream(responseString);
        response.setContentType("text/xml;charset=utf-8");
        byte[] buffer = new byte[131072];
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

class FeatureComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        Feature f1 = (Feature) o1;
        Feature f2 = (Feature) o2;
        return Integer.valueOf(f1.getInternalReference().getReference()).compareTo(f2.getInternalReference().getReference());
    }
}

class CVTermComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        ControlledVocabularyTerm t1 = (ControlledVocabularyTerm) o1;
        ControlledVocabularyTerm t2 = (ControlledVocabularyTerm) o2;
        return t1.getTermId().compareTo(t2.getTermId());
    }
}
