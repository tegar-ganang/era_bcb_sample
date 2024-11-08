package org.ccnx.ccn.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.logging.Level;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.CCNFileOutputStream;
import org.ccnx.ccn.io.CCNOutputStream;
import org.ccnx.ccn.io.RepositoryFileOutputStream;
import org.ccnx.ccn.io.RepositoryOutputStream;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;

public abstract class CommonOutput {

    protected CCNTime doPut(CCNHandle handle, String fileName, ContentName nodeName) throws IOException, InvalidKeyException, ConfigurationException {
        InputStream is;
        if (CommonParameters.verbose) System.out.printf("filename %s\n", fileName);
        if (fileName.startsWith("http://")) {
            if (CommonParameters.verbose) System.out.printf("filename is http\n");
            is = new URL(fileName).openStream();
        } else {
            if (CommonParameters.verbose) System.out.printf("filename is file\n");
            File theFile = new File(fileName);
            if (!theFile.exists()) {
                System.out.println("No such file: " + theFile.getName());
                usage();
            }
            is = new FileInputStream(theFile);
        }
        if (!CommonParameters.rawMode) {
            handle.keyManager().publishKeyToRepository();
        }
        CCNOutputStream ostream;
        if (CommonParameters.rawMode) {
            if (CommonParameters.unversioned) ostream = new CCNOutputStream(nodeName, handle); else ostream = new CCNFileOutputStream(nodeName, handle);
        } else {
            if (CommonParameters.unversioned) ostream = new RepositoryOutputStream(nodeName, handle, CommonParameters.local); else ostream = new RepositoryFileOutputStream(nodeName, handle, CommonParameters.local);
        }
        if (CommonParameters.timeout != null) ostream.setTimeout(CommonParameters.timeout);
        do_write(ostream, is);
        return ostream.getVersion();
    }

    private void do_write(CCNOutputStream ostream, InputStream is) throws IOException {
        long time = System.currentTimeMillis();
        int size = CommonParameters.BLOCK_SIZE;
        int readLen = 0;
        byte[] buffer = new byte[CommonParameters.BLOCK_SIZE];
        if (Log.isLoggable(Level.FINER)) {
            Log.finer("do_write: " + is.available() + " bytes left.");
            while ((readLen = is.read(buffer, 0, size)) != -1) {
                ostream.write(buffer, 0, readLen);
                Log.finer("do_write: wrote " + size + " bytes.");
                Log.finer("do_write: " + is.available() + " bytes left.");
            }
        } else {
            while ((readLen = is.read(buffer, 0, size)) != -1) {
                ostream.write(buffer, 0, readLen);
            }
        }
        ostream.close();
        Log.fine("finished write: {0}", System.currentTimeMillis() - time);
    }

    protected abstract void usage();
}
