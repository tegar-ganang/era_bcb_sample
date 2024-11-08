package org.dita2indesign.idml2dita;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Command-line utility for transforming InDesign CS4 IDML files into DITA-based XML
 * using XSLT transforms.
 */
public class Idml2Dita {

    private static Log log = LogFactory.getLog(Idml2Dita.class);

    /**
	 *
	 */
    public class Options {

        private String xslt;

        private Object styleMapUri;

        /**
		 * @param xsltUri
		 */
        public void setXslt(String xsltUri) {
            Idml2Dita.log.debug("Setting XSLT to \"" + xsltUri + "\"");
            this.xslt = xsltUri;
        }

        /**
		 * @return the xslt
		 */
        public String getXslt() {
            return this.xslt;
        }

        /**
		 * @return
		 */
        public Object getStyleMapUri() {
            return this.styleMapUri;
        }

        /**
		 * @param styleMapUri the styleMapUri to set
		 */
        public void setStyleMapUri(Object styleMapUri) {
            this.styleMapUri = styleMapUri;
        }
    }

    /**
	 * 
	 */
    private static void printUsage() {
        String usage = "Usage:\n\n" + "\t" + Idml2Dita.class.getSimpleName() + " " + " idmlFile " + " {resultXmlFile}" + "\n\n" + "Where:\n\n" + " idmlFile: The InDesign IDML file to be transformed.\n" + " resultXmlFile: The path, and optionally, filename to use for the output XML file. By default, uses the IDML filename.\n" + "\n" + "NOTE: Existing result files are silently overwritten.";
        System.err.println(usage);
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(-1);
        }
        String idmlFilename = args[0];
        File idmlFile = new File(idmlFilename);
        checkExistsAndCanReadSystemExit(idmlFile);
        File resultDir = null;
        File resultFile = null;
        String resultFilename = null;
        String namePart = idmlFile.getName();
        namePart = namePart.substring(0, namePart.lastIndexOf("."));
        resultFilename = namePart + ".xml";
        if (args.length >= 2) {
            String temp = args[1];
            File tempFile = new File(temp);
            checkExistsAndCanReadSystemExit(idmlFile);
            if (tempFile.isDirectory()) {
                resultDir = tempFile;
                if (!resultDir.canWrite()) {
                    System.err.println("Output directory \"" + resultDir.getAbsolutePath() + "\" is not writable");
                    System.exit(-1);
                }
                resultFile = new File(resultDir, resultFilename);
            } else {
                resultFile = tempFile;
            }
        } else {
            resultFile = new File(idmlFile.getParentFile(), resultFilename);
        }
        Idml2Dita app = new Idml2Dita();
        Options options = app.new Options();
        options.setXslt("file:///Users/ekimber/workspace/dita2indesign/xslt/idml2dita/idml2dita.xsl");
        options.setStyleMapUri("file:///Users/ekimber/workspace/dita2indesign/xslt/idml2dita/cc-style2tagmap.xml");
        try {
            app.generateDita(idmlFile, resultFile, options);
            System.out.println("XML generation complete, result is in \"" + resultFile.getAbsolutePath() + "\"");
        } catch (Exception e) {
            System.err.println("XML generation failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
	 * @param idmlFile
	 * @param resultFile
	 * @param options 
	 * @throws Exception 
	 */
    private void generateDita(File idmlFile, File resultFile, Options options) throws Exception {
        File tempDir = getTempDir(true);
        unzip(idmlFile, tempDir);
        File documentXml = new File(tempDir, "designmap.xml");
        if (!documentXml.exists()) {
            throw new RuntimeException("Failed to find document.xml within DOCX package. This should not happen.");
        }
        URL xsltUrl = new URL(options.getXslt());
        TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
        Source style = new StreamSource(xsltUrl.openStream());
        style.setSystemId(xsltUrl.toExternalForm());
        Transformer trans = factory.newTransformer(style);
        trans.setParameter("styleMapUri", options.getStyleMapUri());
        Source xmlSource = new StreamSource(new FileInputStream(documentXml));
        xmlSource.setSystemId(documentXml.getAbsolutePath());
        Result result = new StreamResult(new FileOutputStream(resultFile));
        log.debug("Calling transform()...");
        trans.transform(xmlSource, result);
        log.debug("Transformation complete.");
        FileUtils.deleteDirectory(tempDir);
    }

    /**
	 * Gets a temporary directory.
	 * @param deleteOnExit If set to true, directory will be deleted on exit.
	 * @return
	 * @throws Exception 
	 */
    protected File getTempDir(boolean deleteOnExit) throws Exception {
        File tempFile = File.createTempFile(this.getClass().getSimpleName() + "_", "trash");
        File tempDir = new File(tempFile.getAbsolutePath() + "_dir");
        tempDir.mkdirs();
        tempFile.delete();
        if (deleteOnExit) tempDir.deleteOnExit();
        return tempDir;
    }

    private static void checkExistsAndCanReadSystemExit(File docxFile) {
        try {
            checkExistsAndCanRead(docxFile);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    /**
	 * @param file
	 */
    private static void checkExistsAndCanRead(File file) {
        if (!file.exists()) {
            throw new RuntimeException("File " + file.getAbsolutePath() + " does not exist.");
        }
        if (!file.canRead()) {
            throw new RuntimeException("File " + file.getAbsolutePath() + " exists but cannot be read.");
        }
    }

    /**
	* @param zipInFile
	*            Zip file to be unzipped
	* @param outputDir
	*            Directory to which the zipped files will be unpacked.
	* @throws Exception 
	* @throws ZipException 
	*/
    public static void unzip(File zipInFile, File outputDir) throws Exception {
        Enumeration<? extends ZipEntry> entries;
        ZipFile zipFile = new ZipFile(zipInFile);
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipInFile));
        ZipEntry entry = (ZipEntry) zipInputStream.getNextEntry();
        File curOutDir = outputDir;
        while (entry != null) {
            if (entry.isDirectory()) {
                curOutDir = new File(curOutDir, entry.getName());
                curOutDir.mkdirs();
                continue;
            }
            File outFile = new File(curOutDir, entry.getName());
            File tempDir = outFile.getParentFile();
            if (!tempDir.exists()) tempDir.mkdirs();
            outFile.createNewFile();
            BufferedOutputStream outstream = new BufferedOutputStream(new FileOutputStream(outFile));
            int n;
            byte[] buf = new byte[1024];
            while ((n = zipInputStream.read(buf, 0, 1024)) > -1) outstream.write(buf, 0, n);
            outstream.flush();
            outstream.close();
            zipInputStream.closeEntry();
            entry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
        zipFile.close();
    }
}
