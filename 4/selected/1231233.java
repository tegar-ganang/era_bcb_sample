package rubbish.db.core.conv;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import rubbish.db.exception.IORuntimeException;
import rubbish.db.sql_null.SQL_NULL;
import rubbish.db.util.IOUtils;

/**
 * �W���̃t�B���^�[
 * 
 * @author $Author: winebarrel $
 * @version $Revision: 1.1 $
 */
public class DefaultFilter implements Filter {

    public void setParam(int i, Object param, PreparedStatement stmt) throws SQLException {
        if (param == null || param instanceof SQL_NULL) {
            setNull(i, param, stmt);
        } else {
            if (param instanceof Date) setDate(i, (Date) param, stmt); else if (param instanceof InputStream) setInputStream(i, (InputStream) param, stmt); else if (param instanceof Reader) setReader(i, (Reader) param, stmt); else stmt.setObject(i, param);
        }
    }

    protected void setNull(int i, Object param, PreparedStatement stmt) throws SQLException {
        if (param != null && param instanceof SQL_NULL) stmt.setNull(i, ((SQL_NULL) param).getTypes()); else stmt.setNull(i, Types.VARCHAR);
    }

    protected void setDate(int i, Date date, PreparedStatement stmt) throws SQLException {
        stmt.setObject(i, new Timestamp(date.getTime()));
    }

    protected void setInputStream(int i, InputStream in, PreparedStatement stmt) throws SQLException {
        try {
            stmt.setBinaryStream(i, in, in.available());
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    protected void setReader(int i, Reader reader, PreparedStatement stmt) throws SQLException {
        StringWriter writer = new StringWriter();
        IOUtils.pipe(reader, writer);
        String str = writer.toString();
        stmt.setCharacterStream(i, new StringReader(str), str.length());
    }

    public ResultSet wrapResultSet(ResultSet rs) {
        return new DefaultResultSetWrapper(rs);
    }
}
