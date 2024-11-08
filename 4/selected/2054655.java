package org.mandarax.zkb;

import java.util.Properties;
import java.io.*;

/**
 * Abstract superclass for IOManager implementations.
 * @author <A href="http://www-ist.massey.ac.nz/JBDietrich" target="_top">Jens Dietrich</A>
 * @version 3.4 <7 March 05>
 * @since 3.4
 */
public abstract class AbstractIOManager implements IOManager {

    /**
	 * Read data into a byte buffer.
	 * @param in an input stream
	 * @return an array of bytes
	 */
    protected byte[] readData(InputStream in) throws IOException {
        int c = -1;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        while ((c = in.read()) != -1) bout.write(c);
        return bout.toByteArray();
    }

    /**
	 * Log and (re-)throw an exception.
	 * @param msg the exception message
	 * @param x the cause
	 */
    protected void error(String msg, Exception x) throws ZKBException {
        LOG_ZKB.error(msg);
        throw new ZKBException(msg, x);
    }

    /**
	 * Log and (re-)throw an exception.
	 * @param msg the exception message
	 */
    protected void error(String msg) throws ZKBException {
        error(msg, null);
    }

    /**
	 * Get the ops.
	 * @param metaData meta data
	 * @return teh OPS used
	 */
    protected ObjectPersistencyService getOPS(Properties metaData) throws ZKBException {
        String opsClassName = metaData.getProperty(ZKBManager.OPS);
        ObjectPersistencyService ops = null;
        try {
            LOG_ZKB.debug("Using OPS : " + opsClassName);
            ops = (ObjectPersistencyService) Class.forName(opsClassName).newInstance();
            return ops;
        } catch (ClassNotFoundException x) {
            error("Cannot find class " + opsClassName, x);
        } catch (InstantiationException x) {
            error("Cannot instanciate " + opsClassName, x);
        } catch (IllegalAccessException x) {
            error("Cannot access class " + opsClassName, x);
        }
        return null;
    }
}
