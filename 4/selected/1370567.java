package cn.sduo.app.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import cn.sduo.app.exception.IOPException;
import cn.sduo.app.exception.PCCWBaseException;

/** IOProcessor is a helper class performing IO actions. It eases the use of some standard IO functions.
 */
public class IOPUtil {

    private static final Log LOG = LogFactory.getLog(IOPUtil.class);

    /** Creates a new instance of IOProcessor. */
    private IOPUtil() {
    }

    /** Gets a text from a specified input stream.
	 * @param ins The input stream from which the text is retrieved.
	 * @param enc The encoding used to decode the bytes from the input stream to the text.
	 * @throws IOPException When errors occurred in the IO access.
	 * @return The text retrieved from the specified input stream.
	 */
    public static String getTextFromStream(InputStream ins, String enc) throws IOPException {
        if (ins == null) return null; else {
            InputStreamReader isr = null;
            BufferedReader bis = null;
            try {
                if (enc == null) isr = new InputStreamReader(ins); else isr = new InputStreamReader(ins, enc);
                bis = new BufferedReader(isr);
                String content = "";
                String line = null;
                while ((line = bis.readLine()) != null) content += line + "\n";
                return content;
            } catch (Exception e) {
                throw new IOPException(PCCWBaseException.CODE_RUNTIME, "Unable to get text from the specified stream", e);
            } finally {
                try {
                    if (bis != null) bis.close();
                    if (isr != null) isr.close();
                } catch (IOException ex) {
                    LOG.error("Error in closing buffer reader and input stream reader!!", ex);
                }
            }
        }
    }

    /** Returns an input stream for reading the specified resource.
	 * @param name The resource name.
	 * @throws IOPException If the resource specified is not found or any other errors occurred during the retrieving process.
	 * @return An input stream for reading the resource.
	 */
    public static InputStream getResourceAsStream(String name) throws IOPException {
        try {
            InputStream ins = IOPUtil.class.getResourceAsStream(name);
            if (ins == null) throw new java.io.IOException("Resource not found!"); else return ins;
        } catch (Exception e) {
            throw new IOPException(PCCWBaseException.CODE_RUNTIME, "Unable to get the specified resource '" + name + "'", e);
        }
    }

    public static String getResourceAsString(String name, String enc) throws IOPException {
        try {
            InputStream ins = getResourceAsStream(name);
            return getTextFromStream(ins, enc);
        } catch (IOPException e) {
            throw e;
        } catch (Exception e) {
            throw new IOPException(PCCWBaseException.CODE_RUNTIME, "Unable to getResourceAsString '" + name + "'", e);
        }
    }

    public static String getResourceAsString(String name) throws IOPException {
        return getResourceAsString(name, null);
    }

    public static byte[] getResourceAsBytes(String name) throws IOPException {
        try {
            InputStream ins = getResourceAsStream(name);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int i = 0;
            byte abyte0[] = new byte[1024];
            while ((i = ins.read(abyte0, 0, 1024)) > 0) bos.write(abyte0, 0, i);
            return bos.toByteArray();
        } catch (IOPException e) {
            throw e;
        } catch (Exception e) {
            throw new IOPException(PCCWBaseException.CODE_RUNTIME, "Unable to getResourceAsBytes '" + name + "'", e);
        }
    }

    /** Loads a specified resource into a java.util.Properties.
	 * @param props The Properties into which the specified resource loads.
	 * @param resource The resource name.
	 * @throws IOPException If the specified Properties is null, the resource specified is not found, or any other errors occurred during the IO process.
	 */
    public static void loadProperties(Properties props, String resource) throws IOPException {
        if (props == null) throw new IOPException(PCCWBaseException.CODE_RUNTIME, "Cannot load resource into properties, properties is null.");
        InputStream ins = getResourceAsStream(resource);
        try {
            props.load(ins);
        } catch (Exception e) {
            throw new IOPException(PCCWBaseException.CODE_RUNTIME, "Unable to load the specified resource '" + resource + "'", e);
        }
    }

    /** Loads a specified list of resources into a java.util.Properties.
	 * @param props The Properties into which the specified resources load.
	 * @param resources List of the resources' names.
	 * @throws IOPException If the specified Properties is null, any of the resources specified is not found, or any other errors occurred during the IO process. However, all the resources found will be loaded into the Properties, no matter any resources not found in the specified list of resources.
	 */
    public static void loadProperties(Properties props, String[] resources) throws IOPException {
        if (props == null) throw new IOPException(PCCWBaseException.CODE_RUNTIME, "Cannot load resources into properties, properties is null.");
        if (resources != null) {
            String exception = "";
            for (int i = 0; i < resources.length; i++) {
                try {
                    loadProperties(props, resources[i]);
                } catch (Exception e) {
                    exception += "Resource[" + i + "]: " + e.getMessage() + "; ";
                }
            }
            if (!exception.equals("")) throw new IOPException(PCCWBaseException.CODE_RUNTIME, "Error in loading resources into properties, details: " + exception);
        }
    }

    /** Creates a java.util.Properties and loads the specified resource into it. It simply creates a new instance of Properties and calls the loadProperties() method for loading the resource.
	 * @param resource The resource name.
	 * @throws IOPException When errors occurred during loading the resource.
	 * @return The newly created Properties with the specified resource loaded.
	 */
    public static Properties createProperties(String resource) throws IOPException {
        Properties props = new Properties();
        loadProperties(props, resource);
        return props;
    }

    /** Creates a java.util.Properties and loads the specified resources into it. It simply creates a new instance of Properties and calls the loadProperties() method for loading the resources.
	 * @param resources List of the resources' names.
	 * @throws IOPException When errors occurred during loading the resources.
	 * @return The newly created Properties with the specified resources loaded.
	 */
    public static Properties createProperties(String[] resources) throws IOPException {
        Properties props = new Properties();
        loadProperties(props, resources);
        return props;
    }
}
