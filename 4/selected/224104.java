package gr.demokritos.iit.jinsect.documentModel.documentTypes;

import gr.demokritos.iit.jinsect.documentModel.ITextPrint;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramDistroGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramHistogram;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordDistroGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordHistogram;
import gr.demokritos.iit.jinsect.events.NotificationListener;

/**
 *
 * @author ggianna
 */
public class SimpleTextDistroDocument extends NGramDistroDocument {

    private DocumentWordDistroGraph Graph;

    private DocumentWordHistogram Histogram;

    private String DataString;

    public SimpleTextDistroDocument() {
        Graph = new DocumentWordDistroGraph();
        Histogram = new DocumentWordHistogram();
    }

    public SimpleTextDistroDocument(int iMinNGram, int iMaxNGram, int iDistance) {
        Graph = new DocumentWordDistroGraph();
        Histogram = new DocumentWordHistogram();
    }

    public DocumentNGramHistogram getDocumentHistogram() {
        return Histogram;
    }

    public void setDocumentHistogram(DocumentWordHistogram idnNew) {
        Histogram = idnNew;
    }

    public DocumentNGramDistroGraph getDocumentGraph() {
        return Graph;
    }

    public void setDocumentGraph(DocumentWordDistroGraph idgNew) {
        Graph = idgNew;
    }

    /***
     *Returns the size of the full Document Object, by summing the Graph and
     *Histogram sizes of the document.
     ***/
    public int length() {
        return Histogram.length() + Graph.length();
    }

    public void setTempDataString(String sDataString) {
        DataString = sDataString;
    }

    public void applyTempDataString() {
        if (DataString != null) setDataString(DataString);
    }

    public String getTempDataString() {
        return DataString;
    }

    public void loadTempDataStringFromFile(String sFilename) {
        try {
            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
            FileInputStream fiIn = new FileInputStream(sFilename);
            int iData = 0;
            while ((iData = fiIn.read()) > -1) bsOut.write(iData);
            String sDataString = bsOut.toString();
            fiIn.close();
            DataString = sDataString;
        } catch (IOException e) {
            DataString = "";
        }
    }

    public void loadDataStringFromFile(String sFilename) {
        try {
            Histogram.loadDataStringFromFile(sFilename);
            Graph.loadDataStringFromFile(sFilename);
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
            Histogram.setDataString("");
            Graph.setDataString("");
        }
    }

    public void setDataString(String sDataString) {
        Histogram.setDataString(sDataString);
        Graph.setDataString(sDataString);
    }

    public String getDataString() {
        return Histogram.getDataString();
    }

    public void mergeWith(ITextPrint tpData, double fLearningRate) {
        Histogram.mergeHistogram(tpData.getDocumentHistogram(), fLearningRate);
        Graph.mergeGraph(tpData.getDocumentGraph(), fLearningRate);
    }

    public void prune(double dMinCoexistenceImportance) {
        Graph.prune(dMinCoexistenceImportance);
    }

    public void prune(double dMinCoexistenceImportance, NotificationListener nlDeletionListener) {
        Graph.DeletionNotificationListener = nlDeletionListener;
        Graph.prune(dMinCoexistenceImportance);
        Graph.DeletionNotificationListener = null;
    }
}
