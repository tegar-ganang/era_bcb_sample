package com.pjsofts;

import java.text.NumberFormat;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Vector;
import javax.swing.JOptionPane;

/** Utility methods on string */
public final class FileLib {

    private static final java.util.ResourceBundle i18n = java.util.ResourceBundle.getBundle("com/pjsofts/resources/commonres");

    private static NumberFormat formatter = NumberFormat.getInstance();

    /** Size of the cache used during file r/w in bytes */
    private static final int CACHE_SIZE = 1024;

    public static final long LENGTH_Ko = 1024;

    public static final long LENGTH_Mo = 1024 * LENGTH_Ko;

    public static final long LENGTH_Go = 1024 * LENGTH_Mo;

    public static final long LENGTH_To = 1024 * LENGTH_Go;

    public static final long LENGTH_FLOPPY = 1433 * LENGTH_Ko;

    static {
        formatter.setMaximumFractionDigits(2);
    }

    /** can't be instanciated */
    private FileLib() {
    }

    /** A readable format for a file size
     * (with Mo , Ko etc ..)
     * @return formatted string or null
     * @param size file length in bytes
     */
    public static String getFormattedFileLength(long size) {
        String result;
        double dsize = size;
        if (size >= LENGTH_To) {
            result = formatter.format(dsize / LENGTH_To) + "Tb";
        } else if (size >= LENGTH_Go) {
            result = formatter.format(dsize / LENGTH_Go) + "Gb";
        } else if (size >= LENGTH_Mo) {
            result = formatter.format(dsize / LENGTH_Mo) + "Mb";
        } else if (size >= LENGTH_Ko) {
            result = formatter.format(dsize / LENGTH_Ko) + "Kb";
        } else {
            result = Long.toString(size) + i18n.getString("b");
        }
        return result;
    }

    public interface FileValidator {

        /** Verifies if the file could be created or overwrited by displaying a confirmation
     * dialog to the user if needed
     *
     * @return true if file could be created (does not exist or user accept to overwrite it)
     * @param f File to verify
     */
        public boolean verifyFile(File f);
    }

    /** Execute main split action from this parameters
     * @param source File to split
     * @param target_length choosen size of target files
     * @param todir directory that will contain target files
     * @param prefix Prefix used in naming of target files
     * @return array of target files created or null if operation failed.
     */
    public static File[] splitFile(FileValidator validator, File source, long target_length, File todir, String prefix) {
        if (target_length == 0) return null;
        if (todir == null) {
            todir = new File(System.getProperty("java.io.tmpdir"));
        }
        if (prefix == null || prefix.equals("")) {
            prefix = source.getName();
        }
        Vector result = new Vector();
        FileOutputStream fos = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(source);
            byte[] bytes = new byte[CACHE_SIZE];
            long current_target_size = 0;
            int current_target_nb = 1;
            int nbread = -1;
            try {
                File f = new File(todir, prefix + i18n.getString("targetname_suffix") + current_target_nb);
                if (!validator.verifyFile(f)) return null;
                result.add(f);
                fos = new FileOutputStream(f);
                while ((nbread = fis.read(bytes)) > -1) {
                    if ((current_target_size + nbread) > target_length) {
                        int limit = (int) (target_length - current_target_size);
                        fos.write(bytes, 0, limit);
                        fos.close();
                        current_target_nb++;
                        current_target_size = 0;
                        f = new File(todir, prefix + "_" + current_target_nb);
                        if (!validator.verifyFile(f)) return null;
                        result.add(f);
                        fos = new FileOutputStream(f);
                        fos.write(bytes, limit, nbread - limit);
                        current_target_size += nbread - limit;
                    } else {
                        fos.write(bytes, 0, nbread);
                        current_target_size += nbread;
                    }
                }
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(null, ioe, i18n.getString("Failure"), JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (fos != null) fos.close();
                } catch (IOException e) {
                }
                try {
                    if (fis != null) fis.close();
                } catch (IOException e) {
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e, i18n.getString("Failure"), JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
            }
        }
        File[] fresult = null;
        if (result.size() > 0) {
            fresult = new File[result.size()];
            fresult = (File[]) result.toArray(fresult);
        }
        return fresult;
    }

    /** Concatenate the 'sources' file into the one called 'target' in their respective order.
     * @param target
     * @param sources
     */
    public static void joinFiles(FileValidator validator, File target, File[] sources) {
        FileOutputStream fos = null;
        try {
            if (!validator.verifyFile(target)) return;
            fos = new FileOutputStream(target);
            FileInputStream fis = null;
            byte[] bytes = new byte[512];
            for (int i = 0; i < sources.length; i++) {
                fis = new FileInputStream(sources[i]);
                int nbread = 0;
                try {
                    while ((nbread = fis.read(bytes)) > -1) {
                        fos.write(bytes, 0, nbread);
                    }
                } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(null, ioe, i18n.getString("Failure"), JOptionPane.ERROR_MESSAGE);
                } finally {
                    fis.close();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e, i18n.getString("Failure"), JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
            }
        }
    }

    /** same as joinFiles but use native buffers functionnality of jdk1.4
     * @param target
     * @param sources
     * @see joinFiles
     */
    public static void nioJoinFiles(FileLib.FileValidator validator, File target, File[] sources) {
        boolean big_files = false;
        for (int i = 0; i < sources.length; i++) {
            if (sources[i].length() > Integer.MAX_VALUE) {
                big_files = true;
                break;
            }
        }
        if (big_files) {
            joinFiles(validator, target, sources);
        } else {
            System.out.println(i18n.getString("jdk14_comment"));
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(target);
                FileChannel fco = fos.getChannel();
                FileInputStream fis = null;
                for (int i = 0; i < sources.length; i++) {
                    fis = new FileInputStream(sources[i]);
                    FileChannel fci = fis.getChannel();
                    java.nio.MappedByteBuffer map;
                    try {
                        map = fci.map(FileChannel.MapMode.READ_ONLY, 0, (int) sources[i].length());
                        fco.write(map);
                        fci.close();
                    } catch (IOException ioe) {
                        JOptionPane.showMessageDialog(null, ioe, i18n.getString("Failure"), JOptionPane.ERROR_MESSAGE);
                        try {
                            fis.close();
                            fos.close();
                        } catch (IOException e) {
                        }
                    } finally {
                        fis.close();
                    }
                }
                fco.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e, i18n.getString("Failure"), JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (fos != null) fos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
