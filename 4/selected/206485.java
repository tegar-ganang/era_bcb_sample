package org.fpse.store.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import org.fpse.server.TaggedOutputStream;

/**
 * Created on Jan 10, 2007 12:43:51 AM by Ajay
 */
public class DelayedLoadString implements CharSequence {

    private final File m_source;

    private String m_text;

    public DelayedLoadString(File file) {
        m_source = file;
    }

    private void ensureFileLoaded() {
        if (null != m_text) return;
        StringWriter writer = new StringWriter(2048);
        InputStream in = null;
        Reader reader = null;
        try {
            in = new FileInputStream(m_source);
            reader = new InputStreamReader(in, TaggedOutputStream.DEFAULT_CHARSET);
            char[] temp = new char[2048];
            int read = reader.read(temp);
            while (read > 0) {
                writer.write(temp, 0, read);
                read = reader.read(temp);
            }
            m_text = writer.toString();
        } catch (IOException e) {
            throw new InvalidDataFileFound(m_source.getAbsolutePath(), e);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException _) {
                }
            }
            if (null != in) {
                try {
                    in.close();
                } catch (IOException _) {
                }
            }
            writer = null;
        }
    }

    public char charAt(int index) {
        ensureFileLoaded();
        return m_text.charAt(index);
    }

    public int length() {
        ensureFileLoaded();
        return m_text.length();
    }

    public CharSequence subSequence(int start, int end) {
        ensureFileLoaded();
        return m_text.subSequence(start, end);
    }

    @Override
    public String toString() {
        ensureFileLoaded();
        return m_text;
    }
}
