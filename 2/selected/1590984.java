package org.bbop.io;

import java.io.*;
import java.net.*;
import org.apache.log4j.*;

public class ProgressableURLInputStream extends ProgressableInputStream {

    protected static final Logger logger = Logger.getLogger(ProgressableURLInputStream.class);

    public ProgressableURLInputStream(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        stream = connection.getInputStream();
        fileSize = connection.getContentLength();
    }
}
