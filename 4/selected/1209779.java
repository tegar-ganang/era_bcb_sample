package org.dengues.commons.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import com.csvreader.CsvReader;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf 2007-12-3 qiang.zhang $
 * 
 */
public class FileUtils {

    private static final String ENCODING = "UTF-8";

    private static final String RELATIVE_STR = "..";

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getStoragePath".
     * 
     * @param path
     * @param root
     * @return
     */
    public static IPath getStoragePath(IPath path, String root) {
        String[] segments = path.segments();
        path = path.removeLastSegments(1);
        for (String string : segments) {
            if (root.equals(string)) {
                path = path.removeFirstSegments(1);
                break;
            } else {
                path = path.removeFirstSegments(1);
            }
        }
        return path;
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "copy".
     * 
     * @param source
     * @param target
     */
    public static void copy(File source, File target) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(target);
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getRelativePath".
     * 
     * @param urlPathstr
     * @param modelPathstr
     * @return
     */
    public static String getRelativePath(String urlPathstr, String modelPathstr) {
        StringBuffer res = new StringBuffer();
        IPath urlPath = new Path(urlPathstr);
        IPath modelPath = new Path(modelPathstr);
        if (urlPathstr.indexOf("://") > 0) {
            return urlPathstr;
        }
        int matchingSegments = urlPath.matchingFirstSegments(modelPath);
        int backSegments = modelPath.segmentCount() - matchingSegments - 1;
        while (backSegments > 0) {
            res.append(RELATIVE_STR);
            res.append(File.separatorChar);
            backSegments--;
        }
        int segCount = urlPath.segmentCount();
        for (int i = matchingSegments; i < segCount; i++) {
            if (i > matchingSegments) {
                res.append(File.separatorChar);
            }
            res.append(urlPath.segment(i));
        }
        return res.toString();
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getAbsolutePath".
     * 
     * @param strurlPath
     * @param strmodelPath
     * @return
     */
    public static String getAbsolutePath(String strurlPath, String strmodelPath) {
        IPath urlPath = new Path(strurlPath);
        if (urlPath.isAbsolute()) return strurlPath;
        IPath modelPath = new Path(strmodelPath);
        int rel_level = 0;
        for (int i = 0; i < urlPath.segmentCount(); i++) {
            if (urlPath.segment(i).equals(RELATIVE_STR)) {
                rel_level++;
            }
        }
        urlPath = urlPath.removeFirstSegments(rel_level);
        modelPath = modelPath.removeLastSegments(rel_level + 1);
        urlPath = modelPath.append(urlPath);
        return urlPath.toOSString();
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getOSPath".
     * 
     * @param path
     * @return
     */
    public static String getOSPath(String path) {
        Path path2 = new Path(path);
        return path2.toOSString();
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getPortablePath".
     * 
     * @param path
     * @return
     */
    public static String getPortablePath(String path) {
        Path path2 = new Path(path);
        return path2.toPortableString();
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getFileFromBundle".
     * 
     * @param plugin
     * @param path
     * @return
     */
    public static File getFileFromBundle(String plugin, String path) {
        Bundle b = Platform.getBundle(plugin);
        Path filePath = new Path(path);
        URL url;
        try {
            if (b != null) {
                url = FileLocator.toFileURL(FileLocator.find(b, filePath, null));
                File dir = new File(url.getPath());
                return dir;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Return the contents of the Stream as a String. Note: If the InputStream represents a null String, the Java
     * implementation will try to read from the stream for a certain amount of time before timing out.
     * 
     * @param is the InputStream to transform into a String
     * @return the String representation of the Stream
     */
    public static String getStringFromStream(InputStream is) {
        if (null == is) return null;
        try {
            InputStreamReader reader = new InputStreamReader(is);
            char[] buffer = new char[1024];
            StringWriter writer = new StringWriter();
            int bytes_read;
            while ((bytes_read = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytes_read);
            }
            return (writer.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != is) try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "escapeSpace".
     * 
     * @param name
     * @return
     */
    public static String escapeSpace(String name) {
        return name != null ? name.replace(" ", "") : "";
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "createIFile".
     * 
     * @param file
     * @param content
     */
    public static void createIFile(IProgressMonitor monitor, IFile file, String content) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("save file...", 2000);
            ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
            manager.connect(file.getFullPath(), LocationKind.IFILE, monitor);
            ITextFileBuffer buffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
            buffer.getDocument().set(content);
            buffer.commit(monitor, true);
            manager.disconnect(file.getFullPath(), LocationKind.IFILE, monitor);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            monitor.done();
        }
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getCSVArray".
     * 
     * @param is
     * @return
     */
    public static List<String[]> getCSVArray(File is) {
        List<String[]> rows = new ArrayList<String[]>();
        try {
            if (is.exists()) {
                CsvReader csvReader = new CsvReader(new BufferedReader(new InputStreamReader(new java.io.FileInputStream(is), ENCODING)), ';');
                csvReader.setRecordDelimiter('\n');
                csvReader.setSkipEmptyRecords(true);
                csvReader.setTextQualifier('"');
                csvReader.setEscapeMode(com.csvreader.CsvReader.ESCAPE_MODE_DOUBLED);
                while (csvReader.readRecord()) {
                    rows.add(csvReader.getValues());
                }
                csvReader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is.exists()) {
                is.deleteOnExit();
            }
        }
        return rows;
    }
}
