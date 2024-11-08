package org.skins.dao.hibernate.usertype.oracle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import oracle.sql.BLOB;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

public class OracleBLOBType implements UserType {

    public Object assemble(Serializable arg0, Object arg1) throws HibernateException {
        System.out.println(arg0);
        System.out.println(arg1);
        return null;
    }

    public Object deepCopy(Object arg0) throws HibernateException {
        System.out.println("MyBLOB.deepCopy() " + arg0);
        return null;
    }

    public Serializable disassemble(Object arg0) throws HibernateException {
        System.out.println(arg0);
        return null;
    }

    public boolean equals(Object arg0, Object arg1) throws HibernateException {
        return false;
    }

    public int hashCode(Object arg0) throws HibernateException {
        return 0;
    }

    public boolean isMutable() {
        return true;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object arg2) throws HibernateException, SQLException {
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
        return os.toByteArray();
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, sqlTypes()[0]);
            return;
        }
        try {
            Connection conn = st.getConnection().getMetaData().getConnection();
            OutputStream tempBlobOutputStream = null;
            BLOB tempBlob = BLOB.createTemporary(conn, true, BLOB.DURATION_SESSION);
            try {
                tempBlob.open(BLOB.MODE_READWRITE);
                tempBlobOutputStream = tempBlob.getBinaryOutputStream();
                tempBlobOutputStream.write((byte[]) value);
                tempBlobOutputStream.flush();
            } finally {
                if (tempBlobOutputStream != null) tempBlobOutputStream.close();
                tempBlobOutputStream.close();
            }
            st.setBlob(index, (Blob) tempBlob);
        } catch (IOException e) {
            throw new HibernateException(e);
        }
    }

    public Object replace(Object arg0, Object arg1, Object arg2) throws HibernateException {
        System.out.println("MyBLOB.replace()" + arg0);
        System.out.println("MyBLOB.replace()" + arg1);
        System.out.println("MyBLOB.replace()" + arg2);
        return null;
    }

    public Class returnedClass() {
        return byte[].class;
    }

    public int[] sqlTypes() {
        return new int[] { Types.BLOB };
    }
}
