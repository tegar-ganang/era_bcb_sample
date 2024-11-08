package nl.langits.util.remote.spi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import javax.naming.CompoundName;
import nl.langits.util.remote.IRemoteRepositoryHandler;

/**
 * A repository handler for the file protocol.
 * <br>
 * Copyright (c) 2004 by Niels Lang <br>
 * License of use: <a href="http://www.gnu.org/copyleft/lesser.html">Lesser
 * General Public License (LGPL) </a>, no warranty <br>
 * 
 * @author Niels Lang, mailto:nlang@gmx.net .
 */
public class FILERepositoryHandler implements IRemoteRepositoryHandler {

    private final File REMOTE_BASE_DIR;

    public FILERepositoryHandler(URL remoteRepositoryURL) {
        if (!remoteRepositoryURL.getProtocol().equals("file")) throw new IllegalArgumentException("No file url provided: " + remoteRepositoryURL);
        REMOTE_BASE_DIR = new File(remoteRepositoryURL.getPath());
        if (!REMOTE_BASE_DIR.isDirectory() || !REMOTE_BASE_DIR.exists()) throw new IllegalArgumentException("Illegal or non-existent dir provided: " + remoteRepositoryURL);
    }

    /**
     * @see nl.langits.util.remote.IRemoteRepositoryHandler#putFile(javax.naming.CompoundName, java.io.FileInputStream)
     */
    public void putFile(CompoundName file, FileInputStream fileInput) throws IOException {
        File fullDir = new File(REMOTE_BASE_DIR.getCanonicalPath());
        for (int i = 0; i < file.size() - 1; i++) fullDir = new File(fullDir, file.get(i));
        fullDir.mkdirs();
        File outputFile = new File(fullDir, file.get(file.size() - 1));
        FileOutputStream outStream = new FileOutputStream(outputFile);
        for (int byteIn = fileInput.read(); byteIn != -1; byteIn = fileInput.read()) outStream.write(byteIn);
        fileInput.close();
        outStream.close();
    }
}
