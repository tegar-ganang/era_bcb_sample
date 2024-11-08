package org.dbe.composer.wfengine.bpel.server.engine.storage.sql;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import org.dbe.composer.wfengine.SdlException;
import org.dbe.composer.wfengine.util.SdlCloser;
import org.dbe.composer.wfengine.util.SdlUnsynchronizedCharArrayWriter;
import org.dbe.composer.wfengine.xml.XMLParserBase;
import org.w3c.dom.Document;

/**
 *  Common db utility methods.
 */
public class SdlDbUtils {

    /**
     * Constant for true int (1).
     */
    public static final int TRUE = 1;

    /**
     * Constant for true int (0).
     */
    public static final int FALSE = 0;

    /**
     * Conevert int to boolean (1 == true, everything else is false).
     * @param aValue
     */
    public static boolean convertIntToBoolean(int aValue) {
        return aValue == TRUE;
    }

    /**
     * Convert boolean to TRUE constant.
     * @param aValue
     */
    public static int convertBooleanToInt(boolean aValue) {
        return aValue ? TRUE : FALSE;
    }

    /**
     * Returns a <code>Document</code> loaded from the specified <code>Clob</code>.
     *
     * @param aClob
     * @return Document
     * @throws SQLException
     */
    public static Document getDocument(Clob aClob) throws SQLException {
        Reader in = aClob.getCharacterStream();
        try {
            return getSdlXMLParser().loadWsdlDocument(in, null);
        } catch (SdlException e) {
            throw new SQLException("Failed to load document:" + e.getLocalizedMessage());
        } finally {
            SdlCloser.close(in);
        }
    }

    /**
     * Returns parser to use to load document.
     */
    protected static XMLParserBase getSdlXMLParser() {
        XMLParserBase parser = new XMLParserBase();
        parser.setValidating(false);
        parser.setNamespaceAware(true);
        return parser;
    }

    /**
     * Extract the string data from the given clob.
     * @param aClob
     * @throws SQLException
     */
    public static String getString(Clob aClob) throws SQLException {
        Reader reader = aClob.getCharacterStream();
        SdlUnsynchronizedCharArrayWriter writer = new SdlUnsynchronizedCharArrayWriter();
        try {
            char[] buff = new char[1024 * 128];
            int read;
            while ((read = reader.read(buff)) != -1) {
                writer.write(buff, 0, read);
            }
            return writer.toString();
        } catch (IOException io) {
            SdlException.logError(io, "Error reading from clob");
            throw new SQLException("Error reading from clob" + ":" + io.getLocalizedMessage());
        } finally {
            SdlCloser.close(reader);
        }
    }

    /**
     * Returns the specified column of the result set as a <code>Date</code>.
     *
     * @param aResultSet
     * @param aColumnName
     * @throws SQLException
     */
    public static Date getDate(ResultSet aResultSet, String aColumnName) throws SQLException {
        Timestamp timestamp = aResultSet.getTimestamp(aColumnName);
        if (timestamp == null || aResultSet.wasNull()) {
            return null;
        } else {
            return new Date(timestamp.getTime());
        }
    }

    /**
     * Returns the specified column of the result set as a <code>Date</code>.  The column is of
     * type BIGINT, representing the # of millis since the epoch.
     *
     * @param aResultSet
     * @param aColumnName
     * @throws SQLException
     */
    public static Date getDateFromMillis(ResultSet aResultSet, String aColumnName) throws SQLException {
        long millis = aResultSet.getLong(aColumnName);
        if (aResultSet.wasNull() || millis < 0) {
            return null;
        }
        return new Date(millis);
    }
}
