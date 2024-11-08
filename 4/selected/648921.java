package org.o14x.alpha.services;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import org.o14x.alpha.util.Messages;

/**
 * Services for files.
 * 
 * @author Olivier DANGREAUX
 */
public class FileServices {

    /**
	 * log.
	 */
    private Log log = LogFactory.getLog(FileServices.class);

    /**
	 * Regexp pattern for duplicate file base name
	 */
    private Pattern duplicateBaseNamePattern;

    private MessageFormat[] messageFormats;

    /**
	 * Creates a new instance of FileServices.
	 */
    public FileServices() {
        duplicateBaseNamePattern = Pattern.compile("(.*)_copy(\\d*)");
        messageFormats = new MessageFormat[7];
        messageFormats[0] = new MessageFormat(Messages.getString("FileServices.move_confirmation1"));
        messageFormats[1] = new MessageFormat(Messages.getString("FileServices.move_confirmation2"));
        messageFormats[2] = new MessageFormat(Messages.getString("FileServices.overwrite_confirmation1"));
        messageFormats[3] = new MessageFormat(Messages.getString("FileServices.overwrite_confirmation2"));
        messageFormats[4] = new MessageFormat(Messages.getString("FileServices.copy_confirmation1"));
        messageFormats[5] = new MessageFormat(Messages.getString("FileServices.copy_confirmation2"));
        messageFormats[6] = new MessageFormat("{0}_copy{1}{2}");
    }

    /**
	 * Gets the extension of a file.
	 * 
	 * @param file A file.
	 * 
	 * @return The extension of the file or null if the file name doesn't contain an extension.
	 */
    public String getExtension(File file) {
        String extension = null;
        int dotIndex = file.getName().lastIndexOf(".");
        if (dotIndex != -1) {
            extension = file.getName().substring(dotIndex + 1);
        }
        return extension;
    }

    /**
	 * Gets the size of the given file in bytes.
	 * 
	 * @param file A file.
	 * 
	 * @return The size of the file in bytes or 0 if the file is a directory.
	 */
    public long getSize(File file) {
        return file.length();
    }

    /**
	 * Gets the size of the given file in ko.
	 * 
	 * @param file A file.
	 * 
	 * @return The size of the file in bytes or 0 if the file is a directory.
	 */
    public long getKoSize(File file) {
        return file.length() / 1024;
    }

    /**
	 * Returns a string representation of the given file type.
	 * 
	 * @param file A file.
	 * 
	 * @return A string representation of the given file type.
	 */
    public String getFileType(File file) {
        String fileType = null;
        if (file.isDirectory()) {
            fileType = Messages.getString("FileServices.directory_file_type");
        } else {
            fileType = getExtension(file);
            if (fileType == null) {
                fileType = "";
            }
        }
        return fileType;
    }

