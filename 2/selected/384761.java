package org.rapla.components.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.avalon.framework.CascadingException;

/** Some IOHelper methods. */
public abstract class IOUtil {

    /** returns the path of the url without the last path component */
    public static URL getBase(URL url) {
        try {
            String file = url.getPath();
            String separator = "/";
            if (url.getProtocol().equals("file") && file.indexOf(File.separator) > 0) {
                separator = File.separator;
            }
            int index = file.lastIndexOf(separator);
            String dir = (index < 0) ? file : file.substring(0, index + 1);
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), dir);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Unknown error while getting the base of the url!");
        }
    }

    /** reads the content form an url into a ByteArray*/
    public static byte[] readBytes(URL url) throws IOException {
        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            in = url.openStream();
            byte[] buffer = new byte[1024];
            int count = 0;
            do {
                out.write(buffer, 0, count);
                count = in.read(buffer, 0, buffer.length);
            } while (count != -1);
            return out.toByteArray();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /** same as {@link URLDecoder#decode}.
     *   But calls the deprecated method under 1.3.
     */
    public static String decode(String s, String enc) throws UnsupportedEncodingException {
        return callEncodeDecode(URLDecoder.class, "decode", s, enc);
    }

    /** same as {@link URLEncoder#encode}.
     * But calls the deprecated method under 1.3.
     */
    public static String encode(String s, String enc) throws UnsupportedEncodingException {
        return callEncodeDecode(URLEncoder.class, "encode", s, enc);
    }

    private static String callEncodeDecode(Class clazz, String methodName, String s, String enc) throws UnsupportedEncodingException {
        Assert.notNull(s);
        Assert.notNull(enc);
        try {
            Method method = clazz.getMethod(methodName, new Class[] { String.class, String.class });
            return (String) method.invoke(null, new Object[] { s, enc });
        } catch (NoSuchMethodException ex) {
            try {
                Method method = URLDecoder.class.getMethod(methodName, new Class[] { String.class });
                return (String) method.invoke(null, new Object[] { s });
            } catch (Exception ex2) {
                ex2.printStackTrace();
                throw new IllegalStateException("Should not happen" + ex2.getMessage());
            }
        } catch (InvocationTargetException ex) {
            throw (UnsupportedEncodingException) ex.getTargetException();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Should not happen" + ex.getMessage());
        }
    }

    /**  returns a BufferedInputStream from the url.
         If the url-protocol is "file" no url connection will
         be opened.
     */
    public static InputStream getInputStream(URL url) throws IOException {
        if (url.getProtocol().equals("file")) {
            String path = decode(url.getPath(), "UTF-8");
            return new BufferedInputStream(new FileInputStream(path));
        } else {
            return new BufferedInputStream(url.openStream());
        }
    }

    public static File getFileFrom(URL url) throws IOException {
        String path = decode(url.getPath(), "UTF-8");
        return new File(path);
    }

    /** copies a file.
    * @param srcPath the source-path. Thats the path of the file that should be copied.
    * @param destPath the destination-path
      */
    public static void copy(String srcPath, String destPath) throws IOException {
        copy(srcPath, destPath, false);
    }

    /** copies a file.
      * @param srcPath the source-path. Thats the path of the file that should be copied.
      * @param destPath the destination-path
        */
    public static void copy(String srcPath, String destPath, boolean onlyOverwriteIfNewer) throws IOException {
        copy(new File(srcPath), new File(destPath), onlyOverwriteIfNewer);
    }

    /** copies a file.
        */
    public static void copy(File srcFile, File destFile, boolean onlyOverwriteIfNewer) throws IOException {
        if (!srcFile.exists()) {
            throw new IOException(srcFile.getPath() + " doesn't exist!!");
        }
        if (destFile.exists() && destFile.lastModified() >= srcFile.lastModified() && onlyOverwriteIfNewer) {
            return;
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(destFile);
            copyStreams(in, out);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    /** copies the contents of the input stream to the output stream.
       * @param in
       * @param out
       * @throws IOException
       */
    public static void copyStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[32000];
        int n = 0;
        while (n != -1) {
            out.write(buf, 0, n);
            n = in.read(buf, 0, buf.length);
        }
    }

    /** returns the relative path of file to base.
         * @throws IOException if position of file is not relative  to base
        */
    public static String getRelativePath(File base, File file) throws IOException {
        String filePath = file.getAbsoluteFile().getCanonicalPath();
        String basePath = base.getAbsoluteFile().getCanonicalPath();
        int start = filePath.indexOf(basePath);
        if (start != 0) throw new IOException(basePath + " not ancestor of " + filePath);
        return filePath.substring(basePath.length());
    }

    /** returns the relative path of file to base.
         * same as {@link #getRelativePath(File, File)} but replaces windows-plattform-specific
         * file separator <code>\</code> with <code>/</code>
         * @throws IOException if position of file is not relative  to base
         */
    public static String getRelativeURL(File base, File file) throws IOException {
        StringBuffer result = new StringBuffer(getRelativePath(base, file));
        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == '\\') result.setCharAt(i, '/');
        }
        return result.toString();
    }

    public static File[] getJarFiles(String baseDir, String dirList) throws IOException {
        ArrayList completeList = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(dirList, ",");
        while (tokenizer.hasMoreTokens()) {
            File jarDir = new File(baseDir, tokenizer.nextToken());
            if (jarDir.exists() && jarDir.isDirectory()) {
                File[] jarFiles = jarDir.listFiles();
                for (int i = 0; i < jarFiles.length; i++) {
                    if (jarFiles[i].getAbsolutePath().endsWith(".jar")) {
                        completeList.add(jarFiles[i].getCanonicalFile());
                    }
                }
            }
        }
        return (File[]) completeList.toArray(new File[] {});
    }

    public static String getStackTraceAsString(Throwable ex) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bytes, true);
        writer.println("<h2>" + ex.getMessage() + "</h2><br/>");
        ex.printStackTrace(writer);
        while (ex instanceof CascadingException) {
            ex = ((CascadingException) ex).getCause();
            if (ex != null) {
                writer.println("<br/><h2>Caused by: " + ex.getMessage() + "</h2><br/>");
                ex.printStackTrace(writer);
            } else {
                break;
            }
        }
        return bytes.toString();
    }
}
