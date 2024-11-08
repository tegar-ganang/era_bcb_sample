package com.reserveamerica.jirarmi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import com.reserveamerica.jirarmi.beans.EntityRemote;
import com.reserveamerica.jirarmi.exceptions.JiraException;

/**
 * Miscellaneous utilities.
 * 
 * @author BStasyszyn
 */
public class Utils {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Utils.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    /**
   * Extracts the IDs from the given collection of entities.
   * 
   * @param entities
   * @return The IDs or an empty set.
   */
    public static Set<Long> toIds(Collection<? extends EntityRemote> entities) {
        if (entities == null) {
            return Collections.emptySet();
        }
        Set<Long> ids = new LinkedHashSet<Long>(entities.size());
        for (EntityRemote entity : entities) {
            ids.add(entity.getId());
        }
        return ids;
    }

    public static <E> Set<E> toSet(E[] values) {
        Set<E> set = new LinkedHashSet<E>(values.length);
        for (E value : values) {
            set.add(value);
        }
        return set;
    }

    /**
   * Extracts the keys from the given collection of entities.
   * 
   * @param entities
   * @return The keys or an empty set.
   */
    public static Set<String> toKeys(Collection<? extends EntityRemote> entities) {
        if (entities == null) {
            return Collections.emptySet();
        }
        Set<String> keys = new LinkedHashSet<String>(entities.size());
        for (EntityRemote entity : entities) {
            keys.add(entity.getKey());
        }
        return keys;
    }

    /**
   * Retrieves the byte contents of the given file.
   * 
   * @param file
   * @return The byte contents of the file.
   * @throws JiraException
   */
    public static byte[] getContents(File file) throws JiraException {
        ByteArrayOutputStream os = null;
        InputStream is = null;
        try {
            os = new ByteArrayOutputStream();
            is = new FileInputStream(file);
            byte[] b = new byte[4096];
            while (true) {
                int read = is.read(b);
                if (read <= 0) {
                    break;
                }
                os.write(b, 0, read);
            }
            return os.toByteArray();
        } catch (IOException ex) {
            log.error("getContents - ", ex);
            throw new JiraException("Unable to get contents of file [" + file.getAbsolutePath() + "].");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    log.warn("getContents - Unable to close input stream.", ex);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    log.warn("getContents - Unable to close output stream.", ex);
                }
            }
        }
    }

    /**
   * Returns the given data as a {@link Timestamp} object.
   * 
   * @param date
   * @return The timestamp or <tt>null</tt> if <tt>date</tt> is <tt>null</tt>.
   */
    public static Timestamp getTimestamp(Date date) {
        return getTimestamp(date, false);
    }

    /**
   * Returns the given data as a {@link Timestamp} object.
   * 
   * @param date
   * @param useCurrentTimeIfNull - Uses current time if date is null.
   * @return The timestamp.
   */
    public static Timestamp getTimestamp(Date date, boolean useCurrentTimeIfNull) {
        if (date == null) {
            return useCurrentTimeIfNull ? new Timestamp(System.currentTimeMillis()) : null;
        }
        if (date instanceof Timestamp) {
            return (Timestamp) date;
        }
        return new Timestamp(date.getTime());
    }

    /**
   * Returns the string representation of the given date, in a format
   * that is supported by Jira's advanced searching.
   * 
   * @param date The date
   * @return The string representation of the date.
   */
    public static String formatDate(Date date) {
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }
}
