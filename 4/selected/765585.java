package de.ddb.conversion.converters;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.ddb.conversion.CharstreamConverter;
import de.ddb.conversion.ConversionParameters;
import de.ddb.conversion.ConverterException;

/**
 * @author heck
 *
 */
public class DumpConverter extends CharstreamConverter {

    private static final Log LOGGER = LogFactory.getLog(DumpConverter.class);

    @Override
    public void convert(Reader reader, Writer writer, ConversionParameters params) throws ConverterException, IOException {
        try {
            char[] buffer = new char[1024];
            int num = 0;
            while ((num = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, num);
            }
            writer.flush();
        } finally {
            closeReader(reader);
            closeWriter(writer);
        }
    }

    private void closeReader(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close reader.");
            }
        }
    }

    private void closeWriter(Writer writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close writer.");
            }
        }
    }
}
