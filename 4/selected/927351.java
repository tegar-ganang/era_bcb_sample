package org.vd.servlet.method.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vd.store.VirtualFile;

public class GetMethod extends HeadMethod {

    private static final Log LOGGER = LogFactory.getLog(GetMethod.class);

    @Override
    protected void sendResponse(VirtualFile file, InputStream in) throws IOException {
        OutputStream out = response.getOutputStream();
        byte[] temp = new byte[8 * 1024];
        int read = in.read(temp);
        while (read > 0) {
            out.write(temp, 0, read);
            read = in.read(temp);
        }
    }
}
