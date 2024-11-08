package edu.upmc.opi.caBIG.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import oracle.sql.BLOB;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import com.mchange.v2.c3p0.dbms.OracleUtils;
import edu.upmc.opi.caBIG.common.CaBIG_LobUtilities;

/**
 * The Class OracleStringifiedBlobType.
 *
 * @author mitchellkj@upmc.edu
 * @version $Id: OracleCompressedBase64TextType.java,v 1.2 2010/05/25 15:32:00 mitchellkj01 Exp $
 * @since 1.4.2_04 
 */
public class OracleCompressedBase64TextType implements UserType {

    /**
     * The log.
     */
    private Logger log = Logger.getLogger(getClass());

    /**
     * Return the SQL type codes for the columns mapped by this type.
     * 
     * @return the int[]
     */
    public int[] sqlTypes() {
        return new int[] { Types.CLOB };
    }

    /**
     * The class returned by <tt>nullSafeGet()</tt>.
     * 
     * @return the class
     */
    public Class returnedClass() {
        return String.class;
    }

    /**
     * Equals.
     * 
     * @param y the y
     * @param x the x
     * 
     * @return true, if equals
     */
    public boolean equals(Object x, Object y) {
        return ObjectUtils.equals(x, y);
    }

    /**
     * Retrieve an instance of the mapped class from a JDBC resultset.
     * Implementors should handle possibility of null values.
     * 
     * @param owner the owner
     * @param names the names
     * @param rs the rs
     * 
     * @return the object
     * 
     * @throws SQLException the SQL exception
     * @throws HibernateException the hibernate exception
     */
    public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
        InputStream blobReader = rs.getBinaryStream(names[0]);
        if (blobReader == null) return null;
        byte[] b = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            while ((blobReader.read(b)) != -1) os.write(b);
        } catch (IOException e) {
            throw new SQLException(e.toString());
        } finally {
            try {
                os.close();
            } catch (IOException e) {
            }
        }
        return new String((String) CaBIG_LobUtilities.decodeLob(os.toByteArray()));
    }

    /**
     * Write an instance of the mapped class to a prepared statement.
     * Implementors should handle possibility of null values. A multi-column
     * type should be written to parameters starting from <tt>index</tt>.
     * 
     * @param index the index
     * @param value the value
     * @param st the st
     * 
     * @throws SQLException the SQL exception
     * @throws HibernateException the hibernate exception
     */
    public void nullSafeSet(PreparedStatement ps, Object value, int index) throws HibernateException, SQLException {
        if (value == null) {
            ps.setNull(index, sqlTypes()[0]);
            return;
        } else {
            String payLoad = CaBIG_LobUtilities.encodeLob((String) value);
            ps.setString(index, payLoad);
        }
    }

    /**
     * Return a deep copy of the persistent state, stopping at entities and at
     * collections.
     * 
     * @param value the value
     * 
     * @return the object
     */
    public Object deepCopy(Object value) {
        String result = null;
        if (value != null && value instanceof java.lang.String) {
            result = new String((String) value);
        }
        return result;
    }

    /**
     * Are objects of this type mutable?.
     * 
     * @return true, if is mutable
     */
    public boolean isMutable() {
        return false;
    }

    /**
     * Hash code.
     * 
     * @param arg0 the arg0
     * 
     * @return the int
     * 
     * @throws HibernateException the hibernate exception
     */
    public int hashCode(Object arg0) throws HibernateException {
        return 0;
    }

    /**
     * Disassemble.
     * 
     * @param arg0 the arg0
     * 
     * @return the serializable
     * 
     * @throws HibernateException the hibernate exception
     */
    public Serializable disassemble(Object arg0) throws HibernateException {
        return null;
    }

    /**
     * Assemble.
     * 
     * @param arg1 the arg1
     * @param arg0 the arg0
     * 
     * @return the object
     * 
     * @throws HibernateException the hibernate exception
     */
    public Object assemble(Serializable arg0, Object arg1) throws HibernateException {
        return null;
    }

    /**
     * Replace.
     * 
     * @param arg1 the arg1
     * @param arg0 the arg0
     * @param arg2 the arg2
     * 
     * @return the object
     * 
     * @throws HibernateException the hibernate exception
     */
    public Object replace(Object arg0, Object arg1, Object arg2) throws HibernateException {
        return null;
    }
}
