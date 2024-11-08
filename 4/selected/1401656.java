package uk.org.ogsadai.converters.tuple.webrowset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import org.apache.xerces.impl.dv.util.Base64;
import uk.org.ogsadai.converters.tuple.ColumnStrategy;
import uk.org.ogsadai.tuple.ColumnNotFoundException;
import uk.org.ogsadai.tuple.Tuple;
import uk.org.ogsadai.tuple.TupleMetadata;

/**
 * Strategy to retrieve a binary column value from a tuple.
 * 
 * @author The OGSA-DAI Project Team.
 */
public class BinaryColumnStrategy implements ColumnStrategy {

    /** Copyright statement. */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2002-2008.";

    /**
     * {@inheritDoc}
     */
    public void convertField(StringBuffer output, Tuple tuple, TupleMetadata tupleMetadata, int column) throws IOException, ColumnNotFoundException {
        InputStream stream = null;
        try {
            Blob value = tuple.getBlob(column);
            if (value == null) {
                output.append("<null/>");
                return;
            }
            stream = value.getBinaryStream();
        } catch (SQLException e) {
            IOException exception = new IOException("Could not read from BLOB");
            exception.initCause(e);
            throw exception;
        }
        if (stream != null) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[2048];
            int read = 0;
            while ((read = stream.read(bytes)) >= 0) {
                buffer.write(bytes, 0, read);
            }
            byte[] value = buffer.toByteArray();
            if ((value != null) && (value.length > 0)) {
                output.append(new String(Base64.encode(value)));
            }
        } else {
            output.append("<null/>");
        }
    }
}
