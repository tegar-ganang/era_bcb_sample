package com.ashs.jump.plugin;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.datasource.*;
import com.vividsolutions.jump.io.*;

public class CGDEFWriter implements JUMPWriter {

    private CGDWriter cgdWriter = new CGDWriter();

    /**constuctor*/
    public CGDEFWriter() {
    }

    /**
     * The first JUMP Readers took responsibility for handling .zip and
     * .gz files (a more modular design choice would have been to handle
     * compression outside of the Readers); this class uses a
     * DelegatingCompressedFileHandler to ensure that these JUMP Readers
     * receive the properties they need to do decompression.
     */
    private static class ClassicReaderWriterFileDataSource extends StandardReaderWriterFileDataSource {

        public ClassicReaderWriterFileDataSource(JUMPReader reader, JUMPWriter writer, String[] extensions) {
            super(new DelegatingCompressedFileHandler(reader, toEndings(extensions)), writer, extensions);
            this.extensions = extensions;
        }
    }

    public static class CGDEF extends ClassicReaderWriterFileDataSource {

        public CGDEF() {
            super(new CGDEFReader(), new CGDEFWriter(), new String[] { "cgd", "cgdef", "txt" });
        }
    }

    /**
     * Main method - writes a list of CGDEF features (no attributes).
     *
     * @param featureCollection features to write
     * @param dp 'OutputFile' or 'DefaultValue' to specify the output file.
     */
    public void write(FeatureCollection featureCollection, DriverProperties dp) throws IllegalParametersException, Exception {
        String outputFname;
        outputFname = dp.getProperty("File");
        if (outputFname == null) {
            outputFname = dp.getProperty("DefaultValue");
        }
        if (outputFname == null) {
            throw new IllegalParametersException("call to CGDEFWriter.write() has DataProperties w/o an OutputFile specified");
        }
        java.io.Writer w;
        w = new java.io.FileWriter(outputFname);
        this.write(featureCollection, w);
        w.close();
    }

    /**
     * Function that actually does the writing
     *@param featureCollection features to write
     *@param writer where to write
     */
    public void write(FeatureCollection featureCollection, Writer writer) throws IOException {
        FeatureSchema schema = featureCollection.getFeatureSchema();
        for (Iterator i = featureCollection.iterator(); i.hasNext(); ) {
            Feature feature = (Feature) i.next();
            String id = "";
            if (schema.hasAttribute(LoadASHSExtension.MAP_ID_FIELDNAME)) id = feature.getString(LoadASHSExtension.MAP_ID_FIELDNAME).trim();
            cgdWriter.writeFormatted(feature.getGeometry(), id, writer);
        }
    }
}
