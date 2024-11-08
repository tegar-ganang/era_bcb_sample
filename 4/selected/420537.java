package agentgui.core.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class can be used in order to copy one file from a source to
 * a destination location.
 * 
 * @author Christian Derksen - DAWIS - ICB - University of Duisburg - Essen
 */
public class FileCopier {

    /**
	 * This method does the actual copying process
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[0xFFFF];
        for (int len; (len = in.read(buffer)) != -1; ) out.write(buffer, 0, len);
    }

    /**
	 * This method allows to copy a file from one location to another one
	 * 
	 * @param srcPath
	 * @param destPath
	 */
    public void copyFile(String srcPath, String destPath) {
        File checkFile = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        checkFile = new File(srcPath);
        if (checkFile.exists() == false) {
            return;
        }
        try {
            fis = new FileInputStream(srcPath);
            fos = new FileOutputStream(destPath);
            copy(fis, fos);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) try {
                fis.close();
            } catch (IOException e) {
            }
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
            }
        }
    }
}
