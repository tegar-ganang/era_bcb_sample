package org.t2framework.samples.reviewme.project.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.t2framework.commons.exception.IORuntimeException;
import org.t2framework.commons.util.Assertion;
import org.t2framework.commons.util.CloseableUtil;
import org.t2framework.samples.reviewme.project.ProjectContext;
import org.t2framework.samples.reviewme.project.UploadFileExtracter;

public class UploadFileExtracterImpl implements UploadFileExtracter {

    protected File rootDir;

    protected static final int BUF_SIZE = 1024;

    public UploadFileExtracterImpl(File rootDir) {
        Assertion.notNull(rootDir);
        if (rootDir.isDirectory() == false) {
            throw new IllegalStateException();
        }
        this.rootDir = rootDir;
    }

    @Override
    public ProjectContext extract(final InputStream input) {
        Assertion.notNull(input);
        ZipInputStream is = null;
        try {
            is = new ZipInputStream(new BufferedInputStream(input));
            ZipEntry entry = null;
            while ((entry = is.getNextEntry()) != null) {
                final String name = entry.getName();
                if (entry.isDirectory()) {
                    new File(this.rootDir, name).mkdirs();
                } else {
                    new File(this.rootDir, new File(name).getParent()).mkdirs();
                    File file = new File(this.rootDir, name);
                    CheckedOutputStream out = new CheckedOutputStream(new BufferedOutputStream(new FileOutputStream(file)), new CRC32());
                    byte[] buf = new byte[BUF_SIZE];
                    int writeSize = 0;
                    int totalSize = 0;
                    while ((writeSize = is.read(buf)) != -1) {
                        totalSize += writeSize;
                        out.write(buf, 0, writeSize);
                    }
                    out.close();
                }
                is.closeEntry();
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            CloseableUtil.close(is);
            CloseableUtil.close(input);
        }
        return null;
    }
}
