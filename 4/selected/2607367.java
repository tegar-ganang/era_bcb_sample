package com.googlecode.usc.folder.compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import com.googlecode.usc.folder.compression.utils.CompressionUtil;

/**
 *
 * @author ShunLi
 */
public abstract class Strategy {

    public void doCompress(File[] files, File out, List<String> excludedKeys) {
        Map<String, File> map = new HashMap<String, File>();
        String parent = FilenameUtils.getBaseName(out.getName());
        for (File f : files) {
            CompressionUtil.list(f, parent, map, excludedKeys);
        }
        if (!map.isEmpty()) {
            FileOutputStream fos = null;
            ArchiveOutputStream aos = null;
            InputStream is = null;
            try {
                fos = new FileOutputStream(out);
                aos = getArchiveOutputStream(fos);
                for (Map.Entry<String, File> entry : map.entrySet()) {
                    File file = entry.getValue();
                    ArchiveEntry ae = getArchiveEntry(file, entry.getKey());
                    aos.putArchiveEntry(ae);
                    if (file.isFile()) {
                        IOUtils.copy(is = new FileInputStream(file), aos);
                        IOUtils.closeQuietly(is);
                        is = null;
                    }
                    aos.closeArchiveEntry();
                }
                aos.finish();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(aos);
                IOUtils.closeQuietly(fos);
            }
        }
    }

    /**
     * @param out
     * @return ArchiveOutputStream
     * @throws FileNotFoundException
     */
    public abstract ArchiveOutputStream getArchiveOutputStream(FileOutputStream fos) throws IOException;

    /**
     * @param entry
     * @param file
     * @return ArchiveEntry
     */
    public abstract ArchiveEntry getArchiveEntry(File inputFile, String entryName);
}
