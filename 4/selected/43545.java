package net.sf.refactorit.utils;

import net.sf.refactorit.common.exception.SystemException;
import net.sf.refactorit.common.util.FileCopier;
import net.sf.refactorit.common.util.StringUtil;
import net.sf.refactorit.exception.ErrorCodes;
import net.sf.refactorit.refactorings.undo.IUndoableEdit;
import net.sf.refactorit.refactorings.undo.IUndoableTransaction;
import net.sf.refactorit.refactorings.undo.RitUndoManager;
import net.sf.refactorit.refactorings.undo.SourceInfo;
import net.sf.refactorit.test.Utils;
import net.sf.refactorit.vfs.Source;
import net.sf.refactorit.vfs.SourcePathFilter;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *	Set of static functions to deal with file IO.
 * @author Siim Kaalep
 */
public final class FileUtil {

    public static final String JAVA_FILE_EXT = ".java";

    public static final String HTM_FILE_EXT = ".htm";

    public static final String XML_FILE_EXT = ".xml";

    public static final String HTML_FILE_EXT = ".html";

    public static final String CSV_FILE_EXT = ".csv";

    public static final String TEXT_FILE_EXT = ".txt";

    static final String JSP_FILE_EXT = ".jsp";

    public static boolean isJavaFile(String name) {
        if (name != null) {
            return name.endsWith(JAVA_FILE_EXT);
        }
        return false;
    }

    public static final boolean isJspFile(Source source) {
        return isJspFile(source.getName());
    }

    public static boolean isJspFile(String name) {
        if (name != null) {
            return name.endsWith(JSP_FILE_EXT);
        }
        return false;
    }

    public static boolean isHtmlFile(String name) {
        if (name != null) {
            return (name.endsWith(HTM_FILE_EXT) || name.endsWith(HTML_FILE_EXT));
        }
        return false;
    }

    public static boolean isXmlFile(String name) {
        if (name != null) {
            return name.endsWith(XML_FILE_EXT);
        }
        return false;
    }

    public static boolean isCsvFile(String name) {
        if (name != null) {
            return name.endsWith(CSV_FILE_EXT);
        }
        return false;
    }

    public static boolean isPlainTextFile(String name) {
        if (name != null) {
            return name.endsWith(TEXT_FILE_EXT);
        }
        return false;
    }

    public static String extractFileNameFromPath(final String filePath, char separatorChar) {
        int index = filePath.lastIndexOf(separatorChar);
        if (index < 0) {
            return filePath;
        }
        return filePath.substring(index + 1);
    }

    public static String extractPathUpToLastSeparator(final String filePath, final char separatorChar) {
        int index = filePath.lastIndexOf(separatorChar);
        if (index < 0) {
            return "";
        }
        return filePath.substring(0, index);
    }

    /**
   * Recursively removes directory and all files in it
   * @param dir directory to remove
   */
    public static void removeDirectory(File dir, SourcePathFilter filter) {
        if (!dir.exists() || (!filter.acceptDirectoryByName(dir.getName()))) {
            return;
        }
        emptyDirectory(dir, filter);
        dir.delete();
    }

    /**
   * deletes all files and subdir in dir
   * @param dir
   */
    public static void emptyDirectory(File dir, SourcePathFilter filter) {
        String[] files = dir.list();
        for (int q = 0; q < files.length; q++) {
            File file = new File(dir, files[q]);
            if (file.isDirectory()) {
                removeDirectory(file, filter);
            } else {
                file.delete();
            }
        }
    }

    /**
   * Replaces all slashes and back-slashes to current system path separator
   * @param path not system specific filename
   * @return filename with system specific slashes
   */
    public static String useSystemPS(String path) {
        String temp = StringUtil.replace(path, "\\", File.separator);
        return StringUtil.replace(temp, "/", File.separator);
    }

    public static String getCommonPath(String package1, String package2) {
        return extractPathUpToLastDot(StringUtil.getCommonPart(package1 + '.', package2 + '.'));
    }

    public static String extractPathUpToLastDot(String s) {
        int pos = s.lastIndexOf('.');
        if (pos == -1) {
            return "";
        } else {
            return s.substring(0, pos);
        }
    }

    public static String getAbsolutePath(String path) {
        if (!new File(path).isAbsolute()) {
            return new File(Utils.getTestProjectsDirectory(), path).getAbsoluteFile().getAbsolutePath();
        } else {
            return path;
        }
    }

