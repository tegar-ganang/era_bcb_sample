package net.sourceforge.jdirdiff.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *  Manages pair of files features.
 *
 * @author    Al Vega
 */
public class FileEntry implements DiffValues {

    /**
     *  Actual level of difference.
     *
     * @see    DiffValues
     */
    private int actualDifference;

    /**  file object */
    public File file;

    /**  describes this file type (ls -F like) */
    private String lsFileType;

    /**
     *Constructor for the FileEntry object
     *
     * @param  sfile  string path to a file
     */
    public FileEntry(String sfile) {
        file = new File(sfile);
    }

    /**
     *  Determines if both are directories and are not the same.
     *
     * @param  froots  pair of files
     * @return         true if areDirsNDiff
     */
    public static boolean areDirsNDiff(FileEntry[] froots) {
        for (int i = 0; i < TWO; i++) if (!froots[i].file.isDirectory()) return false;
        if (froots[0].file.getAbsolutePath().equals(froots[1].file.getAbsolutePath())) return false;
        return true;
    }

    /**
     *  Copy src file to dest file using java.nio.
     *
     * @param  src   source file.
     * @param  dest  destination file.
     */
    public static void copy(File src, File dest) {
        try {
            FileChannel srcChannel = new FileInputStream(src).getChannel();
            FileChannel dstChannel = new FileOutputStream(dest).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
        }
    }

    /**
     *  fill fes: compare both files, and determine: level of difference and
     *  lsfiletype.
     *
     * @param  fes        pair of files.
     * @param  whoIsNull  one of the pair of files can be null. If whoIsNull is
     * null, both files are not null. jo jo jo
     */
    public static void fill(FileEntry[] fes, Integer whoIsNull) {
        fillDifference(fes, whoIsNull);
        fillLsFileType(fes);
    }

    public int getActualDifference() {
        return actualDifference;
    }

    /**
     *  Filter user requested files bye include and exclude.
     *
     * @return    true if this file is included by the filter.
     */
    public boolean includeFilter() {
        String basename = file.getName();
        if (basename.equals(".")) return false;
        if (basename.equals("..")) return false;
        if (file.isDirectory()) return true;
        String sexclude = ResourceConfig.getInstance().get_String(EXCLUDE);
        if (sexclude != null && !sexclude.equals("")) {
            if (basename.matches(sexclude)) return false;
        }
        String sinclude = ResourceConfig.getInstance().get_String(INCLUDE);
        if (sinclude != null && !sinclude.equals("")) {
            if (basename.matches(sinclude)) return true;
            return false;
        }
        return true;
    }

    /**
     *  retrieves this directory's children.
     *  It should be asserted that this file is directory.
     *
     * @return    String array of children.
     */
    public String[] retrieveContents() {
        List files = Arrays.asList(file.list());
        Collections.sort(files);
        return (String[]) files.toArray();
    }

    /**
     *  String representation of this object, in html.
     *
     * @return    html string representation of this object.
     */
    public String toString() {
        String color = ResourceConfig.getInstance().get_String(DIFFS[actualDifference][DIFF_NAME]);
        return "<html><font color=#" + color + ">" + DIFFS[actualDifference][DIFF_MARK] + " " + file.getName() + lsFileType + "</font></html>";
    }

    /**
     *  String representation of this object, text mode.
     *
     * @return    text string representation of this object.
     */
    public String toStringOld0() {
        return "====>\ndirName: " + file.getParent() + "\nbaseName: " + file.getName() + "\ndifference: " + "\n";
    }

    /**
     *  String representation of this object, simple text mode.
     *
     * @return    simple text string representation of this object.
     */
    public String toStringSimple() {
        return DIFFS[actualDifference][DIFF_MARK] + file.getName();
    }

    private static boolean compareMD5(FileEntry[] fes) {
        MD5 md5 = new MD5();
        boolean ret = false;
        try {
            ret = md5.compareMD5(fes[0].file, fes[1].file);
        } catch (Exception e) {
            System.err.println("compareMD5" + e);
        }
        return ret;
    }

    private static void fillDifference(FileEntry[] fes, Integer whoIsNull) {
        if (whoIsNull != null) {
            fes[whoIsNull.intValue()].actualDifference = NOT_HERE;
            fes[1 - whoIsNull.intValue()].actualDifference = ONLY_ME;
            return;
        }
        fes[0].actualDifference = fes[1].actualDifference = getDiff(fes);
    }

    private static void fillLsFileType(FileEntry[] fes) {
        for (int i = 0; i < fes.length; i++) if (fes[i] != null) {
            if (fes[i].file.isDirectory()) fes[i].lsFileType = "/"; else fes[i].lsFileType = "";
        }
    }

    private static int getDiff(FileEntry[] fes) {
        int diffLevel = ResourceConfig.getInstance().get_int(DIFF_LEVEL);
        if (fes[0].file.isDirectory() && fes[1].file.isDirectory()) return SAME_SAME;
        if (SAME_NAME == diffLevel) return SAME_SAME;
        if (fes[0].file.length() != fes[1].file.length()) return SAME_NAME;
        if (SAME_LENGTH == diffLevel) return SAME_SAME;
        if (fes[0].file.lastModified() == fes[1].file.lastModified()) return SAME_SAME;
        if (compareMD5(fes)) return SAME_SAME;
        return SAME_LENGTH;
    }
}
