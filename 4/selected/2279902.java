package edu.unibi.agbi.biodwh.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import edu.unibi.agbi.biodwh.config.log.Log;
import edu.unibi.agbi.biodwh.project.logic.Process;
import edu.unibi.agbi.biodwh.project.logic.queue.DownloadQueue;
import edu.unibi.agbi.biodwh.project.logic.queue.ProcessQueue;

/**
 * @author Benjamin Kormeier
 * @version 1.0 12.12.2006
 */
public class Download implements Runnable {

    private String projectName = null;

    private String parserID = null;

    private URL source = null;

    private File target = null;

    private Calendar source_date = null;

    private long file_size = 0;

    private long read_position = 0;

    private boolean abort = false;

    public Download(String projectName, String parserID, URL source, long file_size, Calendar source_date, String target) {
        this.projectName = projectName;
        this.parserID = parserID;
        this.source = source;
        this.file_size = file_size * 1024;
        this.source_date = source_date;
        this.target = new File(target);
    }

    private boolean checkTarget() throws IOException {
        File path = new File(target.getParent());
        if (!path.exists()) path.mkdirs();
        return target.exists();
    }

    private void downloadFile() throws IOException {
        int default_buffer_size = 2048;
        BufferedInputStream is = new BufferedInputStream(source.openStream(), default_buffer_size);
        FileOutputStream fo = new FileOutputStream(target);
        BufferedOutputStream bos = new BufferedOutputStream(fo, default_buffer_size);
        int read = 0;
        while ((read = is.read()) != -1 && !abort) {
            read_position++;
            bos.write(read);
        }
        is.close();
        bos.flush();
        bos.close();
    }

    private int calculateProgress(long pos) {
        double result = ((double) pos / (double) (file_size));
        return (int) (result);
    }

    public int getProgress() {
        if (file_size != 0) return calculateProgress(read_position); else return -1;
    }

    /**
	 * @return the projectName
	 */
    public synchronized String getProjectName() {
        return projectName;
    }

    /**
	 * @return the parserID
	 */
    public synchronized String getParserID() {
        return parserID;
    }

    public String getFileName() {
        return target.getName();
    }

    public boolean abort() {
        abort = true;
        return abort;
    }

    public void run() {
        try {
            if (checkTarget()) {
                Calendar target_date = Calendar.getInstance();
                target_date.setTimeInMillis(this.target.lastModified());
                if (source_date.after(target_date)) downloadFile();
            } else downloadFile();
        } catch (IOException e) {
            Log.writeFatalLog(this.getClass(), e.getMessage(), e);
            Process p = ProcessQueue.getActiveProcess(projectName, parserID);
            if (p != null) p.failedProcess();
        }
        DownloadQueue.removeActiveDownload(this);
    }
}
