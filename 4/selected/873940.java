package com.threerings.jpkg.ant.dpkg.scripts.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;
import org.apache.commons.io.IOUtils;
import com.threerings.jpkg.ant.dpkg.Dpkg;
import com.threerings.jpkg.ant.dpkg.DpkgData;

/**
 * A base64 encoded version of a {@link PackageScript} suitable for input to the uuencode command.
 * This class is public as required by Velocity, and should not be considered part of the public API.
 */
public class EncodedScript {

    /**
     * Construct a new EncodedScript from the supplied {@link PackageScript}.
     * @param source The {@link PackageScript} to encode.
     * @param data The {@link DpkgData} to pass to the script.
     * @throws IOException If the encoding fails.
     */
    public EncodedScript(PackageScript source, DpkgData data) throws IOException {
        _source = source;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream output = null;
        try {
            output = MimeUtility.encode(bytes, BASE64);
        } catch (final MessagingException e) {
            throw new IOException("Failed to uuencode script. name=[" + _source.getFriendlyName() + "], reason=[" + e.getMessage() + "].");
        }
        IOUtils.write(HEADER, bytes, Dpkg.CHAR_ENCODING);
        bytes.flush();
        IOUtils.copy(_source.getSource(data), output);
        output.flush();
        IOUtils.write(FOOTER, bytes, Dpkg.CHAR_ENCODING);
        bytes.flush();
        output.close();
        bytes.close();
        _encoded = bytes.toString(Dpkg.CHAR_ENCODING);
    }

    /**
     * Returns a human readable name for this script.
     * @see PackageScript#getFriendlyName()
     */
    public String getFriendlyName() {
        return _source.getFriendlyName();
    }

    /**
     * Returns whether this scripts failure should be reported to the packaging system.
     * @see PackageScript#failOnError()
     */
    public boolean failOnError() {
        return _source.failOnError();
    }

    /**
     * Returns the original script source base64 encoded.
     */
    public String getEncodedSource() {
        return _encoded;
    }

    /** The base64 constant as defined in javax.mail. */
    private static final String BASE64 = "base64";

    /** The header and footer required by the uuedecode command. */
    private static final String HEADER = "begin-base64 644 encoder.buf\n";

    private static final String FOOTER = "\n====";

    /** The uuencoded script source. */
    private final String _encoded;

    /** The script being encoded. */
    private final PackageScript _source;
}
