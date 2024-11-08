package com.lonelytaste.narafms.loader.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import org.apache.commons.io.IOUtils;
import com.lonelytaste.narafms.core.AbstractFileUploader;

/**
 * <p> Title: [加密上传]</p>
 * <p> Description: [描述]</p>
 * <p> Created on May 11, 2009</p>
 * <p> Copyright: Copyright (c) 2009</p>
 * <p> Company: </p>
 * @author 苏红胜 - mrsuhongsheng@gmail.com
 * @version 1.0
 */
public class FileUploadEncrypter extends AbstractFileUploader {

    private static FileUploadEncrypter instance = null;

    private FileUploadEncrypter() {
    }

    public static FileUploadEncrypter getInstance() {
        if (instance == null) {
            synchronized (FileUploadEncrypter.class) {
                instance = new FileUploadEncrypter();
            }
        }
        return instance;
    }

    @Override
    protected void copy(Reader reader, OutputStream outputs) throws IOException {
        IOUtils.copy(reader, outputs);
    }

    @Override
    protected void copy(InputStream inputs, OutputStream outputs) throws IOException {
        IOUtils.copy(inputs, outputs);
    }
}
