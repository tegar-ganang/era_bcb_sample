package com.lonelytaste.narafms.loader.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import com.lonelytaste.narafms.core.AbstractFileUploader;

/**
 * <p> Title: [名称]</p>
 * <p> Description: [描述]</p>
 * <p> Created on May 12, 2009</p>
 * <p> Copyright: Copyright (c) 2009</p>
 * <p> Company: </p>
 * @author 苏红胜 - mrsuhongsheng@gmail.com
 * @version 1.0
 */
public class FileUploadZip extends AbstractFileUploader {

    private static FileUploadZip instance = null;

    private FileUploadZip() {
    }

    public static FileUploadZip getInstance() {
        if (instance == null) {
            synchronized (FileUploadZip.class) {
                instance = new FileUploadZip();
            }
        }
        return instance;
    }

    @Override
    protected void copy(Reader reader, OutputStream outputs) throws IOException {
        if (outputs == null) {
            throw new NullPointerException();
        }
        if (reader == null) {
            throw new NullPointerException();
        }
        ZipOutputStream zipoutputs = null;
        try {
            zipoutputs = new ZipOutputStream(outputs);
            zipoutputs.putNextEntry(new ZipEntry("default"));
            IOUtils.copy(reader, zipoutputs);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (zipoutputs != null) {
                zipoutputs.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    @Override
    protected void copy(InputStream inputs, OutputStream outputs) throws IOException {
        if (outputs == null) {
            throw new NullPointerException();
        }
        if (inputs == null) {
            throw new NullPointerException();
        }
        ZipOutputStream zipoutputs = null;
        try {
            zipoutputs = new ZipOutputStream(outputs);
            zipoutputs.putNextEntry(new ZipEntry("default"));
            IOUtils.copy(inputs, zipoutputs);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (zipoutputs != null) {
                zipoutputs.close();
            }
            if (inputs != null) {
                inputs.close();
            }
        }
    }
}
