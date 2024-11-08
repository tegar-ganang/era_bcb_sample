package cs;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Properties;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.Utils;

/**
* This class maintains a persistent store of the files that have
* checked ok and their associated timestamp. It uses a property file
* for storage.  A hashcode of the Configuration is stored in the
* cache file to ensure the cache is invalidated when the
* configuration has changed.
*
* @author Oliver Burn
*/
final class PropertyCacheFile {

    /**
  * The property key to use for storing the hashcode of the
  * configuration. To avoid nameclashes with the files that are
  * checked the key is chosen in such a way that it cannot be a
  * valid file name.
  */
    private static final String CONFIG_HASH_KEY = "configuration*?";

    /** name of file to store details **/
    private final String mDetailsFile;

    /** the details on files **/
    private final Properties mDetails = new Properties();

    /**
  * Creates a new <code>PropertyCacheFile</code> instance.
  *
  * @param aCurrentConfig the current configuration, not null
  * @param aFileName the cache file
  */
    PropertyCacheFile(Configuration aCurrentConfig, String aFileName) {
        boolean setInActive = true;
        if (aFileName != null) {
            FileInputStream inStream = null;
            final String currentConfigHash = getConfigHashCode(aCurrentConfig);
            try {
                inStream = new FileInputStream(aFileName);
                mDetails.load(inStream);
                final String cachedConfigHash = mDetails.getProperty(CONFIG_HASH_KEY);
                setInActive = false;
                if ((cachedConfigHash == null) || !cachedConfigHash.equals(currentConfigHash)) {
                    mDetails.clear();
                    mDetails.put(CONFIG_HASH_KEY, currentConfigHash);
                }
            } catch (final FileNotFoundException e) {
                setInActive = false;
                mDetails.put(CONFIG_HASH_KEY, currentConfigHash);
            } catch (final IOException e) {
                Utils.getExceptionLogger().debug("Unable to open cache file, ignoring.", e);
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (final IOException ex) {
                        Utils.getExceptionLogger().debug("Unable to close cache file.", ex);
                    }
                }
            }
        }
        mDetailsFile = (setInActive) ? null : aFileName;
    }

    /** Cleans up the object and updates the cache file. **/
    void destroy() {
        if (mDetailsFile != null) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(mDetailsFile);
                mDetails.store(out, null);
            } catch (final IOException e) {
                Utils.getExceptionLogger().debug("Unable to save cache file.", e);
            } finally {
                if (out != null) {
                    try {
                        out.flush();
                        out.close();
                    } catch (final IOException ex) {
                        Utils.getExceptionLogger().debug("Unable to close cache file.", ex);
                    }
                }
            }
        }
    }

    /**
  * @return whether the specified file has already been checked ok
  * @param aFileName the file to check
  * @param aTimestamp the timestamp of the file to check
  */
    boolean alreadyChecked(String aFileName, long aTimestamp) {
        final String lastChecked = mDetails.getProperty(aFileName);
        return (lastChecked != null) && (lastChecked.equals(Long.toString(aTimestamp)));
    }

    /**
  * Records that a file checked ok.
  * @param aFileName name of the file that checked ok
  * @param aTimestamp the timestamp of the file
  */
    void checkedOk(String aFileName, long aTimestamp) {
        mDetails.put(aFileName, Long.toString(aTimestamp));
    }

    /**
  * Calculates the hashcode for a GlobalProperties.
  *
  * @param aConfiguration the GlobalProperties
  * @return the hashcode for <code>aConfiguration</code>
  */
    private String getConfigHashCode(Serializable aConfiguration) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(aConfiguration);
            oos.flush();
            oos.close();
            final MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(baos.toByteArray());
            return hexEncode(md.digest());
        } catch (final Exception ex) {
            Utils.getExceptionLogger().debug("Unable to calculate hashcode.", ex);
            return "ALWAYS FRESH: " + System.currentTimeMillis();
        }
    }

    /** hex digits */
    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /** mask for last byte */
    private static final int MASK_0X0F = 0x0F;

    /** bit shift */
    private static final int SHIFT_4 = 4;

    /**
  * Hex-encodes a byte array.
  * @param aByteArray the byte array
  * @return hex encoding of <code>aByteArray</code>
  */
    private static String hexEncode(byte[] aByteArray) {
        final StringBuffer buf = new StringBuffer(2 * aByteArray.length);
        for (int i = 0; i < aByteArray.length; i++) {
            final int b = aByteArray[i];
            final int low = b & MASK_0X0F;
            final int high = (b >> SHIFT_4) & MASK_0X0F;
            buf.append(HEX_CHARS[high]);
            buf.append(HEX_CHARS[low]);
        }
        return buf.toString();
    }
}
