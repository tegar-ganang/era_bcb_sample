package burster.explode;

import burster.pageExtractor.Burster;
import burster.sender.Sender;
import burster.sender.factory.SenderFactory;
import burster.settings.Settings;
import burster.utils.Pair;
import burster.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public abstract class Exploder extends PDFTextStripper {

    protected Log log = LogFactory.getLog(Exploder.class);

    protected Writer textWriter = null;

    protected ByteArrayOutputStream textStream = null;

    protected Integer pageIndex = 0;

    protected Hashtable<String, Pair> pagesMap = new Hashtable<String, Pair>();

    private Settings settings;

    private boolean isSendFiles = true;

    private boolean isDeleteFiles = false;

    private String defaultFileName;

    private String outputFolder;

    private String runtimeFolder;

    private String runtimeOutputFolder;

    private String filePath;

    private Vector<String> burstedDocuments = new Vector<String>();

    private SenderFactory senderFactory;

    public Exploder(Settings settings) throws IOException {
        log.trace("Exploder(Settings settings)");
        this.settings = settings;
        outputFolder = settings.getOutputFolder();
        isSendFiles = settings.isSendFiles();
        isDeleteFiles = settings.isDeleteFiles();
        defaultFileName = settings.getDefaultFileName();
        File outFolder = new File(outputFolder);
        if (!outFolder.exists()) {
            throw new FileNotFoundException("Output folder does not exist : " + outputFolder);
        }
        senderFactory = new SenderFactory(settings);
        textStream = new ByteArrayOutputStream();
        textWriter = new OutputStreamWriter(textStream, "UTF-16");
    }

    public void doExplode(String fileToExplodePath) throws IOException {
        log.trace("doExplode(String fileToExplodePath) : fileToExplodePath=" + fileToExplodePath);
        log.info("Checking document: '" + fileToExplodePath + "' for tokens...");
        filePath = fileToExplodePath;
        PDDocument doc = PDDocument.load(filePath);
        writeText(doc, textWriter);
        log.info("Document: '" + fileToExplodePath + "' was completed.");
        doc.close();
    }

    protected void prepare() throws IOException {
        log.trace("prepare()");
        String baseName = FilenameUtils.getName(filePath);
        runtimeFolder = baseName + "/" + Utils.getDateAsString(new Date(), settings.getDateFormat());
        runtimeOutputFolder = outputFolder + "/" + runtimeFolder;
        String runtimeInputFolder = runtimeOutputFolder + "/input";
        File runtimeInputDir = new File(runtimeInputFolder);
        FileUtils.forceMkdir(runtimeInputDir);
        FileUtils.copyFile(new File(filePath), new File(runtimeInputFolder + "/" + baseName));
    }

    protected void endDocument(PDDocument pdf) throws IOException {
        log.trace("endDocument(PDDocument pdf)");
        if (pagesMap.size() == 0) {
            log.warn("No burst tokens were found in document " + filePath);
        } else {
            prepare();
            int sequence = 1;
            Enumeration keys = pagesMap.keys();
            while (keys.hasMoreElements()) {
                String token = (String) keys.nextElement();
                Vector<PDPage> pages = (Vector<PDPage>) pagesMap.get(token).getSecond();
                String burstFilePath = runtimeOutputFolder + "/" + Utils.getFileNameOfExplodedDocument(defaultFileName, token, sequence);
                Burster pageBurster = getPageExtractor(pdf, burstFilePath, token);
                pageBurster.doExtract(pages);
                burstedDocuments.add(burstFilePath);
                if (isSendFiles) {
                    Sender sender = getSender(token);
                    sender.attachFile(burstFilePath);
                    sender.setQuarantineFolder("./quarantine/" + runtimeFolder + "/");
                    sender.send(token);
                } else {
                    log.warn("Bursted document " + burstFilePath + " was not sent to any recipient because {sendfiles} configuration is false");
                }
                if (isDeleteFiles) {
                    File burstedFile = new File(burstFilePath);
                    if (burstedFile.delete()) {
                        log.warn("Bursted document '" + burstFilePath + "' was deleted because {deletefiles} configuration is true");
                    } else {
                        log.error("Failed to delete " + burstFilePath);
                    }
                }
                sequence++;
            }
            if (isDeleteFiles) {
                String runtimeInputFolder = runtimeOutputFolder + "/input";
                String inputFilePath = runtimeInputFolder + "/" + FilenameUtils.getName(filePath);
                File inputFile = new File(inputFilePath);
                if (!inputFile.delete()) {
                    log.error("Failed to delete " + inputFilePath);
                }
                File inputDir = new File(runtimeInputFolder);
                if (!FileUtils.deleteQuietly(inputDir)) {
                    log.error("Failed to delete " + runtimeInputFolder);
                }
                File runtimeDir = new File(runtimeOutputFolder);
                if (!FileUtils.deleteQuietly(runtimeDir)) {
                    log.error("Failed to delete " + runtimeOutputFolder);
                }
            }
        }
    }

    public boolean checkCorrectness(Hashtable checkAgainstMetaData) {
        log.trace("checkCorrectness(Hashtable checkAgainstMetaData)");
        if (checkAgainstMetaData.size() != pagesMap.size()) {
            return false;
        }
        Enumeration keys = pagesMap.keys();
        while (keys.hasMoreElements()) {
            String explodeToken = (String) keys.nextElement();
            Vector<Integer> pageIndexes = (Vector<Integer>) pagesMap.get(explodeToken).getFirst();
            Vector<Integer> checkAgainsPageIndexes = (Vector<Integer>) checkAgainstMetaData.get(explodeToken);
            if (!pageIndexes.equals(checkAgainsPageIndexes)) {
                return false;
            }
        }
        return true;
    }

    protected Burster getPageExtractor(PDDocument pdf, String outputFileName, String token) {
        log.trace("getPageExtractor(PDDocument pdf, String outputFileName) : outputFileName=" + outputFileName + ",token=" + token);
        return new Burster(pdf, settings, outputFileName, token);
    }

    protected Sender getSender(String token) {
        log.trace("getSender(String explodeToken) : token=" + token);
        Sender sender = senderFactory.getSender(token);
        return sender;
    }

    public Vector<String> getExplodedDocuments() {
        return burstedDocuments;
    }

    public String getRuntimeOutputFolder() {
        return this.runtimeOutputFolder;
    }
}
