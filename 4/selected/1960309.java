package com.billdimmick.merkabah;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class MessageFormatAdapter {

    private static final Log log = LogFactory.getLog(MessageFormatAdapter.class);

    private final Map<String, String> parameters = new HashMap<String, String>();

    private String body;

    private MessageFormatAdapter() {
    }

    public static MessageFormatAdapter create(final String body) {
        final MessageFormatAdapter adapter = new MessageFormatAdapter();
        adapter.body = body;
        return adapter;
    }

    public static MessageFormatAdapter parse(final String message) {
        Validate.notNull(message, "Cannot parse null message");
        final MessageFormatAdapter adapter = new MessageFormatAdapter();
        final BufferedReader reader = new BufferedReader(new StringReader(message));
        String line;
        do {
            try {
                line = StringUtils.defaultString(reader.readLine(), "");
            } catch (IOException io) {
                throw new IllegalStateException("IOException on reading in-memory string.");
            }
            final String[] pair = line.split("=", 2);
            if (pair.length == 2) {
                if ("null".equals(pair[1])) {
                    pair[1] = null;
                }
                adapter.parameters.put(pair[0], pair[1]);
            }
        } while (!"".equals(line));
        final StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(reader, writer);
        } catch (IOException io) {
            throw new IllegalStateException("IOException on reading in-memory string.");
        }
        adapter.body = writer.toString();
        return adapter;
    }

    public Collection<String> getKeys() {
        return Collections.unmodifiableCollection(this.parameters.keySet());
    }

    public String getParameter(final String key) {
        Validate.notNull(key, "Provided key cannot be null.");
        return this.parameters.get(key);
    }

    public void setParameter(final String key, final String value) {
        Validate.notNull(key, "Provided key cannot be null.");
        this.parameters.put(key, value);
    }

    public String getBody() {
        return this.body;
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
            builder.append(entry.getKey());
            builder.append("=");
            builder.append(entry.getValue());
            builder.append("\n");
        }
        builder.append("\n");
        builder.append(this.body);
        return builder.toString();
    }

    public boolean equals(final Object o) {
        if (!(o instanceof MessageFormatAdapter)) {
            return false;
        }
        final MessageFormatAdapter target = (MessageFormatAdapter) o;
        if (!StringUtils.equals(this.body, target.body)) {
            return false;
        }
        if (this.parameters.size() != target.parameters.size()) {
            return false;
        }
        for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
            if (!StringUtils.equals(entry.getValue(), target.getParameter(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
