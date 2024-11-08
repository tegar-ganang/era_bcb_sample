package com.gargoylesoftware.htmlunit.source;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 * Subversion utilities.
 *
 * @version $Revision: 6701 $
 * @author Ahmed Ashour
 */
public final class SVN {

    private SVN() {
    }

    /**
     * Recursively deletes any '.svn' folder which contains Subversion information.
     * @param dir the directory to recursively delete '.svn' from
     * @throws IOException if an exception happens
     */
    public static void deleteSVN(final File dir) throws IOException {
        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                if (".svn".equals(f.getName())) {
                    FileUtils.deleteDirectory(f);
                } else {
                    deleteSVN(f);
                }
            }
        }
    }

    /**
     * Ensures that all files inside the specified directory has consistent new lines.
     * @param dir the directory to recursively ensure all contained files have consistent new lines
     * @throws IOException if an exception happens
     */
    public static void consistentNewlines(final File dir) throws IOException {
        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                if (!".svn".equals(f.getName())) {
                    consistentNewlines(f);
                }
            } else {
                FileUtils.writeLines(f, FileUtils.readLines(f));
            }
        }
    }
}
