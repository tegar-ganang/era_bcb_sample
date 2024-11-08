package elapse.domain;

import com.google.inject.Inject;
import elapse.gui.ElapseFrame;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.TransformerHandler;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: maarten
 * Date: 28/04/11
 * Time: 18:12
 * To change this template use File | Settings | File Templates.
 */
public class LogBookSourceXML implements LogBookSource, LogBookImportExport {

    String tutorialFilename;

    private LogBook logBook;

    private String applicationFolder;

    private Settings.Source settingsSource;

    @Inject
    public LogBookSourceXML(@Annotations.ApplicationFolder String applicationFolder, Settings.Source settingsSource) {
        this.applicationFolder = applicationFolder;
        this.settingsSource = settingsSource;
    }

    public LogBook getLogBook() {
        return logBook;
    }

    public boolean open() {
        try {
            this.tutorialFilename = copyTutorial();
            this.logBook = doImport(tutorialFilename);
            return true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public void save() {
    }

    public void close() {
        new File(getFilenameForCopiedTutorial()).delete();
        logBook = null;
    }

    public String getLocationDescriptor() {
        return tutorialFilename;
    }

    public void setLogBook(LogBook logBook) {
        this.logBook = logBook;
    }

    public LogBook doImport(String filename) {
        LogBookXML logBookXML = new LogBookXML();
        try {
            File f = new File(filename);
            if (f.exists()) {
                FileReader r = new FileReader(filename);
                logBook = logBookXML.read(new InputSource(r));
                return logBook;
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public boolean doExport(LogBook logBook, String filename) {
        if (logBook == null) {
            return false;
        }
        synchronized (logBook) {
            try {
                TransformerHandler hd = XMLUtils.createTransformerHandler(filename);
                hd.startDocument();
                LogBookXML.write(logBook, hd);
                hd.endDocument();
                return true;
            } catch (SAXException ex) {
                Logger.getLogger(ElapseFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerConfigurationException ex) {
                Logger.getLogger(ElapseFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerFactoryConfigurationError ex) {
                Logger.getLogger(ElapseFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ElapseFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ElapseFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    private String copyTutorial() throws IOException {
        File inputFile = new File(getFilenameForOriginalTutorial());
        File outputFile = new File(getFilenameForCopiedTutorial());
        FileReader in = new FileReader(inputFile);
        FileWriter out = new FileWriter(outputFile);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
        return getFilenameForCopiedTutorial();
    }

    String getFilenameForOriginalTutorial() {
        return applicationFolder + "/resource/tutorial.ell";
    }

    String getFilenameForCopiedTutorial() {
        return settingsSource.getSettings().userFolder + "/tutorial(copy).ell";
    }
}
