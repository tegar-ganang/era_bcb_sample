package net.hussnain.io;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

/**
 *
 * @author  BillGates
 * @version
 */
public class Utilities {

    private static Logger logger = Logger.getLogger("RabtPad");

    /** Creates new FileUtilities */
    public Utilities() {
    }

    public static final String parseExtension(String fileName) {
        String ext = null;
        if (fileName != null) {
            int i = fileName.lastIndexOf('.');
            if (i > 0 && i < fileName.length() - 1) {
                ext = fileName.substring(i + 1).toLowerCase();
            }
        }
        return ext;
    }

    /**
     * copy file with stream channels from one location to another
     */
    public static final void copyFile(File source, File target) {
        try {
            FileChannel srcChannel = new FileInputStream(source).getChannel();
            FileChannel dstChannel = new FileOutputStream(target).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (java.io.IOException e) {
        }
    }

    /**
     * checks file extension for the target file, if necessary creates new file
     */
    public static final File assignFileExtension(File file, String fileExtension) {
        String userExtention = net.hussnain.io.Utilities.parseExtension(file.getName());
        String file_name = file.getName();
        if (userExtention == null) {
            file_name = file.getName() + "." + fileExtension;
            String parentDirectory = file.getParent();
            File parentDirectoryFile = new File(parentDirectory);
            file = new File(parentDirectoryFile, file_name);
        } else {
            if (!userExtention.equals(fileExtension)) {
                file_name = file.getName() + "." + fileExtension;
                file = new File(new File(file.getParent()), file_name);
            }
        }
        return file;
    }

    /**
     * p_type is -1 for exporting
     */
    public static final java.io.File openFileDialog(java.awt.Component p_owner, java.io.File p_workingDirectory, int p_type) {
        java.io.File var_File = null;
        javax.swing.JFileChooser fd = new javax.swing.JFileChooser();
        fd.setCurrentDirectory(p_workingDirectory);
        net.hussnain.utils.UIUtil.setLocationToMid(fd);
        int action = 0;
        if (p_type == fd.OPEN_DIALOG) {
            fd.addChoosableFileFilter(RabtFileFilter.createDocumentFilter());
            action = fd.showOpenDialog(p_owner);
        } else {
            if (p_type == fd.SAVE_DIALOG) {
                fd.addChoosableFileFilter(RabtFileFilter.createDocumentFilter());
                action = fd.showSaveDialog(p_owner);
            } else {
                if (p_type == -1) {
                    fd.addChoosableFileFilter(RabtFileFilter.createExportFilter());
                    action = fd.showSaveDialog(p_owner);
                }
            }
        }
        if (action == javax.swing.JFileChooser.APPROVE_OPTION) {
            if (fd.getSelectedFile() == null) return var_File;
            var_File = fd.getSelectedFile();
        }
        return var_File;
    }

    public static final java.net.URL openFileDialogForUrl(java.awt.Component p_owner, java.io.File p_workingDirectory, int p_type) {
        java.net.URL url = null;
        java.io.File var_File = openFileDialog(p_owner, p_workingDirectory, p_type);
        if (var_File != null) {
            try {
                if (var_File.isFile()) {
                    url = var_File.toURI().toURL();
                }
            } catch (java.net.MalformedURLException e) {
                logger.warning("bad conversion from file to url. ");
            }
        }
        return url;
    }

    /**
     * Reads a true type font from the given path.
     */
    public static Font createNewFont(Class classRef, String path) {
        Font newFont = null;
        try {
            InputStream fontStream = classRef.getResourceAsStream(path);
            if (fontStream != null) {
                logger.info("Adding font from file " + path);
                newFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                fontStream.close();
            }
        } catch (Exception e) {
            logger.warning("error reading from font stream.");
        }
        return newFont;
    }
}
