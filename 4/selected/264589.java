package uk.org.ogsadai.converters.resultset.webrowset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.xerces.impl.dv.util.Base64;
import uk.org.ogsadai.converters.resultset.ColumnStrategy;

/**
 * Strategy to retrieve a binary column value from the database.
 * 
 * @author The OGSA-DAI Team.
 */
public class BinaryColumnStrategy implements ColumnStrategy {

    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh,  2002 - 2007.";

    public void convertField(StringBuffer output, ResultSet rs, int column) throws SQLException, IOException {
        InputStream stream = rs.getBinaryStream(column);
        if (stream != null) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[2048];
            int read = 0;
            while ((read = stream.read(bytes)) >= 0) {
                buffer.write(bytes, 0, read);
            }
            byte[] value = buffer.toByteArray();
            if ((value != null) && (value.length > 0)) {
                output.append(Base64.encode(value));
            }
        } else {
            output.append("<null/>");
        }
    }
}
