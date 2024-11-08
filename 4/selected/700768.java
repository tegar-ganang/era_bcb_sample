package org.tigr.cloe.model.facade.assemblerFacade.assemblerDataConverter;

import org.jcvi.io.IOUtil;
import org.tigr.seq.cloe.Cloe;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * User: aresnick
 * Date: Mar 25, 2010
 * Time: 12:06:09 PM
 * <p/>
 * $HeadURL$
 * $LastChangedRevision$
 * $LastChangedBy$
 * $LastChangedDate$
 * <p/>
 * Description:
 */
public abstract class ConverterUtils {

    private static final String configFile = "org/tigr/cloe/model/facade/assemblerFacade/collectionAssembler.properties";

    public static Properties getProperties() throws Exception {
        InputStream in = null;
        try {
            in = ConverterUtils.class.getClassLoader().getResourceAsStream(configFile);
            if (in == null) {
                throw new Exception("Properties file not found!");
            } else {
                Properties props = new Properties();
                props.load(in);
                return props;
            }
        } catch (Exception e) {
            throw new Exception("Unable to load properties file " + configFile, e);
        } finally {
            IOUtil.closeAndIgnoreErrors(in);
        }
    }

    public static void copyXMLToOutputFile(byte[] assemblyRequestXML, File outputFile) throws Exception {
        FileOutputStream xmlInputStream = null;
        try {
            xmlInputStream = new FileOutputStream(outputFile);
            xmlInputStream.write(assemblyRequestXML);
        } finally {
            IOUtil.closeAndIgnoreErrors(xmlInputStream);
        }
    }

    public static void moveOutputAsmFile(File inputLocation, File outputLocation) throws Exception {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(inputLocation);
            outputStream = new FileOutputStream(outputLocation);
            byte buffer[] = new byte[1024];
            while (inputStream.available() > 0) {
                int read = inputStream.read(buffer);
                outputStream.write(buffer, 0, read);
            }
            inputLocation.delete();
        } finally {
            IOUtil.closeAndIgnoreErrors(inputStream);
            IOUtil.closeAndIgnoreErrors(outputStream);
        }
    }
}
