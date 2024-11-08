package cz.cuni.mff.ufal.volk.patterns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;

/**
 * TODO
 *
 * The implementation is NOT thread-safe.
 *
 * @author Bartłomiej Etenkowski
 */
public class LanguageDescription {

    /**
	 * An empty language description.
	 */
    public static LanguageDescription EMPTY = new LanguageDescription();

    public LanguageDescription(InputStream spec) throws IOException {
        this(LanguageDescription.class.getResource("/pl/predef.pl").openStream(), spec);
    }

    public LanguageDescription(InputStream base, InputStream spec) throws IOException {
        buffer = readAllBytes(new SequenceInputStream(base, spec));
        languageStream = new ByteArrayInputStream(buffer);
    }

    /**
	 * Creates an empty language description. Not for public use, one should use
	 * {@see LanguageDescription#EMPTY} constant.
	 */
    private LanguageDescription() {
        try {
            URL predefUrl = LanguageDescription.class.getResource("/pl/predef.pl");
            InputStream in = predefUrl.openStream();
            try {
                buffer = readAllBytes(in);
                languageStream = new ByteArrayInputStream(buffer);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("An IOException occurred during initialization of an empty language description", e);
        }
    }

    private byte[] buffer;

    private InputStream languageStream;

    public InputStream getLanguageStream() {
        return languageStream;
    }

    private static byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1000];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            out.close();
        }
    }
}
