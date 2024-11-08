package edu.ucla.mbi.curator.actions.curator.ajax;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.tree.TreeNode;
import edu.ucla.mbi.xml.MIF.elements.controlledVocabularies.ControlledVocabularyTerm;
import edu.ucla.mbi.xml.MIF.elements.controlledVocabularies.MolecularInteractions.MIVocabTree;
import edu.ucla.mbi.xml.MIF.elements.referencing.ExperimentReferrer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.io.OutputStream;
import java.io.StringBufferInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: jason
 * Date: Feb 13, 2006
 * Time: 3:32:53 PM
 */
public class UpdateVocabBrowser extends Action {

    private static Log log = LogFactory.getLog(UpdateVocabBrowser.class);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String side = request.getParameter("side");
        ControlledVocabularyTerm currentTerm = null;
        String currentTermId = request.getParameter("termId");
        String parentName = request.getParameter("parentName");
        String parentId = request.getParameter("parentId");
        String lastTermId = request.getParameter("lastTermId");
        String useRoot = request.getParameter("useRoot");
        if (useRoot != null && useRoot.trim().length() > 0 && currentTermId != null && !useRoot.equalsIgnoreCase("blah")) {
            Enumeration<ControlledVocabularyTerm> e = null;
            if (useRoot.equals("interactionDetectionMethod")) {
                e = MIVocabTree.getInteractionDetectionMethodRoot().depthFirstEnumeration();
            } else if (useRoot.equals("featureDetectionMethod")) {
                e = MIVocabTree.getFeatureDetectionMethodRoot().depthFirstEnumeration();
            } else if (useRoot.equals("participantIdentificationMethod")) {
                e = MIVocabTree.getParticipantIDMethodRoot().depthFirstEnumeration();
            } else if (useRoot.equals("featureType")) {
                e = MIVocabTree.getFeatureTypeRoot().depthFirstEnumeration();
            } else if (useRoot.equals("experimentalRole")) {
                e = MIVocabTree.getExptlRoleRoot().depthFirstEnumeration();
            } else if (useRoot.equalsIgnoreCase("experimentPreparation")) {
                e = MIVocabTree.getExptlPreparationRoot().depthFirstEnumeration();
            }
            while (e != null && e.hasMoreElements()) {
                ControlledVocabularyTerm cvTerm = e.nextElement();
                if (cvTerm.getTermId().equals(currentTermId)) {
                    currentTerm = cvTerm;
                    break;
                }
            }
        } else if (currentTermId != null) currentTerm = MIVocabTree.getInstance().findOriginalByTermId(currentTermId);
        ArrayList<ControlledVocabularyTerm> leftside, rightside;
        ControlledVocabularyTerm localRoot;
        if (currentTerm != null) {
            localRoot = MIVocabTree.getInstance().findOriginalByTermId(currentTerm.getLocalRootTermIdentifer());
            request.setAttribute("localRoot", localRoot);
            if (currentTerm.getChildren().size() > 0) {
                leftside = ((ControlledVocabularyTerm) currentTerm.getParent()).getChildren();
                rightside = currentTerm.getChildren();
            } else if (currentTerm.getChildren().size() == 0 && currentTerm.getParent() != localRoot) {
                leftside = ((ControlledVocabularyTerm) currentTerm.getParent().getParent()).getChildren();
                rightside = ((ControlledVocabularyTerm) currentTerm.getParent()).getChildren();
            } else if (currentTerm.getChildren().size() == 0 && currentTerm.getParent() == localRoot) {
                leftside = localRoot.getChildren();
                rightside = new ArrayList<ControlledVocabularyTerm>();
            } else throw new NullPointerException("Like the Iraq war, this never should have happened.");
        } else {
            localRoot = MIVocabTree.getRoot();
            request.setAttribute("localRoot", localRoot);
            currentTerm = localRoot;
            leftside = localRoot.getChildren();
            rightside = new ArrayList<ControlledVocabularyTerm>();
            assert currentTerm.getChildren().size() > 0;
            currentTermId = currentTerm.getChildren().get(0).getTermId();
        }
        TreeNode[] path = currentTerm.getPath();
        ControlledVocabularyTerm[] displayPath = null;
        if (path.length > 2) {
            displayPath = new ControlledVocabularyTerm[path.length - 2];
            System.arraycopy(path, 2, displayPath, 0, displayPath.length);
        } else displayPath = new ControlledVocabularyTerm[] {};
        String index = request.getParameter("index");
        if (index == null) index = "0";
        String responseString = "<response>\n";
        responseString += "\t<vocabUpdate>\n" + "\t<targetIndex>" + index + "</targetIndex>\n" + "\t<localRootName>" + localRoot.getTerm() + "</localRootName>\n" + "\t<lastTermId>" + lastTermId + "</lastTermId>\n";
        if (parentName != null && parentName.trim().length() > 0) {
            responseString += "\t<parentName>" + parentName + "</parentName>\n";
            if (parentName.toLowerCase().startsWith("interactor")) responseString += "\t<hiddenField>interactorTypeTermId</hiddenField>\n"; else if (parentName.toLowerCase().startsWith("interaction")) responseString += "\t<hiddenField>interactionTypeTermId</hiddenField>\n";
        }
        if (parentId != null && parentId.trim().length() > 0) responseString += "\t<parentId>" + parentId + "</parentId>\n";
        if (useRoot != null && useRoot.trim().length() > 0) responseString += "\t<useRoot>" + useRoot + "</useRoot>\n";
        responseString += "\t<crumbs>\n";
        for (ControlledVocabularyTerm term : displayPath) {
            responseString += "\t\t<crumb>\n";
            if (term.getTermId().equals(currentTermId)) responseString += "\t\t\t<selected/>\n";
            responseString += "\t\t\t<termId>" + term.getTermId() + "</termId>\n\t\t\t" + "<term>" + term.getTerm() + "</term>" + "\t\t</crumb>\n";
        }
        responseString += "\t</crumbs>\n";
        responseString += "\t<leftside>\n";
        for (ControlledVocabularyTerm term : leftside) {
            responseString += "\t\t<item>\n";
            if (term.getTermId().equals(currentTermId)) responseString += "\t\t\t<selected/>\n"; else if (side != null && !side.equalsIgnoreCase("left") && term.getChildren().contains(currentTerm)) responseString += "\t\t\t<selected/>\n";
            if (term.getChildren().size() > 0) responseString += "\t\t\t<parent/>\n";
            responseString += "\t\t\t<termId>" + term.getTermId() + "</termId>\n\t\t\t" + "<term>" + term.getTerm() + "</term>" + "\n\t\t</item>\n";
        }
        responseString += "\t</leftside>\n";
        responseString += "\t<rightside>\n";
        for (ControlledVocabularyTerm term : rightside) {
            responseString += "\t\t<item>\n";
            if (term.getTermId().equals(currentTermId)) responseString += "\t\t\t<selected/>\n";
            if (term.getChildren().size() > 0) responseString += "\t\t\t<parent/>\n";
            responseString += "\t\t\t<termId>" + term.getTermId() + "</termId>\n\t\t\t" + "<term>" + term.getTerm() + "</term>" + "\n\t\t</item>\n";
        }
        responseString += "\t</rightside>\n";
        responseString += "</vocabUpdate></response>\n";
        sendResponse(response, responseString.toString());
        return null;
    }

    private void sendResponse(HttpServletResponse response, String responseString) throws Exception {
        OutputStream out = response.getOutputStream();
        responseString = stripNewlinesAndTabs(responseString);
        StringBufferInputStream in = new StringBufferInputStream(responseString);
        response.setContentType("text/xml;charset=utf-8");
        byte[] buffer = new byte[8192];
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
