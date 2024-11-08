package org.vd.extensions.javamail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import javax.mail.internet.MimeUtility;

/**
 * Created on Oct 10, 2006 2:41:15 PM by Ajay
 */
public class JavaMailUtils {

    private static final int READ_TEST_LIMIT = 512;

    public static String guessContentType(BufferedInputStream stream) throws IOException {
        InputStream in = readTestSegment(stream);
        try {
            return URLConnection.guessContentTypeFromStream(in);
        } finally {
            in.close();
        }
    }

    public static String getEncoding(InputDataSource source) throws IOException {
        InputStream in = readTestSegment(source.getInputStream());
        try {
            InputDataSource copy = new InputDataSource(new BufferedInputStream(in), source.getContentType(), source.getName());
            return MimeUtility.getEncoding(copy);
        } finally {
            in.close();
        }
    }

    private static InputStream readTestSegment(BufferedInputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(READ_TEST_LIMIT);
        try {
            byte[] data = new byte[READ_TEST_LIMIT];
            in.mark(READ_TEST_LIMIT);
            try {
                int read = in.read(data);
                while (read > 0 && bout.size() < READ_TEST_LIMIT) {
                    bout.write(data, 0, read);
                    int diff = READ_TEST_LIMIT - bout.size();
                    if (diff > 0) read = in.read(data, 0, diff);
                }
            } finally {
                in.reset();
            }
            return new ByteArrayInputStream(bout.toByteArray());
        } finally {
            bout.close();
        }
    }
}
