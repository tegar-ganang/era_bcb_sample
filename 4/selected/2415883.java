package com.ctrcv.framework.persistence;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ctrcv.site.SiteConstants;

public class LocalFilePersistence extends FilePersistence {

    static Logger LOGGER = LoggerFactory.getLogger(LocalFilePersistence.class);

    public static String DATA_DIR = SiteConstants.BOARD_PATH_PREFIX;

    static {
        File f = new File(DATA_DIR);
        if (!f.isDirectory()) {
            f.mkdir();
        }
    }

    @Override
    public byte[] read(String path) throws PersistenceException {
        InputStream reader = null;
        ByteArrayOutputStream sw = new ByteArrayOutputStream();
        try {
            reader = new FileInputStream(path);
            IOUtils.copy(reader, sw);
        } catch (Exception e) {
            LOGGER.error("fail to read file - " + path, e);
            throw new PersistenceException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("fail to close reader", e);
                }
            }
        }
        return sw.toByteArray();
    }

    @Override
    public void write(String path, InputStream is) throws PersistenceException {
        Writer out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(path), "utf-8");
            IOUtils.copy(is, out);
        } catch (IOException e) {
            LOGGER.error("fail to write file", e);
            throw new PersistenceException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    out = null;
                }
            }
        }
    }

    @Override
    public String[] list(String path) {
        return new File(path).list();
    }

    @Override
    public void delete(String path) throws PersistenceException {
        boolean result = new File(path).delete();
        if (!result) throw new PersistenceException("fail to delete the file - " + path);
    }
}
