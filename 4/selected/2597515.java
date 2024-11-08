package org.sharefast.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.sharefast.core.ServerConsoleServlet;

/**
 * WorkflowUtil provides common methods for sfw files.
 * 
 * @author Kazuo Hiekata <hiekata@nakl.t.u-tokyo.ac.jp>
 */
public class FileUtil {

    /**
	 * Copy text file. <br/>
	 * 
	 * @param src
	 * @param dst
	 * @return boolean
	 */
    public static boolean copyTextFile(File src, File dst) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dst));
            byte[] buf = new byte[1024];
            int readsize = 0;
            while ((readsize = bis.read(buf)) != -1) {
                bos.write(buf, 0, readsize);
            }
            bos.flush();
            bos.close();
            bis.close();
        } catch (IOException e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
            return false;
        }
        return true;
    }
}
