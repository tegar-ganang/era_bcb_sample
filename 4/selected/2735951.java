package org.hkupp.db.accessors;

import java.io.*;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

/**
 * This class extends the SpectrumTableAccessor.
 *
 * @author Lennart Martens
 * @version $Id$
 */
public class Spectrum extends SpectrumTableAccessor {

    public Spectrum(HashMap aData) {
        super(aData);
    }

    public void loadSpectrumFile(File aSpectrumFile) throws IOException {
        if (aSpectrumFile == null) {
            throw new RuntimeException("No spectrum file defined!");
        }
        byte[] bytes = null;
        String name = null;
        if (!aSpectrumFile.exists()) {
            throw new IOException("File '" + aSpectrumFile.getAbsolutePath() + "' does not exist!");
        }
        name = aSpectrumFile.getName();
        bytes = this.zipFile(aSpectrumFile);
        super.setFilename(name);
        super.setSpectrumfile(bytes);
    }

    /**
     * This method loads and zips the file data.
     *
     * @param   aFile  File with the data.
     * @return  byte[]  with the GZIPped data.
     * @exception   IOException whenever the GZIPping process fails.
     */
    private byte[] zipFile(File aFile) throws IOException {
        byte[] bytes = null;
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(aFile));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(baos);
        int reading = -1;
        while ((reading = bis.read()) != -1) {
            gos.write(reading);
        }
        gos.finish();
        bis.close();
        baos.flush();
        bytes = baos.toByteArray();
        gos.close();
        baos.close();
        return bytes;
    }
}
