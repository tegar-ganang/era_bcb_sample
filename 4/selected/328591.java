package org.monet.docservice.core.library.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.monet.docservice.core.library.LibraryFile;
import org.monet.docservice.core.library.LibraryZip;
import org.monet.docservice.core.log.Logger;
import org.monet.docservice.core.util.StreamHelper;
import com.google.inject.Inject;

public class LibraryZipImpl implements LibraryZip {

    private static final Integer BUFFER_SIZE = 16384;

    private LibraryFile oLibraryFile;

    private Logger logger;

    @Inject
    public void injectLibraryFile(LibraryFile oLibraryFile) {
        this.oLibraryFile = oLibraryFile;
    }

    @Inject
    public void injectLogger(Logger logger) {
        this.logger = logger;
    }

    public Boolean compress(String sSourceDir, ArrayList<String> aFiles, String sDestinationFilename) {
        logger.debug("compress(%s, %s, %s)", sSourceDir, aFiles, sDestinationFilename);
        BufferedInputStream oOrigin = null;
        FileOutputStream oDestination;
        ZipOutputStream oOutput = null;
        Iterator<String> oIterator;
        byte[] aData;
        try {
            oDestination = new FileOutputStream(sDestinationFilename);
            oOutput = new ZipOutputStream(new BufferedOutputStream(oDestination));
            aData = new byte[BUFFER_SIZE];
            oIterator = aFiles.iterator();
            while (oIterator.hasNext()) {
                try {
                    String sFilename = (String) oIterator.next();
                    FileInputStream fisInput = new FileInputStream(sSourceDir + File.separator + sFilename);
                    oOrigin = new BufferedInputStream(fisInput, BUFFER_SIZE);
                    ZipEntry oEntry = new ZipEntry(sFilename.replace('\\', '/'));
                    oOutput.putNextEntry(oEntry);
                    int iCount;
                    while ((iCount = oOrigin.read(aData, 0, BUFFER_SIZE)) != -1) oOutput.write(aData, 0, iCount);
                } finally {
                    StreamHelper.close(oOrigin);
                }
            }
        } catch (Exception oException) {
            logger.error(oException.getMessage(), oException);
            return false;
        } finally {
            StreamHelper.close(oOutput);
        }
        return true;
    }

    public Boolean decompress(String sFilename, String sDestination) {
        logger.debug("decompress(%s, %s)", sFilename, sDestination);
        BufferedOutputStream oDestination;
        FileInputStream oOrigin;
        ZipInputStream oInput = null;
        try {
            oDestination = null;
            oOrigin = new FileInputStream(new File(sFilename));
            oInput = new ZipInputStream(new BufferedInputStream(oOrigin));
            int iCount;
            byte aData[] = new byte[BUFFER_SIZE];
            ZipEntry oEntry;
            while ((oEntry = oInput.getNextEntry()) != null) {
                if (oEntry.isDirectory()) new File(sDestination + File.separator + oEntry.getName()).mkdirs(); else {
                    try {
                        String sDestDN = oLibraryFile.getDirname(sDestination + File.separator + oEntry.getName());
                        String sDestFN = sDestination + File.separator + oEntry.getName();
                        new File(sDestDN).mkdirs();
                        FileOutputStream oOutput = new FileOutputStream(sDestFN);
                        oDestination = new BufferedOutputStream(oOutput, BUFFER_SIZE);
                        while ((iCount = oInput.read(aData, 0, BUFFER_SIZE)) != -1) {
                            oDestination.write(aData, 0, iCount);
                        }
                        oDestination.flush();
                    } finally {
                        StreamHelper.close(oDestination);
                    }
                }
            }
        } catch (Exception oException) {
            logger.error(oException.getMessage(), oException);
            return false;
        } finally {
            StreamHelper.close(oInput);
        }
        return true;
    }
}
