package org.mcisb.subliminal.kegg;

import java.io.*;
import java.net.*;
import javax.xml.rpc.*;
import org.mcisb.util.io.*;

/**
 * 
 * @author Neil Swainston
 */
class KeggDownloader {

    /**
	 * 
	 */
    private final FileUtils fileUtils = new FileUtils();

    /**
	 * 
	 * @throws ServiceException
	 */
    public KeggDownloader() throws ServiceException {
    }

    /**
	 * 
	 * @param targetDirectory
	 * @param pathway
	 * @throws IOException 
	 * @throws SocketException 
	 */
    File downloadKgml(final File targetDirectory, final String pathway) throws SocketException, IOException {
        if (targetDirectory.exists()) {
            if (!targetDirectory.delete()) {
                throw new IOException();
            }
        }
        if (!targetDirectory.mkdir()) {
            throw new IOException();
        }
        final URL pathwayUrl = new URL("http://www.genome.jp/kegg-bin/download?entry=" + pathway + "&format=kgml");
        final File kgmlFile = new File(targetDirectory, pathway + ".xml");
        fileUtils.write(kgmlFile, fileUtils.read(pathwayUrl));
        return kgmlFile;
    }
}
