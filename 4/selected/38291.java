package nuts.core.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimerTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * File Cache Task class
 */
public class FileCacheTask extends TimerTask {

    private static final Log log = LogFactory.getLog(FileCacheTask.class);

    private static FileCacheTask instance;

    private List<File> fileList = new ArrayList<File>();

    /**
	 * work directory
	 */
    private File workdir = new File(System.getProperty("java.io.tmpdir"));

    /**
	 * The prefix string to be used in generating the file's name; must be at least three characters long
	 */
    private String prefix = "fcs-";

    /**
	 * milliseconds since file last modified. (default: 1h)
	 */
    private long expiredTime = 60 * 60 * 1000;

    /**
	 * @return the instance
	 */
    public static synchronized FileCacheTask getInstance() {
        if (instance == null) {
            instance = new FileCacheTask();
        }
        return instance;
    }

    /**
	 * @param instance the instance to set
	 */
    public static synchronized void setInstance(FileCacheTask instance) {
        FileCacheTask.instance = instance;
    }

    /**
	 * @return the workdir
	 */
    public File getWorkdir() {
        return workdir;
    }

    /**
	 * @param workdir the workdir to set
	 */
    public void setWorkdir(File workdir) {
        if (!workdir.exists()) {
            workdir.mkdirs();
        }
        if (!workdir.isDirectory()) {
            throw new IllegalArgumentException("FileCacheTask work directory does not exists: " + workdir.getPath());
        }
        this.workdir = workdir;
    }

    /**
	 * @param workdir the workdir to set
	 */
    public void setWorkdir(String workdir) {
        setWorkdir(new File(workdir));
    }

    /**
	 * @return the expiredTime
	 */
    public long getExpiredTime() {
        return expiredTime;
    }

    /**
	 * @param expiredTime the expiredTime to set
	 */
    public void setExpiredTime(long expiredTime) {
        this.expiredTime = expiredTime;
    }

    /**
	 * delete temp files
	 */
    public void deleteTempFiles() {
        for (File f : workdir.listFiles()) {
            boolean r = f.delete();
            if (log.isDebugEnabled()) {
                log.debug("delete temp file " + f.getPath() + " ... " + (r ? "[OK]" : "[FAILED]"));
            }
        }
    }

    /**
	 * new file for delete when expired
	 * @return cache file
	 * @throws IOException if an I/O error occurs
	 */
    public File newFile() throws IOException {
        return newFile(".tmp");
    }

    /**
	 * new file for delete when expired
	 * @param suffix suffix
	 * @return cache file
	 * @throws IOException if an I/O error occurs
	 */
    public File newFile(String suffix) throws IOException {
        File nf = File.createTempFile(prefix, suffix, workdir);
        synchronized (fileList) {
            fileList.add(nf);
        }
        if (log.isDebugEnabled()) {
            log.debug("New file [" + nf.getPath() + "]");
        }
        return nf;
    }

    /**
	 * add file for delete when expired
	 * @param file file
	 * @param suffix suffix
	 * @return cache file
	 * @throws IOException if an I/O error occurs
	 */
    public File addFile(File file, String suffix) throws IOException {
        if (file.exists() && file.isFile()) {
            File nf = File.createTempFile(prefix, "." + suffix, workdir);
            nf.delete();
            if (!file.renameTo(nf)) {
                IOUtils.copy(file, nf);
            }
            synchronized (fileList) {
                fileList.add(nf);
            }
            if (log.isDebugEnabled()) {
                log.debug("Add file [" + file.getPath() + "] -> [" + nf.getPath() + "]");
            }
            return nf;
        }
        return file;
    }

    /**
	 * add file for delete when expired
	 * @param file file
	 * @return cache file
	 * @throws IOException if an I/O error occurs
	 */
    public File addFile(File file) throws IOException {
        return addFile(file, IOUtils.getFileNameExtension(file));
    }

    /**
	 * get file
	 * @param fileName fileName
	 * @return file name[
	 */
    public File getFile(String fileName) {
        File file = new File(workdir, fileName);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    /**
	 * @see java.util.TimerTask#run()
	 */
    @Override
    public void run() {
        synchronized (fileList) {
            long time = Calendar.getInstance().getTimeInMillis();
            List<File> removeList = new ArrayList<File>();
            for (File f : fileList) {
                if (f.exists()) {
                    if (time - f.lastModified() > expiredTime) {
                        if (f.delete()) {
                            removeList.add(f);
                            if (log.isDebugEnabled()) {
                                log.debug("File deleted - " + f.getPath());
                            }
                        } else {
                            if (log.isWarnEnabled()) {
                                log.warn("File delete failed - " + f.getPath());
                            }
                        }
                    }
                } else {
                    removeList.add(f);
                }
            }
            for (File f : removeList) {
                fileList.remove(f);
            }
        }
    }
}
