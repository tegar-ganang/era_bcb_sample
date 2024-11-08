package net.sf.poormans.utils.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Just for encapsulating the zipping stuff.
 *
 * @version $Id: Zip.java 694 2006-12-22 16:07:21Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
public class Zip {

    public static void compress(final File zip, final Map<InputStream, String> entries) throws IOException {
        if (zip == null || entries == null || CollectionUtils.isEmpty(entries.keySet())) throw new IllegalArgumentException("One ore more parameters are empty!");
        if (zip.exists()) zip.delete(); else if (!zip.getParentFile().exists()) zip.getParentFile().mkdirs();
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zip)));
        out.setLevel(Deflater.BEST_COMPRESSION);
        InputStream in = null;
        try {
            for (InputStream inputStream : entries.keySet()) {
                in = inputStream;
                ZipEntry zipEntry = new ZipEntry(skipBeginningSlash(entries.get(in)));
                out.putNextEntry(zipEntry);
                IOUtils.copy(in, out);
                out.closeEntry();
                in.close();
            }
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    /**
	 * Static method to compress all files in 'entries' into 'zip'. Each entry has a File and its String representation in the zip.
	 * Just a wrapper to {@link #compress(File, Map)}.
	 *
	 * Usage:
	 * <pre>
	 * Map&lt;File, String&gt; entries = new HashMap&lt;File, String&gt;();
	 * entries.put(new File(&quot;/tmp/db.xml&quot;), &quot;data/db.xml&quot;);
	 * ZIP.compress(new File(&quot;/tmp/test.zip&quot;), entries);
	 * </pre>
	 *
	 * @param zip
	 * @param entries
	 * @throws IOException
	 */
    public static void compressFiles(final File zip, final Map<File, String> entries) throws IOException {
        if (zip == null || entries == null || CollectionUtils.isEmpty(entries.keySet())) throw new IllegalArgumentException("One ore more parameters are empty!");
        Map<InputStream, String> newEntries = new HashMap<InputStream, String>(entries.size());
        for (File file : entries.keySet()) {
            newEntries.put(new FileInputStream(file), entries.get(file));
        }
        compress(zip, newEntries);
    }

    private static String skipBeginningSlash(final String string) {
        return (StringUtils.isNotBlank(string) && string.startsWith("/")) ? string.substring(1) : string;
    }

    /**
	 * Just a wrapper to {@link #getEntryInfo(File, String)}.
	 *
	 * @throws IOException
	 */
    public static Map<String, ZipEntryInfo> getEntryInfo(final File zip) throws IOException {
        return getEntryInfo(zip, null);
    }

    /**
	 * @return A Map that contains a string representation and the {@link ZipEntryInfo} of itself.
	 *
	 * @throws IOException
	 */
    public static Map<String, ZipEntryInfo> getEntryInfo(final File zip, final String dirToFilter) throws IOException {
        if (!zip.exists()) throw new IllegalArgumentException("Zip file doesn't exists!");
        Map<String, ZipEntryInfo> infos = new HashMap<String, ZipEntryInfo>();
        ZipFile zipFile = new ZipFile(zip);
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            if (StringUtils.isBlank(dirToFilter) || zipEntry.getName().startsWith(dirToFilter)) infos.put(zipEntry.getName(), new ZipEntryInfo(zipEntry, zipFile.getInputStream(zipEntry)));
        }
        return infos;
    }

    /**
	 * Methode to extract all {@link ZipEntryInfo}s into 'destDir'. Inner directory structure will be copied.
	 *
	 * @param destDir
	 * @param entryInfos
	 * @throws IOException
	 */
    public static void extract(final File destDir, final Collection<ZipEntryInfo> entryInfos) throws IOException {
        if (destDir == null || CollectionUtils.isEmpty(entryInfos)) throw new IllegalArgumentException("One or parameter is null or empty!");
        if (!destDir.exists()) destDir.mkdirs();
        for (ZipEntryInfo entryInfo : entryInfos) {
            ZipEntry entry = entryInfo.getZipEntry();
            InputStream in = entryInfo.getInputStream();
            File entryDest = new File(destDir, entry.getName());
            entryDest.getParentFile().mkdirs();
            if (!entry.isDirectory()) {
                OutputStream out = new FileOutputStream(new File(destDir, entry.getName()));
                try {
                    IOUtils.copy(in, out);
                    out.flush();
                } finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                }
            }
        }
    }

    /**
	 * @return An InputStream of the requested entry.
	 * @throws IOException
	 */
    public static InputStream toInputstream(final File zip, final String entryName) throws IOException {
        Map<String, ZipEntryInfo> infos = getEntryInfo(zip);
        if (infos.keySet().contains(entryName)) return infos.get(entryName).getInputStream();
        return null;
    }

    /**
	 * Just a wrapper to {@link #toInputstream(File, String)}.
	 * @throws IOException
	 */
    public static String toString(final File zip, final String entryName) throws IOException {
        InputStream in = toInputstream(zip, entryName);
        return (in == null) ? null : IOUtils.toString(in);
    }
}
