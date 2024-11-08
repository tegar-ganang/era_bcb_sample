package org.jucetice.javascript.classes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.vfs.FileSystemException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class ScriptableZipEntry extends ScriptableObject {

    static final String CLASSNAME = "ZipEntry";

    ArchiveEntry entry;

    ScriptableFile file;

    long offset;

    public ScriptableZipEntry() {
        file = null;
        entry = null;
        offset = 0;
    }

    protected ScriptableZipEntry(ScriptableFile newFile, ArchiveEntry newEntry, long newOffset) {
        file = newFile;
        entry = newEntry;
        offset = newOffset;
    }

    public static Scriptable jsConstructor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) throws IllegalArgumentException, IOException, FileSystemException, ArchiveException {
        ScriptableFile newFile = (ScriptableFile) args[0];
        ArchiveEntry newEntry = (ArchiveEntry) args[1];
        long newOffset = (long) Context.toNumber(args[2]);
        return new ScriptableZipEntry(newFile, newEntry, newOffset);
    }

    /**
     * 
     */
    public void jsFunction_extract(ScriptableFile outputFile) throws IOException, FileSystemException, ArchiveException {
        InputStream is = file.jsFunction_createInputStream();
        OutputStream output = outputFile.jsFunction_createOutputStream();
        BufferedInputStream buf = new BufferedInputStream(is);
        ArchiveInputStream input = ScriptableZipArchive.getFactory().createArchiveInputStream(buf);
        try {
            long count = 0;
            while (input.getNextEntry() != null) {
                if (count == offset) {
                    IOUtils.copy(input, output);
                    break;
                }
                count++;
            }
        } finally {
            input.close();
            output.close();
            is.close();
        }
    }

    /**
     * 
     * @return
     */
    public String jsFunction_getName() {
        return entry.getName();
    }

    /**
     * 
     * @return
     */
    public long jsFunction_getSize() {
        return entry.getSize();
    }

    /**
     * 
     * @return
     */
    public boolean jsFunction_isDirectory() {
        return entry.isDirectory();
    }

    /**
     * 
     * @return
     */
    public String getName() {
        return entry.getName();
    }

    /**
     * 
     * @return
     */
    public long getSize() {
        return entry.getSize();
    }

    /**
     * 
     * @return
     */
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    /**
     * 
     * @return
     */
    public ArchiveEntry getEntry() {
        return entry;
    }

    /**
     * 
     * @return
     */
    public String getClassName() {
        return CLASSNAME;
    }
}
