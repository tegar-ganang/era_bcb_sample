package org.smartcrawler.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.smartcrawler.common.AbstractParametrizableComponent;
import org.smartcrawler.retriever.Content;
import org.smartcrawler.common.Link;
import org.smartcrawler.common.SCLogger;
import org.smartcrawler.extractor.MimeTypeTranslator;

/**
 *
 *
 * @author <a href="mailto:pozzad@alice.it">Davide Pozza</a>
 * @version <tt>$Revision: 1.15 $</tt>
 */
public class FileSystemPersister extends AbstractParametrizableComponent implements Persister {

    private static Logger log = SCLogger.getLogger(FileSystemPersister.class);

    private static Logger logPers = SCLogger.getPersisterLogger();

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /**
     * Creates a new instance of FileSystemPersister
     * @param conf
     */
    public FileSystemPersister() {
        log.info("Created persister");
    }

    /**
     *
     * @param content
     */
    public void persist(Content content) {
        Link link = content.getLink();
        byte[] buffer = content.getBuffer();
        if (buffer != null) {
            File rootDir;
            if (getParameter("rootDir") != null) {
                rootDir = new File(getParameter("rootDir"));
            } else {
                rootDir = new File(".");
            }
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }
            File file = linkToFilePath(link, rootDir, content.getContentType());
            if (file == null) {
                log.error("persist(): Unable to convert url " + link + " to a correct file name");
                logPers.error("Unable to convert url " + link + " to a correct file name");
                return;
            }
            log.debug("persist(): File is " + file.getAbsolutePath());
            if (file.exists() && file.length() == buffer.length) {
                log.info("persist(): File " + file.getAbsolutePath() + " exists");
            } else {
                try {
                    log.debug("persist(): Allocating buffer of size " + buffer.length);
                    ByteBuffer bbuf = ByteBuffer.allocate(buffer.length);
                    bbuf.put(buffer);
                    bbuf.flip();
                    WritableByteChannel wChannel = new FileOutputStream(file).getChannel();
                    log.debug("persist(): Got channel for file " + file.getAbsolutePath());
                    int numWritten = wChannel.write(bbuf);
                    log.debug("persist(): Wrote " + numWritten + " bytes on channel for file " + file.getAbsolutePath());
                    wChannel.close();
                    log.debug("persist(): Closed channel for file " + file.getAbsolutePath());
                    bbuf.flip();
                    bbuf.clear();
                    logPers.info("The buffer for url " + link + " was successfully " + "saved on file " + file.getAbsolutePath());
                } catch (IOException e) {
                    log.error("persist(): Problem saving buffer for file " + file.getAbsolutePath(), e);
                    logPers.error("Error saving buffer for file " + file.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        } else {
            log.warn("persist(): the buffer for url " + link + " is NULL");
            logPers.warn("The buffer for url " + link + " is NULL");
        }
    }

    /**
     *
     * @param link
     * @param rootDir
     * @param cType
     * @return
     */
    protected File linkToFilePath(Link link, File rootDir, String cType) {
        log.debug("linkToFilePath(): BEGIN");
        String urlStr = link.toString();
        String fileName = null;
        try {
            log.debug("linkToFilePath(): url string is " + urlStr + " cType=" + cType);
            if (urlStr.toLowerCase().startsWith("http://")) {
                urlStr = urlStr.substring(7);
            }
            if (urlStr.toLowerCase().endsWith("/")) {
                urlStr = urlStr.substring(0, urlStr.length() - 1);
            }
            URL url = link.getURL();
            String qs = url.getQuery();
            String ulrPath = url.getHost() + "/" + url.getPath();
            if (qs != null) {
                qs = qs.replaceAll("\\\\|/", "_");
                urlStr = ulrPath + "_" + qs;
            }
            urlStr = urlStr.replaceAll(":|<|>|\\||\\*", "_");
            StringTokenizer st = new StringTokenizer(urlStr, "/");
            int tokensNum = st.countTokens();
            int counter = 0;
            String path = rootDir.getAbsolutePath() + FILE_SEPARATOR;
            File file = null;
            while (st.hasMoreElements()) {
                counter++;
                String elem = (String) st.nextElement();
                if (elem.length() == 0) continue;
                log.debug("linkToFilePath(): str=" + urlStr + "|token=" + elem + "|tokensNum=" + tokensNum + "|counter=" + counter);
                if (counter == tokensNum && elem.indexOf(".") >= 0 && tokensNum > 1) {
                    fileName = elem;
                } else if (getParameter("preservePath") != null && getParameter("preservePath").equals("true")) {
                    String dirName = path + elem;
                    File dir = new File(dirName);
                    if (!dir.exists()) {
                        dir.mkdir();
                        log.debug("linkToFilePath(): Created dir " + dir.getAbsolutePath());
                    }
                    path += elem + FILE_SEPARATOR;
                }
            }
            String ext = "unknown";
            try {
                ext = MimeTypeTranslator.getFileExtension(cType);
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
            if (fileName == null) {
                fileName = FILE_SEPARATOR + "index." + ext;
            } else if (!fileName.toLowerCase().endsWith(ext)) {
                fileName += "." + ext;
            }
            fileName = path + fileName;
            file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
                log.debug("linkToFilePath(): created file " + file.getAbsolutePath() + " for url: " + urlStr);
            } else {
                log.warn("linkToFilePath(): The file " + fileName + " already exists!");
            }
            return file;
        } catch (Exception e) {
            log.error("linkToFilePath(): Unable to create file: " + fileName + " Error: " + e.getMessage());
            return null;
        } finally {
            log.debug("linkToFilePath(): END");
        }
    }
}
