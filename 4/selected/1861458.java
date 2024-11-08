package org.sourceforge.kga.speciesIo;

import java.io.*;
import org.sourceforge.kga.*;

public class SerializableSpecies {

    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(Species.class.getName());

    /**
     * The file which user will save to. Null if not selected yet.
     */
    private transient File file = null;

    private SpeciesList speciesList = null;

    static SpeciesListFormatV1 v1 = new SpeciesListFormatV1();

    /**
     * Creating a new species list
     */
    public SerializableSpecies() {
        speciesList = new SpeciesList();
    }

    /**
     * Load a species list from the jar file
     * @param is a kga formated resource
     */
    public SerializableSpecies(InputStream is) throws IOException, InvalidFormatException {
        speciesList = loadFrom(is);
    }

    /**
     * Creating a new species list and loads from file
     * @param file a kga formated file
     */
    public SerializableSpecies(File file) throws FileNotFoundException, IOException, InvalidFormatException {
        log.info("Loading species list from " + file.getAbsolutePath());
        speciesList = loadFrom(new FileInputStream(file));
        this.file = file;
    }

    public SpeciesList getSpeciesList() {
        return speciesList;
    }

    public File getFile() {
        return file;
    }

    /**
     * Loading set of squares from InputStream.
     * @param is the InputStream to load from
     * @return errorcode specified in SpeciesList
     */
    private SpeciesList loadFrom(InputStream is) throws IOException, InvalidFormatException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        while (is.available() > 0) {
            int read = is.read(buffer, 0, 65536);
            tmp.write(buffer, 0, read);
        }
        ByteArrayInputStream speciesStream = new ByteArrayInputStream(tmp.toByteArray());
        try {
            return v1.load(speciesStream);
        } catch (InvalidFormatException ex) {
            throw ex;
        }
    }

    /**
     * Tries to save species list to the file which this species was loaded from.
     * If species list was created from scratch this method does nothing and returns false.
     * @return true if successful otherwise false
     */
    public boolean saveToFile() throws FileNotFoundException, IOException {
        if (file == null) return false;
        saveToFile(file);
        return true;
    }

    /**
     * Saving species to file.
     * @param file the file to save the squares
     * @return true if successful
     */
    public void saveToFile(File file) throws FileNotFoundException, IOException {
        log.info("Saving to " + file.getAbsolutePath());
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        v1.save(speciesList, out);
        out.close();
        this.file = file;
    }
}
