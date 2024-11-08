package accessories.plugins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import freemind.extensions.ExportHook;
import freemind.main.FreeMind;
import freemind.main.FreeMindStarter;
import freemind.main.Resources;
import freemind.main.Tools;

/**
 * @author foltin
 * 
 */
public class ExportToOoWriter extends ExportHook {

    private static java.util.logging.Logger logger = null;

    /**
	 * 
	 */
    public ExportToOoWriter() {
        super();
        if (logger == null) {
            logger = freemind.main.Resources.getInstance().getLogger(this.getClass().getName());
        }
    }

    public void startupMapHook() {
        super.startupMapHook();
        File chosenFile = chooseFile(getResourceString("file_type"), null, null);
        if (chosenFile == null) {
            return;
        }
        getController().getFrame().setWaitingCursor(true);
        try {
            exportToOoWriter(chosenFile);
        } catch (IOException e) {
            freemind.main.Resources.getInstance().logException(e);
        }
        getController().getFrame().setWaitingCursor(false);
    }

    public boolean exportToOoWriter(File chosenFile) throws IOException {
        StringWriter writer = new StringWriter();
        getController().getMap().getFilteredXml(writer);
        String xslts = getResourceString("files");
        return exportToOoWriter(chosenFile, writer, xslts);
    }

    /**
	 * @return true, if successful.
	 */
    private boolean applyXsltFile(String xsltFileName, StringWriter writer, Result result) throws IOException {
        URL xsltUrl = getResource(xsltFileName);
        if (xsltUrl == null) {
            logger.severe("Can't find " + xsltFileName + " as resource.");
            throw new IllegalArgumentException("Can't find " + xsltFileName + " as resource.");
        }
        InputStream xsltStream = xsltUrl.openStream();
        Source xsltSource = new StreamSource(xsltStream);
        try {
            StringReader reader = new StringReader(writer.getBuffer().toString());
            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);
            trans.setParameter("date", DateFormat.getDateInstance(DateFormat.SHORT).format(new Date()));
            trans.transform(new StreamSource(reader), result);
            return true;
        } catch (Exception e) {
            freemind.main.Resources.getInstance().logException(e);
            return false;
        }
    }

    public boolean exportToOoWriter(File file, StringWriter writer, String xslts) throws IOException {
        boolean resultValue = true;
        ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(file));
        Result result = new StreamResult(zipout);
        StringTokenizer tokenizer = new StringTokenizer(xslts, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String[] files = token.split("->");
            if (files.length == 2) {
                ZipEntry entry = new ZipEntry(files[1]);
                zipout.putNextEntry(entry);
                if (files[0].endsWith(".xsl")) {
                    logger.info("Transforming with xslt " + files[0] + " to file " + files[1]);
                    resultValue &= applyXsltFile(files[0], writer, result);
                } else {
                    logger.info("Copying resource from " + files[0] + " to file " + files[1]);
                    resultValue &= copyFromResource(files[0], zipout);
                }
                zipout.closeEntry();
            }
        }
        zipout.close();
        return resultValue;
    }

    /**
     * @return true, if successful.
     */
    private boolean copyFromResource(String fileName, OutputStream out) {
        try {
            logger.finest("searching for " + fileName);
            URL resource = getResource(fileName);
            if (resource == null) {
                logger.severe("Cannot find resource: " + fileName);
                return false;
            }
            InputStream in = resource.openStream();
            Tools.copyStream(in, out, false);
            return true;
        } catch (Exception e) {
            logger.severe("File not found or could not be copied. " + "Was earching for " + fileName + " and should go to " + out);
            freemind.main.Resources.getInstance().logException(e);
            return false;
        }
    }
}
