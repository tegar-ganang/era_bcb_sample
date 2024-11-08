package au.gov.naa.digipres.xena.plugin.website;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import au.gov.naa.digipres.xena.core.ReleaseInfo;
import au.gov.naa.digipres.xena.kernel.XenaException;
import au.gov.naa.digipres.xena.kernel.XenaInputSource;
import au.gov.naa.digipres.xena.kernel.filenamer.AbstractFileNamer;
import au.gov.naa.digipres.xena.kernel.filenamer.FileNamerManager;
import au.gov.naa.digipres.xena.kernel.metadatawrapper.AbstractMetaDataWrapper;
import au.gov.naa.digipres.xena.kernel.metadatawrapper.MetaDataWrapperManager;
import au.gov.naa.digipres.xena.kernel.normalise.AbstractNormaliser;
import au.gov.naa.digipres.xena.kernel.normalise.BinaryToXenaBinaryNormaliser;
import au.gov.naa.digipres.xena.kernel.normalise.NormaliserResults;
import au.gov.naa.digipres.xena.kernel.type.Type;
import au.gov.naa.digipres.xena.util.FileUtils;

/**
 * Base class for normalising web sites. This normaliser is based on the ArchiveNormaliser, as a website is considered to be a collection of web site
 * files inside a Zip archive, with the zip file given an extension of ".wsx".
 * 
 * This normaliser simply produces an individual normalised file for each file inside the zip archive, and also produces an index of each original file
 * path to the name of the corresponding normalised file. This index will be used when viewing the normalised web site.
 * 
 * created 14/7/09
 * @author Justin Waddell
 * archive
 * Short desc of class:
 */
public class WebsiteNormaliser extends AbstractNormaliser {

    public static final String WEBSITE_PREFIX = "website";

    public static final String WEBSITE_TAG = "website";

    public static final String FILE_TAG = "file";

    public static final String FILE_ORIGINAL_PATH_ATTRIBUTE = "original_path";

    public static final String FILE_OUTPUT_FILENAME = "output_filename";

    public static final String WEBSITE_URI = "http://preservation.naa.gov.au/website/1.0";

    public static final String DATE_FORMAT_STRING = "yyyyMMdd'T'HHmmssZ";

    @Override
    public void parse(InputSource input, NormaliserResults results, boolean convertOnly) throws SAXException, java.io.IOException {
        FileNamerManager fileNamerManager = normaliserManager.getPluginManager().getFileNamerManager();
        AbstractFileNamer fileNamer = fileNamerManager.getActiveFileNamer();
        MetaDataWrapperManager wrapperManager = normaliserManager.getPluginManager().getMetaDataWrapperManager();
        OutputStream entryOutputStream = null;
        ZipInputStream archiveStream = null;
        File tempStagingDir = null;
        String previousBasePath = null;
        boolean setNewBasePath = false;
        try {
            ContentHandler ch = getContentHandler();
            AttributesImpl att = new AttributesImpl();
            ch.startElement(WEBSITE_URI, WEBSITE_TAG, WEBSITE_PREFIX + ":" + WEBSITE_TAG, att);
            archiveStream = new ZipInputStream(input.getByteStream());
            tempStagingDir = File.createTempFile("website-extraction", "");
            tempStagingDir.delete();
            tempStagingDir.mkdirs();
            previousBasePath = wrapperManager.getBasePathName();
            wrapperManager.setBasePathName(tempStagingDir.getAbsolutePath());
            setNewBasePath = true;
            WebsiteEntry entry = getNextEntry(archiveStream, tempStagingDir);
            while (entry != null) {
                File tempFile = new File(entry.getFilename());
                XenaInputSource childXis = new XenaInputSource(tempFile);
                Type fileType = normaliserManager.getPluginManager().getGuesserManager().mostLikelyType(childXis);
                childXis.setType(fileType);
                AbstractNormaliser entryNormaliser = normaliserManager.lookup(fileType);
                File entryOutputFile;
                if (convertOnly) {
                    if (entryNormaliser.isConvertible()) {
                        entryOutputFile = fileNamer.makeNewOpenFile(childXis, entryNormaliser);
                    } else {
                        String strOutputPath = tempFile.toString().replaceFirst(tempStagingDir.toString(), results.getDestinationDirString());
                        FileUtils.fileCopy(tempFile, strOutputPath, false);
                        tempFile.delete();
                        entry = getNextEntry(archiveStream, tempStagingDir);
                        continue;
                    }
                } else {
                    entryOutputFile = fileNamer.makeNewXenaFile(childXis, entryNormaliser);
                }
                childXis.setOutputFileName(entryOutputFile.getName());
                NormaliserResults childResults;
                try {
                    entryOutputStream = new FileOutputStream(entryOutputFile);
                    childResults = normaliseWebsiteEntry(childXis, entryNormaliser, entryOutputFile, entryOutputStream, fileNamerManager, fileType, convertOnly);
                } catch (Exception ex) {
                    System.out.println("Normalisation of website file failed, switching to binary.\n" + ex);
                    if (entryOutputFile.exists()) {
                        entryOutputFile.delete();
                    }
                    entryNormaliser = normaliserManager.lookup(BinaryToXenaBinaryNormaliser.BINARY_NORMALISER_NAME);
                    entryOutputFile = fileNamer.makeNewXenaFile(childXis, entryNormaliser);
                    childXis.setOutputFileName(entryOutputFile.getName());
                    entryOutputStream = new FileOutputStream(entryOutputFile);
                    childResults = normaliseWebsiteEntry(childXis, entryNormaliser, entryOutputFile, entryOutputStream, fileNamerManager, fileType, convertOnly);
                } finally {
                    if (entryOutputStream != null) {
                        entryOutputStream.close();
                    }
                }
                results.addChildAIPResult(childResults);
                String entryPath = entry.getName().replace('\\', '/');
                String entryOutputFilename = entryOutputFile.getName();
                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute(WEBSITE_URI, FILE_ORIGINAL_PATH_ATTRIBUTE, WEBSITE_PREFIX + ":" + FILE_ORIGINAL_PATH_ATTRIBUTE, "CDATA", entryPath);
                atts.addAttribute(WEBSITE_URI, FILE_OUTPUT_FILENAME, WEBSITE_PREFIX + ":" + FILE_OUTPUT_FILENAME, "CDATA", entryOutputFilename);
                ch.startElement(WEBSITE_URI, FILE_TAG, WEBSITE_PREFIX + ":" + FILE_TAG, atts);
                ch.endElement(WEBSITE_URI, FILE_TAG, WEBSITE_PREFIX + ":" + FILE_TAG);
                tempFile.delete();
                entry = getNextEntry(archiveStream, tempStagingDir);
            }
            ch.endElement(WEBSITE_URI, WEBSITE_TAG, WEBSITE_PREFIX + ":" + WEBSITE_TAG);
        } catch (XenaException x) {
            throw new SAXException("Problem parseing Xena file", x);
        } catch (TransformerException e) {
            throw new SAXException("Problem creating XML transformer", e);
        } finally {
            if (entryOutputStream != null) {
                entryOutputStream.close();
            }
            if (archiveStream != null) {
                archiveStream.close();
            }
            FileUtils.deleteDirAndContents(tempStagingDir);
            if (setNewBasePath) {
                wrapperManager.setBasePathName(previousBasePath);
            }
        }
    }

