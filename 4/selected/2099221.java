package com.lonelytaste.narafms.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import org.apache.commons.io.IOUtils;
import com.lonelytaste.narafms.core.AbstractFileUploader;
import com.lonelytaste.narafms.core.FmsFileID;

/**
 * <p> Title: [名称]</p>
 * <p> Description: [描述]</p>
 * <p> Created on May 12, 2009</p>
 * <p> Copyright: Copyright (c) 2009</p>
 * <p> Company: </p>
 * @author 苏红胜 - mrsuhongsheng@gmail.com
 * @version 1.0
 */
public class UploaderSimpleImpl extends AbstractFileUploader {

    private static UploaderSimpleImpl instance = null;

    private UploaderSimpleImpl() {
    }

    public static UploaderSimpleImpl getInstance() {
        if (instance == null) {
            synchronized (UploaderSimpleImpl.class) {
                instance = new UploaderSimpleImpl();
            }
        }
        return instance;
    }

    protected void copy(Reader reader, OutputStream outputs) throws IOException {
        IOUtils.copy(reader, outputs);
    }

    protected void copy(InputStream inputs, OutputStream outputs) throws IOException {
        IOUtils.copy(inputs, outputs);
    }
}
