package aurora;

import java.awt.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/**
 * Class for program settings description.
 * @author Alex Kurzhanskiy
 * @version $Id: SimulationSettings.java 96 2010-04-25 21:00:27Z akurzhan $
 */
public class SimulationSettings implements AuroraConfigurable, Serializable {

    private static final long serialVersionUID = -5690805735719405L;

    protected static PrintStream outputStream = System.out;

    protected static PrintStream errorStream = System.err;

    protected static Dimension windowSize = new Dimension();

    protected File tmpDataFile = null;

    protected PrintStream tmpDataOutput = null;

    protected double displayTP = 1.0 / 12.0;

    protected double timeMax = 24;

    protected double timeInitial = 0;

    protected int tsMax = 100000;

    protected int tsInitial = 0;

    protected int timeout = 1000;

    protected boolean isPred = false;

    /**
	 * Initializes settings from given DOM structure.
	 * @param p DOM node.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise.
	 * @throws ExceptionConfiguration
	 */
    public boolean initFromDOM(Node p) throws ExceptionConfiguration {
        boolean res = true;
        if (p == null) return !res;
        try {
            if (p.hasChildNodes()) {
                NodeList pp = p.getChildNodes();
                for (int i = 0; i < pp.getLength(); i++) {
                    if (pp.item(i).getNodeName().equals("display")) {
                        Node att = pp.item(i).getAttributes().getNamedItem("tp");
                        if (att != null) displayTP = Math.max(0, Double.parseDouble(att.getNodeValue()));
                        att = pp.item(i).getAttributes().getNamedItem("timeout");
                        if (att != null) timeout = Math.max(0, Integer.parseInt(att.getNodeValue()));
                        att = pp.item(i).getAttributes().getNamedItem("tsMax");
                        if (att != null) tsMax = Math.max(0, Integer.parseInt(att.getNodeValue()));
                        att = pp.item(i).getAttributes().getNamedItem("timeMax");
                        if (att != null) timeMax = Math.max(0, Double.parseDouble(att.getNodeValue()));
                        att = pp.item(i).getAttributes().getNamedItem("tsInitial");
                        if (att != null) tsInitial = Math.max(0, Integer.parseInt(att.getNodeValue()));
                        att = pp.item(i).getAttributes().getNamedItem("timeInitial");
                        if (att != null) timeInitial = Math.max(0, Double.parseDouble(att.getNodeValue()));
                        att = pp.item(i).getAttributes().getNamedItem("mode");
                        if (att != null) isPred = att.getNodeValue().toUpperCase().equals("P");
                    }
                    if (pp.item(i).getNodeName().equals("include")) {
                        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pp.item(i).getAttributes().getNamedItem("uri").getNodeValue());
                        initFromDOM(doc.getChildNodes().item(0));
                    }
                }
            } else res = false;
        } catch (Exception e) {
            res = false;
            throw new ExceptionConfiguration(e.getMessage());
        }
        return res;
    }

    /**
	 * Generates XML description of the application settings.<br>
	 * If the print stream is specified, then XML buffer is written to the stream.
	 * @param out print stream.
	 * @throws IOException
	 */
    public void xmlDump(PrintStream out) throws IOException {
        if (out == null) out = System.out;
        String modeBuf = "";
        if (isPred) modeBuf = " mode=\"P\"";
        out.print("<display tp=\"" + Double.toString(displayTP) + "\" timeout=\"" + Integer.toString(timeout) + "\" tsMax=\"" + Integer.toString(tsMax) + "\" timeMax=\"" + Double.toString(timeMax) + "\" tsInitial=\"" + Integer.toString(tsInitial) + "\" timeInitial=\"" + Double.toString(timeInitial) + "\"" + modeBuf + " />\n");
        return;
    }

    /**
	 * Bogus function that always returns <code>true</code>.
	 */
    public boolean validate() throws ExceptionConfiguration {
        return true;
    }

    /**
	 * Returns <code>true</code> if current application runs simulation with prediction, <code>false</code> - otherwise.
	 */
    public final boolean isPrediction() {
        return isPred;
    }

    /**
	 * Returns output stream.
	 */
    public PrintStream getOutputStream() {
        return outputStream;
    }

    /**
	 * Returns error stream.
	 */
    public PrintStream getErrorStream() {
        return errorStream;
    }

    /**
	 * Returns window size.
	 */
    public Dimension getWindowSize() {
        return windowSize;
    }

    /**
	 * Returns display update period
	 */
    public double getDisplayTP() {
        return displayTP;
    }

    /**
	 * Returns maximum simulation time.
	 */
    public double getTimeMax() {
        return timeMax;
    }

    /**
	 * Returns initial simulation time.
	 */
    public double getTimeInitial() {
        return timeInitial;
    }

    /**
	 * Returns maximum simulation step.
	 */
    public int getTSMax() {
        return tsMax;
    }

    /**
	 * Returns initial simulation step.
	 */
    public int getTSInitial() {
        return tsInitial;
    }

    /**
	 * Returns timeout between updates.
	 */
    public int getTimeout() {
        return timeout;
    }

    /**
	 * Returns temporary data file pointer.
	 */
    public File getTmpDataFile() {
        return tmpDataFile;
    }

    /**
	 * Returns temporary data output stream.
	 */
    public PrintStream getTmpDataOutput() {
        return tmpDataOutput;
    }

    /**
	 * Creates new data header.
	 * @param fpath directory for temporary file.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise.
	 */
    public synchronized boolean createDataHeader() {
        if (tmpDataOutput == null) return false;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        tmpDataOutput.print(dateFormat.format(new Date()) + "\n\n");
        return true;
    }

    /**
	 * Creates new temporary data file.
	 * @param fpath directory for temporary file.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise.
	 */
    public synchronized boolean createNewTmpDataFile(File fpath) throws IOException {
        if (tmpDataOutput != null) tmpDataOutput.close();
        tmpDataOutput = null;
        if (tmpDataFile != null) {
            tmpDataFile.delete();
        }
        tmpDataFile = File.createTempFile(".AuroraRNM", "Simulation.tmp", fpath);
        tmpDataFile.deleteOnExit();
        tmpDataOutput = new PrintStream(new FileOutputStream(tmpDataFile.getAbsolutePath()));
        return true;
    }

    /**
	 * Sets temporary data file.
	 * @param tmpFile temporary data file.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise.
	 */
    public synchronized boolean setTmpDataFile(File tmpFile) throws IOException {
        if (tmpFile == null) return false;
        if (tmpFile.exists()) tmpFile.delete();
        if (tmpDataOutput != null) tmpDataOutput.close();
        tmpDataOutput = null;
        if (tmpDataFile != null) {
            tmpDataFile.delete();
        }
        tmpDataFile = tmpFile;
        tmpDataOutput = new PrintStream(new FileOutputStream(tmpDataFile.getAbsolutePath()));
        return true;
    }

    /**
	 * Copies temporary data file to a destination.
	 * @param fpath new name.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise.
	 */
    public synchronized boolean copyTmpDataFile(String fpath) throws IOException {
        if (tmpDataOutput != null) tmpDataOutput.close();
        tmpDataOutput = null;
        if (tmpDataFile == null) return false;
        File nfp = new File(fpath);
        if (nfp.exists()) nfp.delete();
        FileInputStream src = new FileInputStream(tmpDataFile);
        FileOutputStream dst = new FileOutputStream(nfp);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = src.read(buffer)) != -1) dst.write(buffer, 0, bytesRead);
        src.close();
        dst.close();
        return true;
    }

    /**
	 * Deletes temporary data file.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise.
	 */
    public synchronized boolean deleteTmpDataFile() throws IOException {
        if (tmpDataOutput != null) tmpDataOutput.close();
        tmpDataOutput = null;
        if (tmpDataFile == null) return false;
        tmpDataFile.delete();
        tmpDataFile = null;
        return true;
    }

    /**
	 * Establish data output stream.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise.
	 */
    public synchronized boolean establishDataOutput() throws IOException {
        if (tmpDataFile == null) return false;
        tmpDataOutput = new PrintStream(new FileOutputStream(tmpDataFile.getAbsolutePath(), true));
        return true;
    }

    /**
	 * Sets output stream.
	 * @param os output stream.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise. 
	 */
    public synchronized boolean setOutputStream(PrintStream os) {
        if (os == null) return false;
        outputStream = os;
        return true;
    }

    /**
	 * Sets error stream.
	 * @param es error stream.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise. 
	 */
    public synchronized boolean setErrorStream(PrintStream es) {
        if (es == null) return false;
        errorStream = es;
        return true;
    }

    /**
	 * Sets the window size.
	 * @param dims window size.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise. 
	 */
    public synchronized boolean setWindowSize(Dimension dims) {
        if (dims == null) return false;
        windowSize = dims;
        return true;
    }

    /**
	 * Sets display update period.
	 * @param tp time period.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise. 
	 */
    public synchronized boolean setDisplayTP(double tp) {
        if (tp <= 0.0) return false;
        displayTP = tp;
        return true;
    }

    /**
	 * Sets max simulation time.
	 * @param tmax max time value.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise. 
	 */
    public synchronized boolean setTimeMax(double tmax) {
        if (tmax <= 0.0) return false;
        timeMax = tmax;
        return true;
    }

    /**
	 * Sets initial simulation time.
	 * @param tinit initial time value.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise. 
	 */
    public synchronized boolean setTimeInitial(double tinit) {
        if (tinit <= 0.0) return false;
        timeInitial = tinit;
        return true;
    }

    /**
	 * Sets max simulation step.
	 * @param ts max step value.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise. 
	 */
    public synchronized boolean setTSMax(int ts) {
        if (ts <= 0) return false;
        tsMax = ts;
        return true;
    }

    /**
	 * Sets initial simulation step.
	 * @param ts initial step value.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise. 
	 */
    public synchronized boolean setTSInitial(int ts) {
        if (ts <= 0) return false;
        tsInitial = ts;
        return true;
    }

    /**
	 * Sets timeout between updates.
	 * @param dt timeout in milliseconds.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise. 
	 */
    public synchronized boolean setTimeout(int dt) {
        if (dt <= 0) return false;
        timeout = dt;
        return true;
    }

    /**
	 * Set application type to simulation with prediction.
	 */
    public synchronized void setPrediction(boolean x) {
        isPred = x;
        return;
    }

    /**
	 * Copies data from the given settings object to the current one.
	 * @param x given settings object.
	 * @return <code>true</code> if successful, <code>false</code> - otherwise.
	 */
    public synchronized boolean copyData(SimulationSettings x) {
        if (x == null) return false;
        displayTP = x.getDisplayTP();
        timeMax = x.getTimeMax();
        tsMax = x.getTSMax();
        timeInitial = x.getTimeInitial();
        tsInitial = x.getTSInitial();
        timeout = x.getTimeout();
        outputStream = x.getOutputStream();
        errorStream = x.getErrorStream();
        isPred = x.isPrediction();
        return true;
    }
}