    private NormaliserResults normaliseWebsiteEntry(XenaInputSource childXis, AbstractNormaliser entryNormaliser, File entryOutputFile, OutputStream entryOutputStream, FileNamerManager fileNamerManager, Type fileType, boolean convertOnly) throws TransformerConfigurationException, XenaException, SAXException, IOException {
        SAXTransformerFactory transformFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
        TransformerHandler transformerHandler = transformFactory.newTransformerHandler();
        entryNormaliser.setProperty("http://xena/url", childXis.getSystemId());
        AbstractMetaDataWrapper wrapper = null;
        if (convertOnly) {
            wrapper = normaliserManager.getPluginManager().getMetaDataWrapperManager().getEmptyWrapper().getWrapper();
            transformerHandler.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        } else {
            wrapper = normaliserManager.getPluginManager().getMetaDataWrapperManager().getWrapNormaliser();
            wrapper = getNormaliserManager().wrapTheNormaliser(entryNormaliser, childXis, wrapper);
        }
        wrapper.setContentHandler(transformerHandler);
        wrapper.setLexicalHandler(transformerHandler);
        wrapper.setParent(entryNormaliser);
        wrapper.setProperty("http://xena/input", childXis);
        wrapper.setProperty("http://xena/normaliser", entryNormaliser);
        entryNormaliser.setContentHandler(wrapper);
        entryNormaliser.setLexicalHandler(wrapper);
        entryNormaliser.setProperty("http://xena/file", entryOutputFile);
        entryNormaliser.setProperty("http://xena/normaliser", entryNormaliser);
        OutputStreamWriter osw = new OutputStreamWriter(entryOutputStream, "UTF-8");
        StreamResult streamResult = new StreamResult(osw);
        transformerHandler.setResult(streamResult);
        NormaliserResults childResults = new NormaliserResults(childXis, entryNormaliser, fileNamerManager.getDestinationDir(), fileNamerManager.getActiveFileNamer(), wrapper);
        childResults.setInputType(fileType);
        childResults.setOutputFileName(entryOutputFile.getName());
        normaliserManager.parse(entryNormaliser, childXis, wrapper, childResults, convertOnly);
        childResults.setNormalised(true);
        childResults.setId(wrapper.getSourceId(new XenaInputSource(entryOutputFile)));
        return childResults;
    }

    @Override
    public String getVersion() {
        return ReleaseInfo.getVersion() + "b" + ReleaseInfo.getBuildNumber();
    }

    private WebsiteEntry getNextEntry(ZipInputStream zipStream, File tempStagingDir) throws IOException {
        boolean found = false;
        ZipEntry zipEntry;
        do {
            zipEntry = zipStream.getNextEntry();
            if (zipEntry == null) {
                return null;
            }
            if (!zipEntry.isDirectory()) {
                found = true;
            }
        } while (found == false);
        File entryTempFile = new File(tempStagingDir, zipEntry.getName());
        entryTempFile.getParentFile().mkdirs();
        entryTempFile.deleteOnExit();
        FileOutputStream tempFileOS = new FileOutputStream(entryTempFile);
        byte[] readBuff = new byte[10 * 1024];
        int bytesRead = zipStream.read(readBuff);
        while (bytesRead > 0) {
            tempFileOS.write(readBuff, 0, bytesRead);
            bytesRead = zipStream.read(readBuff);
        }
        WebsiteEntry archiveEntry = new WebsiteEntry(zipEntry.getName(), entryTempFile.getAbsolutePath());
        archiveEntry.setOriginalFileDate(new Date(zipEntry.getTime()));
        archiveEntry.setOriginalSize(zipEntry.getSize());
        return archiveEntry;
    }

    @Override
    public String getName() {
        return "Website";
    }

    @Override
    public boolean isConvertible() {
        return true;
    }

    @Override
    public String getOutputFileExtension() {
        return "wsx";
    }
}