    /**
	 * Moves somes files to a directory.
	 * 
	 * @param filePaths The array containing the files to move.
	 * @param destDir The destination directory.
	 * @param confirm A boolean indicating if a confirmation message must be shown.
	 * @param askBeforeOverwrite A boolean indicating if a confirmation message must be shown before overwriting a file.
	 */
    public void move(File[] files, File destDir, boolean confirm, boolean askBeforeOverwrite) {
        if (files != null && destDir != null) {
            boolean moveIt = true;
            if (confirm) {
                String message = "";
                if (files.length == 1) {
                    File file = files[0];
                    message = messageFormats[0].format(new String[] { file.getName(), getDisplayName(destDir) });
                } else {
                    message = messageFormats[1].format(new String[] { String.valueOf(files.length), getDisplayName(destDir) });
                }
                moveIt = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.getString("FileServices.move_dialog_title"), message);
            }
            if (moveIt) {
                for (File file : files) {
                    try {
                        if (file.isFile()) {
                            boolean reallyMoveIt = true;
                            File destinationFile = new File(destDir, file.getName());
                            if (destinationFile.exists()) {
                                if (askBeforeOverwrite) {
                                    String message = messageFormats[2].format(new String[] { file.getName(), getDisplayName(destDir) });
                                    reallyMoveIt = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.getString("FileServices.overwrite_dialog_title"), message);
                                }
                                if (reallyMoveIt) {
                                    log.debug("delete : " + destinationFile.getPath());
                                    FileUtils.forceDelete(destinationFile);
                                }
                            }
                            if (reallyMoveIt) {
                                log.debug("move : " + file.getPath() + " -> " + destDir.getPath());
                                FileUtils.moveToDirectory(file, destDir, false);
                            }
                        } else if (file.isDirectory()) {
                            File destinationFile = new File(destDir, file.getName());
                            if (destinationFile.exists()) {
                                boolean reallyMoveIt = true;
                                if (askBeforeOverwrite) {
                                    String message = messageFormats[3].format(new String[] { file.getName(), getDisplayName(destDir) });
                                    reallyMoveIt = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.getString("FileServices.overwrite_dialog_title"), message);
                                }
                                if (reallyMoveIt) {
                                    move(file.listFiles(), destinationFile, false, false);
                                }
                            } else {
                                log.debug("move : " + file.getPath() + " -> " + destDir.getPath());
                                FileUtils.moveToDirectory(file, destDir, false);
                            }
                        }
                    } catch (IOException e) {
                        log.error("Error moving file", e);
                    }
                }
            }
        }
    }

    /**
	 * Copies somes files to a directory.
	 * 
	 * @param filePaths The array containing the files to copy.
	 * @param destDir The destination directory.
	 * @param confirm A boolean indicating if a confirmation message must be shown.
	 * @param askBeforeOverwrite A boolean indicating if a confirmation message must be shown before overwriting a file.
	 */
    public void copy(File[] files, File destDir, boolean confirm, boolean askBeforeOverwrite) {
        if (files != null && destDir != null) {
            boolean copyIt = true;
            if (confirm) {
                String message = "";
                if (files.length == 1) {
                    File file = files[0];
                    message = messageFormats[4].format(new String[] { file.getName(), getDisplayName(destDir) });
                } else {
                    message = messageFormats[5].format(new String[] { String.valueOf(files.length), getDisplayName(destDir) });
                }
                copyIt = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.getString("FileServices.copy_dialog_title"), message);
            }
            if (copyIt) {
                for (File file : files) {
                    try {
                        if (file.isFile()) {
                            boolean reallyCopyIt = true;
                            File destinationFile = new File(destDir, file.getName());
                            if (destinationFile.exists()) {
                                if (askBeforeOverwrite) {
                                    String message = messageFormats[2].format(new String[] { file.getName(), getDisplayName(destDir) });
                                    reallyCopyIt = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.getString("FileServices.overwrite_dialog_title"), message);
                                }
                            }
                            if (reallyCopyIt) {
                                log.debug("copy : " + file.getPath() + " -> " + destDir.getPath());
                                FileUtils.copyFileToDirectory(file, destDir);
                            }
                        } else if (file.isDirectory()) {
                            File destinationFile = new File(destDir, file.getName());
                            if (destinationFile.exists()) {
                                boolean reallyCopyIt = true;
                                if (askBeforeOverwrite) {
                                    String message = messageFormats[3].format(new String[] { file.getName(), getDisplayName(destDir) });
                                    reallyCopyIt = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.getString("FileServices.overwrite_dialog_title"), message);
                                }
                                if (reallyCopyIt) {
                                    copy(file.listFiles(), destinationFile, false, false);
                                }
                            } else {
                                log.debug("copy : " + file.getPath() + " -> " + destDir.getPath());
                                FileUtils.copyDirectoryToDirectory(file, destDir);
                            }
                        }
                    } catch (IOException e) {
                        log.error("Error moving file", e);
                    }
                }
            }
        }
    }

    /**
	 * Copies somes files to a directory and renames them if they allready exist in the target directory.
	 * 
	 * @param filePaths The array containing the files to copy.
	 * @param destDir The destination directory.
	 */
    public void duplicate(File[] files, File destDir) {
        for (File file : files) {
            File duplicateFile = getDuplicateFile(file, destDir);
            if (file.isDirectory()) {
                try {
                    log.debug("duplicate directory : " + file.getPath() + " -> " + duplicateFile.getPath());
                    FileUtils.copyDirectory(file, duplicateFile);
                } catch (IOException e) {
                    log.error("Error duplicating directory", e);
                }
            } else {
                try {
                    log.debug("duplicate file : " + file.getPath() + " -> " + duplicateFile.getPath());
                    FileUtils.copyFile(file, duplicateFile);
                } catch (IOException e) {
                    log.error("Error duplicating file", e);
                }
            }
        }
    }

    /**
	 * Gets the file to be used as a duplicata of a file to be copied in a directory.
	 * The name of the duplicata is cimputed in order not to averwrite any file.
	 * 
	 * @param file The file to be copied.
	 * @param destDir The destination directory.
	 * 
	 * @return The file to be used as a duplicata.
	 */
    public File getDuplicateFile(File file, File destDir) {
        String fileName = file.getName();
        File duplicateFile = new File(destDir, fileName);
        if (duplicateFile.exists()) {
            String extension = FilenameUtils.getExtension(fileName);
            if (extension.length() > 0) {
                extension = "." + extension;
            }
            String baseName = FilenameUtils.getBaseName(fileName);
            Matcher matcher = duplicateBaseNamePattern.matcher(baseName);
            if (matcher.matches()) {
                baseName = matcher.group(1);
            }
            int counter = 0;
            while (duplicateFile.exists()) {
                counter++;
                duplicateFile = new File(destDir, messageFormats[6].format(new String[] { baseName, String.valueOf(counter), extension }));
            }
        }
        return duplicateFile;
    }

    /**
	 * Converts an array of file paths to an array of File objects.
	 * 
	 * @param paths The array of file paths to convert.
	 * 
	 * @return The converted array of File objects.
	 */
    public File[] toFiles(String[] paths) {
        File[] files = new File[paths.length];
        for (int i = 0; i < paths.length; i++) {
            files[i] = new File(paths[i]);
        }
        return files;
    }

    /**
	 * Returns the display name for the specified file.
	 * 
	 * @param file A file.
	 * 
	 * @return The display name for the specified file.
	 */
    public String getDisplayName(File file) {
        String displayName = null;
        if (file != null) {
            if (file.getParent() == null) {
                displayName = file.getPath();
            } else {
                displayName = file.getName();
            }
        }
        return displayName;
    }
}
