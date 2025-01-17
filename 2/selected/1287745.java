package org.bdgp.io;

import java.io.*;
import java.net.*;

public class ProgressableURLInputStream extends ProgressableInputStream {

    public ProgressableURLInputStream(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        stream = connection.getInputStream();
        fileSize = (long) connection.getContentLength();
    }
}
