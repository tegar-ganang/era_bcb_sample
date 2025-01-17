package com.bigfatgun.fixjures.json;

import com.bigfatgun.fixjures.FixtureException;
import static com.bigfatgun.fixjures.FixtureException.convert;
import com.bigfatgun.fixjures.FixtureSource;
import com.bigfatgun.fixjures.FixtureType;
import com.google.common.base.Charsets;
import com.google.common.base.Supplier;
import com.google.common.io.CharStreams;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/** Mocks objects based on JSON fixture data. */
public final class JSONSource extends FixtureSource {

    public static FixtureSource newJsonStream(final ReadableByteChannel channel) {
        return new JSONSource(channel);
    }

    public static FixtureSource newJsonFile(final File jsonFile) throws FileNotFoundException {
        return new JSONSource(new RandomAccessFile(jsonFile, "r").getChannel());
    }

    public static FixtureSource newJsonResource(final String resourceName) throws FileNotFoundException {
        return newJsonResource(FixtureSource.class.getClassLoader(), resourceName);
    }

    public static FixtureSource newJsonResource(final ClassLoader clsLoader, final String resourceName) {
        final InputStream input = clsLoader.getResourceAsStream(resourceName);
        if (input == null) {
            throw new FixtureException("Unable to locate resource: " + resourceName);
        } else {
            return new JSONSource(input);
        }
    }

    public static FixtureSource newJsonString(final String json) {
        return new JSONSource(new ByteArrayInputStream(json.getBytes(Charsets.UTF_8)));
    }

    public static FixtureSource newRemoteUrl(final URL url) {
        try {
            return new JSONSource(url.openStream());
        } catch (IOException e) {
            throw FixtureException.convert(e);
        }
    }

    private JSONSource(final InputStream input) {
        this(Channels.newChannel(input));
    }

    private JSONSource(final ReadableByteChannel source) {
        super(source);
    }

    public Object createFixture(final FixtureType type) {
        try {
            final String sourceJson = loadSource();
            Object jsonValue = parseJson(sourceJson);
            final Supplier<?> provider = findValue(type, jsonValue);
            final Object value = provider.get();
            return type.getType().cast(value);
        } catch (Exception e) {
            throw convert(e);
        }
    }

    private Object parseJson(final String json) {
        assert json != null : "JSON data cannot be null.";
        try {
            return JSONValue.parseWithException(json);
        } catch (ParseException e) {
            return FixtureException.convertAndThrowAs(e);
        }
    }

    private String loadSource() throws IOException {
        return CharStreams.toString(new InputStreamReader(Channels.newInputStream(getSource()), getCharset()));
    }
}
