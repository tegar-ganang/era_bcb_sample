package fi.arcusys.qnet.common.dao;

import java.io.*;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import fi.arcusys.qnet.common.model.ResourceFile;

/**
 * A {@link ResourceFile} data storage implemented in the local file system.
 * 
 * <p>Currently this provider has only two configuration parameters:
 * <code>fsRoot</code>, which defines a local filesystem path to the file
 * storage root directory (e.g. "/var/qnet/files") and <code>appName</code>,
 * which defines the application name to be used as a subdirectory name
 * under the root directory.</p>
 * 
 * 
 * @todo FIXME implement as configurable service
 * @author mikko
 * @version 1.0 $Rev: 567 $
 */
public class FSResourceFileStorage implements ResourceFileStorage {

    private static final Log log = LogFactory.getLog(FSResourceFileStorage.class);

    private String fsRoot;

    private String appName;

    public FSResourceFileStorage() {
    }

    public void initialize(Map<String, String> options) {
        log.debug("Initializing");
        this.fsRoot = options.get("fsRoot");
        log.debug("Set fsRoot to '" + fsRoot + "'");
        this.appName = options.get("appName");
        if (null == appName || 0 == appName.length()) {
            log.error("Required property 'appName' is missing");
            throw new RuntimeException("Required property 'appName' is missing");
        }
        log.debug("Set appName to '" + appName + "'");
    }

    private File getFsRootFile(ResourceFile rf, boolean createMissing) throws IOException {
        if (null == fsRoot || 0 == fsRoot.length()) {
            throw new IOException("fsRoot configuration parameter is missing");
        } else if (null == appName || 0 == appName.length()) {
            throw new IOException("appName configuration parameter is missing");
        }
        File dir = new File(new File(fsRoot), appName);
        if (!dir.exists() && createMissing) {
            if (log.isWarnEnabled()) {
                log.warn("fsRoot/appName '" + dir + "' is missing; trying to create it");
            }
            if (dir.mkdirs()) {
                if (log.isInfoEnabled()) {
                    log.info("Created directory for fsRoot/appName: " + dir);
                }
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to create directory for fsRoot/appName: " + dir);
                }
            }
        }
        if (!dir.exists()) {
            throw new FileNotFoundException("fsRoot/appName directory '" + dir + "' is missing and I failed to (or was told not to) create it");
        }
        return dir;
    }

    /**
	 * Create filename for the actual filesystem storage. 
	 * 
	 * <p>Syntax of generated file name is:</p>
	 * <pre>
	 *   "RF" id
	 *   
	 *   where id = primary key value, i.e. id
	 * </pre>
	 * 
	 * @return
	 */
    static String getStorageFileName(ResourceFile rf) {
        StringBuilder sb = new StringBuilder();
        sb.append("RF");
        sb.append(Long.toString(rf.getId()));
        return sb.toString();
    }

    public void writeData(ResourceFile rf, InputStream in) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("writeData: ResourceFile=" + rf);
        }
        File dir = getFsRootFile(rf, true);
        String fn = getStorageFileName(rf);
        if (log.isDebugEnabled()) {
            log.debug("Filename: " + fn);
        }
        File f = new File(dir, fn);
        if (log.isDebugEnabled()) {
            if (f.exists()) {
                log.debug("File '" + f + "' already exists; owerwriting");
            } else {
                log.debug("File '" + f + "' does not exist; creating new file");
            }
        }
        log.debug("Opening FileOutputStream");
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(f, false));
            in = new BufferedInputStream(in);
            if (log.isDebugEnabled()) {
                log.debug("Writing data to file...");
            }
            byte[] readBuf = new byte[4096];
            int read;
            do {
                read = in.read(readBuf);
                if (read > 0) {
                    os.write(readBuf, 0, read);
                }
            } while (read > 0);
            os.flush();
        } finally {
            if (null != os) {
                try {
                    os.close();
                } catch (Exception ex) {
                    log.error("Catched an exception while closing OutputStream", ex);
                }
            }
        }
    }

    public InputStream getDataAsStream(ResourceFile rf) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("getDataAsStream: ResourceFile=" + rf);
        }
        File dir = getFsRootFile(rf, false);
        String fn = getStorageFileName(rf);
        if (log.isDebugEnabled()) {
            log.debug("Filename: " + fn);
        }
        File f = new File(dir, fn);
        log.debug("Opening FileInputStream");
        return new FileInputStream(f);
    }

    public void deleteFileData(ResourceFile rf) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("deleteFileData: ResourceFile=" + rf);
        }
        File dir = getFsRootFile(rf, true);
        String fn = getStorageFileName(rf);
        if (log.isDebugEnabled()) {
            log.debug("Filename: " + fn);
        }
        File f = new File(dir, fn);
        if (!f.exists()) {
            log.warn("File '" + f + "' does not exist");
        } else {
            if (f.delete()) {
                log.info("Deleted ResourceFile Data file: " + f);
            } else {
                log.warn("Failed to delete ResourceFile data file: " + f);
            }
        }
    }
}
