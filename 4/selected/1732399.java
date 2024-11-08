package com.astromine.mp3;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.astromine.base.Log;

/**
 * Play List Parser
 * @author Stephen Fox
 *
 */
public class StreamTranslator extends AbstractTranslator {

    public StreamTranslator() {
        super();
    }

    public StreamTranslator(byte[] data) {
        super();
    }

    public StreamTranslator(InputStream stream) {
        super();
    }

    /**
     * @param data this is expected to be the URL address of the stream
     */
    public StreamTranslator(String data) {
        super();
    }

    public StreamTranslator(URL url) {
        setContent(url.toString());
        this.streams = parseStreams();
    }

    @Override
    public List<String> parseStreams() {
        List<String> list = new ArrayList<String>();
        boolean isValid = false;
        String value = getContent().trim();
        try {
            if (value.endsWith("\"")) {
                value = value.substring(0, value.length() - 1);
            }
            if (value.startsWith("\"")) {
                value = value.substring(1, value.length());
            }
            URI uri = new URI(value);
            URL url = new URL(value);
            if (uri.getHost() != null) {
                list.add(value);
                isValid = true;
            } else if (url.getProtocol() != null) {
                list.add(value);
                isValid = true;
            }
        } catch (URISyntaxException e1) {
            Log.writeToStdout(Log.WARNING, "StreamTranslator", "getChannels", "Invalid stream URI " + value);
        } catch (MalformedURLException e) {
            Log.writeToStdout(Log.WARNING, "StreamTranslator", "getChannels", "Malformed stream URL " + value);
        }
        if (!isValid) {
            list = null;
            Log.writeToStdout(Log.AUDIT, "StreamTranslator", "getChannels", "Invalid playlist");
        }
        return list;
    }
}
