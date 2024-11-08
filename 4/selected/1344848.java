package it.mozzicato.apkwizard.utils;

import it.mozzicato.apkwizard.*;
import java.io.*;
import java.nio.channels.*;

public class PathUtils {

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    /**
	 * Build a file representation of a file or directory under the configured "projects" directory
	 *  
	 * @param subPaths
	 * @return
	 */
    public static File getFileInProjects(String... subPaths) {
        StringBuilder ret = new StringBuilder(Configuration.getProperty(Configuration.PROJECT_DIR, "projects"));
        if (subPaths != null) {
            for (String path : subPaths) {
                ret.append(File.separator);
                ret.append(path);
            }
        }
        return new File(ret.toString());
    }

    public static String extractProjectSubpath(File nestedFile, String projectName) {
        String projectPath = Configuration.getProperty(Configuration.PROJECT_DIR, "projects") + File.separator + projectName + File.separator;
        String filePath = nestedFile.getAbsolutePath();
        for (String subpath : ApkWizard.SUBPATHS) {
            String testingSubpath = (projectPath + subpath).toLowerCase();
            int pos = filePath.toLowerCase().indexOf(testingSubpath);
            if (pos >= 0) return filePath.substring(pos + testingSubpath.length() + File.separator.length());
        }
        return null;
    }
}
