package org.sourceforge.kga.gardenIo;

import java.io.*;
import org.sourceforge.kga.*;

public class SerializableGarden {

    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(Garden.class.getName());

    /**
     * The file which user will save to. Null if not selected yet.
     */
    private transient File file = null;

    private Garden garden = null;

    static GardenFormatV1 v1 = new GardenFormatV1();

    static GardenFormatXmlV1 xmlV1 = new GardenFormatXmlV1();

    /**
     * Creating a new garden
     */
    public SerializableGarden() {
        garden = new Garden();
    }

    /**
     * Load a garden from the jar file
     * @param is a kga formated resource
     */
    public SerializableGarden(InputStream is) throws IOException, InvalidFormatException {
        garden = loadFrom(is);
    }

    /**
     * Creating a new garden and loads squares from file
     * @param file a kga formated file
     */
    public SerializableGarden(File file) throws FileNotFoundException, IOException, InvalidFormatException {
        log.info("Loading garden from " + file.getAbsolutePath());
        garden = loadFrom(new FileInputStream(file));
        this.file = file;
    }

    public Garden getGarden() {
        return garden;
    }

    public File getFile() {
        return file;
    }

    /**
     * Loading set of squares from InputStream.
     * @param is the InputStream to load from
     * @return errorcode specified in Garden
     */
    private Garden loadFrom(InputStream is) throws IOException, InvalidFormatException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        while (is.available() > 0) {
            int read = is.read(buffer, 0, 65536);
            tmp.write(buffer, 0, read);
        }
        ByteArrayInputStream gardenStream = new ByteArrayInputStream(tmp.toByteArray());
        try {
            return v1.load(gardenStream);
        } catch (InvalidFormatException ex) {
        }
        gardenStream.reset();
        return xmlV1.load(gardenStream);
    }

    /**
     * Tries to save garden to the file which this garden was loaded from.
     * If garden was created from scratch this method does nothing and returns false.
     * @return true if successful otherwise false
     */
    public boolean saveToFile() throws FileNotFoundException, IOException {
        if (file == null) return false;
        saveToFile(file);
        return true;
    }

    /**
     * Saving garden to file.
     * @param file the file to save the squares
     * @return true if successful
     */
    public void saveToFile(File file) throws FileNotFoundException, IOException {
        log.info("Saving to " + file.getAbsolutePath());
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        xmlV1.save(garden, out);
        out.close();
        this.file = file;
    }
}
