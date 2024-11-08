package gate.cloud.batch;

import gate.CorpusController;
import gate.cloud.io.InputHandler;
import gate.cloud.io.OutputHandler;
import gate.cloud.util.Tools;
import gate.util.GateException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.apache.log4j.Logger;

/**
 * Class representing a cloud batch job.
 */
public class Batch {

    /**
   * Constructor, which sets no values. Use the accessor methods to set the 
   * needed values, then call {@link #init()}. 
   */
    public Batch() {
    }

    /**
   * Log4J logger.
   */
    private static Logger logger = Logger.getLogger(Batch.class);

    private static XMLOutputFactory staxOutputFactory = XMLOutputFactory.newInstance();

    private static XMLInputFactory staxInputFactory = XMLInputFactory.newInstance();

    /**
   * Prepares this batch for execution:
   * <ul>
   * <li>check that all the required values have been set</li>
   * <li>opens the {@link XMLStreamWriter} for the report file</li>
   * <li>updates the documentIDs value if the batch is being restarted after a
   * partial execution</li>
   * </ul>
   * If any problems are found, a {@link GateException} is thrown.
   */
    public void init() throws GateException {
        if (reportFile == null) throw new GateException("No report file set!");
        boolean restarting = false;
        if (!reportFile.getParentFile().exists() && !reportFile.getParentFile().mkdirs()) {
            throw new GateException("Could not create directories for " + reportFile.getAbsolutePath());
        }
        File backupFile = new File(reportFile.getAbsolutePath() + ".bak");
        if (reportFile.exists()) {
            restarting = true;
            logger.info("Existing report file found at \"" + reportFile.getAbsolutePath() + "\", attempting to restart");
            if (!reportFile.renameTo(backupFile)) {
                try {
                    byte[] buff = new byte[32 * 1024];
                    InputStream in = new BufferedInputStream(new FileInputStream(reportFile));
                    try {
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(backupFile));
                        try {
                            int read = in.read(buff);
                            while (read != -1) {
                                out.write(buff, 0, read);
                                read = in.read(buff);
                            }
                        } finally {
                            out.close();
                        }
                    } finally {
                        in.close();
                    }
                } catch (IOException e) {
                    throw new GateException("Could not restart batch", e);
                }
            }
        }
        try {
            reportWriter = staxOutputFactory.createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream(reportFile)));
            reportWriter.writeStartDocument();
            reportWriter.writeCharacters("\n");
            reportWriter.setDefaultNamespace(Tools.REPORT_NAMESPACE);
            reportWriter.writeStartElement(Tools.REPORT_NAMESPACE, "cloudReport");
            reportWriter.writeDefaultNamespace(Tools.REPORT_NAMESPACE);
            reportWriter.writeCharacters("\n");
            reportWriter.writeStartElement(Tools.REPORT_NAMESPACE, "documents");
        } catch (XMLStreamException e) {
            throw new GateException("Cannot write to the report file!", e);
        } catch (IOException e) {
            throw new GateException("Cannot write to the report file!", e);
        }
        if (restarting) {
            try {
                Set<String> completedDocuments = new HashSet<String>();
                logger.debug("Processing existing report file");
                InputStream bakIn = new BufferedInputStream(new FileInputStream(backupFile));
                XMLEventReader xer = staxInputFactory.createXMLEventReader(bakIn);
                try {
                    XMLEvent event;
                    while (xer.hasNext()) {
                        event = xer.nextEvent();
                        if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("documents")) {
                            break;
                        }
                    }
                    List<XMLEvent> events = new LinkedList<XMLEvent>();
                    String currentReturnCode = null;
                    String currentDocid = null;
                    while (xer.hasNext()) {
                        event = xer.nextEvent();
                        events.add(event);
                        if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("processResult")) {
                            currentReturnCode = event.asStartElement().getAttributeByName(new QName(XMLConstants.NULL_NS_URI, "returnCode")).getValue();
                            currentDocid = event.asStartElement().getAttributeByName(new QName(XMLConstants.NULL_NS_URI, "id")).getValue();
                        }
                        if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("processResult")) {
                            if (currentReturnCode.equals("SUCCESS") && currentDocid != null) {
                                completedDocuments.add(currentDocid);
                                for (XMLEvent evt : events) {
                                    Tools.writeStaxEvent(evt, reportWriter);
                                }
                            }
                            events.clear();
                            currentReturnCode = null;
                            currentDocid = null;
                        }
                        if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("documents")) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Exception while parsing old report file - probably " + "reached the end of old report", e);
                } finally {
                    xer.close();
                    bakIn.close();
                    backupFile.delete();
                }
                List<String> unprocessedDocs = new ArrayList<String>();
                unprocessedDocs.addAll(Arrays.asList(documentIDs));
                unprocessedDocs.removeAll(completedDocuments);
                unprocessedDocumentIDs = unprocessedDocs.toArray(new String[unprocessedDocs.size()]);
            } catch (XMLStreamException e) {
                throw new GateException("Cannot write to the report file!", e);
            } catch (IOException e) {
                throw new GateException("Cannot write to the report file!", e);
            }
        } else {
            unprocessedDocumentIDs = documentIDs;
        }
    }

    private String batchId;

    private String[] documentIDs;

    private String[] unprocessedDocumentIDs;

    private CorpusController gateApplication;

    private File reportFile;

    private InputHandler inputHandler;

    private List<OutputHandler> outputHandlers;

    private XMLStreamWriter reportWriter;

    /**
   * Gets the ID of the this batch.
   * @return a {@link String} value.
   */
    public String getBatchId() {
        return batchId;
    }

    /**
   * Sets the ID of the this batch.
   */
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    /**
   * Gets the {@link File} object denoting the file where the processing report
   * will be written.
   * @return
   */
    public File getReportFile() {
        return reportFile;
    }

    /**
   * Sets the {@link File} object denoting the file where the processing report
   * will be written.
   */
    public void setReportFile(File reportFile) {
        this.reportFile = reportFile;
    }

    /**
   * Gets the list of output handlers for this batch.
   * @return
   */
    public List<OutputHandler> getOutputHandlers() {
        return outputHandlers;
    }

    /**
   * Sets the list of output handlers for this batch.
   */
    public void setOutputHandlers(List<OutputHandler> outputHandlers) {
        this.outputHandlers = outputHandlers;
    }

    /**
   * Gets the list of input document IDs in this batch.
   * @return an array of {@link String}s.
   */
    public String[] getDocumentIDs() {
        return documentIDs;
    }

    /**
   * Sets the list of input document IDs in this batch.
   */
    public void setDocumentIDs(String[] documentIDs) {
        this.documentIDs = documentIDs;
    }

    /**
   * This can be used to obtain a file object pointing to the saved version of 
   * the GATE application that should be used for processing.
   * @return a {@link File} object.
   */
    public CorpusController getGateApplication() {
        return gateApplication;
    }

    /**
   * Sets the file object pointing to the GATE application to be executed by 
   * this batch. 
   * @param appFile
   */
    public void setGateApplication(CorpusController app) {
        this.gateApplication = app;
    }

    /**
   * Gets the input handler used by this batch.
   * @return a {@link InputHandler} value.
   */
    public InputHandler getInputHandler() {
        return inputHandler;
    }

    /**
   * Sets the input handler used by this batch.
   */
    public void setInputHandler(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    /**
   * Gets the list of output handlers.
   * @return a {@link List} of {@link OutputHandler} objects.
   */
    public List<OutputHandler> getOutputs() {
        return outputHandlers;
    }

    /**
   * This gets an {@link XMLStreamWriter} that writes to the
   * report file for this batch.
   * @return a writer for the report file, positioned ready to
   * write the next entry for a completed or failed processing
   * job.
   * @throws IOException if an I/O error occurs while creating
   * the writer
   * @throws XMLStreamException if a StAX error occurs while
   * creating the writer.
   */
    public XMLStreamWriter getReportWriter() throws IOException, XMLStreamException {
        return reportWriter;
    }

    /**
   * This gets the list of all the documents from this batch that
   * are still to be processed.  For a clean batch this would be the
   * same as {@link #getDocumentIDs()} but for a batch that has
   * been interrupted and restarted the values may be different.
   * @return an array of {@link String}s.
   */
    public String[] getUnprocessedDocumentIDs() {
        return unprocessedDocumentIDs;
    }

    public String toString() {
        return "Batch ID:         " + batchId + "\nInput handler:    " + inputHandler.toString() + "\nOutputs:          " + outputHandlers + "\nGATE Application: " + (gateApplication == null ? "not set" : gateApplication.getName()) + "\nReport file:      " + reportFile + "\nInput documents:        " + (documentIDs == null ? 0 : documentIDs.length) + "\nUnprocessed documents:  " + (unprocessedDocumentIDs == null ? 0 : unprocessedDocumentIDs.length);
    }
}
