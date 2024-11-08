package org.unitils.io.conversion.impl;

import org.unitils.io.conversion.ConversionStrategy;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 * This conversion strategy will try to convert the input stream into a String. The default file extension for this
 * conversion strategy is txt. So when not overriding the default file when using the @FileContent the file should
 * end with '.txt' .
 *
 * @author Jeroen Horemans
 * @author Tim Ducheyne
 * @author Thomas De Rycke
 * @since 3.3
 */
public class StringConversionStrategy implements ConversionStrategy<String> {

    public String convertContent(InputStream inputStream, String encoding) throws IOException {
        StringWriter writer = new StringWriter();
        InputStreamReader in = new InputStreamReader(inputStream, encoding);
        IOUtils.copy(in, writer);
        return writer.toString();
    }

    public String getDefaultFileExtension() {
        return "txt";
    }

    public Class<String> getTargetType() {
        return String.class;
    }
}