    /**
   * Copies source to the specified destination. Creates destination if it does
   * not exists. In case destination is file that exists it is overwritten. In
   * case source file is copied to a directory a file having same name as source
   * file is created in that directory.
   *
   * @param source file/directory to be copied.
   * @param destination destination where to create copy.
   *
   * @throws IOException if an I/O exception occurs during the copying.
   * @throws IllegalArgumentException if <code>source</code> is neither a file
   *         nor a directory.
   * @throws IllegalArgumentException if <code>source</code> is a directory and
   *         <code>destination</code> is not a directory.
   */
    public static void copy(Source source, Source destination) throws IOException {
        if (source == null) {
            throw new NullPointerException("source is null");
        }
        if (destination == null) {
            throw new NullPointerException("destination is null for source: " + source);
        }
        if (source.isFile()) {
            if (destination.isDirectory()) {
                IUndoableTransaction trans = RitUndoManager.getCurrentTransaction();
                IUndoableEdit undo = null;
                if (trans != null) {
                    undo = trans.createCreateFileUndo(new SourceInfo(destination, source.getName()));
                }
                Source destinationFile = destination.createNewFile(source.getName());
                if (trans != null && destinationFile != null) {
                    trans.addEdit(undo);
                }
                copy(source, destinationFile);
            } else {
                OutputStream out = null;
                InputStream in = null;
                try {
                    in = source.getInputStream();
                    out = new BufferedOutputStream(destination.getOutputStream());
                    FileCopier.pump(in, out, 8192, false);
                    out.flush();
                } finally {
                    if (in != null) {
                        in.close();
                        in = null;
                    }
                    if (out != null) {
                        out.close();
                        out = null;
                    }
                }
            }
        } else if (source.isDirectory()) {
            if (!"CVS".equals(source.getName())) {
                if (destination.isDirectory()) {
                    final Source[] children = source.getChildren();
                    if (children != null) {
                        for (int i = 0, len = children.length; i < len; i++) {
                            final Source child = children[i];
                            if (child.isDirectory()) {
                                if (!"CVS".equals(child.getName())) {
                                    Source childDestination = destination.getChild(child.getName());
                                    if (childDestination == null) {
                                        childDestination = destination.mkdir(child.getName());
                                    }
                                    if (childDestination == null) {
                                        throw new IOException("Failed to create destination directory (" + childDestination + ") for " + child + " during copying");
                                    }
                                    copy(child, childDestination);
                                }
                            } else {
                                copy(child, destination);
                            }
                        }
                    }
                } else {
                    if (destination.isFile()) {
                        throw new IllegalArgumentException("Cannot copy directory (" + source + ") into a file (" + destination + ")");
                    } else {
                        throw new IllegalArgumentException("Cannot copy source directory (" + source + " into a non-existent file/directory (" + destination + ")");
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("source (" + source + ") is neither a file nor a directory");
        }
    }

    public static PathElement extractPathElement(String path, char separatorChar) {
        String dir = extractPathUpToLastSeparator(path, separatorChar);
        String file = extractFileNameFromPath(path, separatorChar);
        return new PathElement(dir, file);
    }

    public static void fixCrLf(File f) {
        if (f.isFile()) {
            FileCopier.writeStringToFile(f, LinePositionUtil.useUnixNewlines(FileCopier.readFileToString(f)));
        } else {
            File[] children = f.listFiles();
            for (int i = 0; i < children.length; i++) {
                fixCrLf(children[i]);
            }
        }
    }

    /**
   *
   * @param parentSrc
   * @param src
   * @return relative path from parentSrc to src
   * @throws SystemException if parentSrc is not parent of src
   */
    public static String getRelativePathFrom(final Source parentSrc, final Source src) throws SystemException {
        Source parent = src.getParent();
        if (parentSrc.equals(src)) {
            return "";
        } else if (parent == null) {
            throw new SystemException(ErrorCodes.INTERNAL_ERROR, parentSrc + " is not parent for " + src);
        } else if (parentSrc.equals(parent)) {
            return src.getName();
        }
        String path = getRelativePathFrom(parentSrc, parent);
        if (path.length() == 0) {
            return src.getName();
        }
        return path + src.getSeparatorChar() + src.getName();
    }
}
