package com.innovative.io.serializer;

import org.w3c.dom.Document;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import com.innovative.io.dialogs.FileChooser;
import com.innovative.main.Main;
import com.innovative.main.MainFrame;
import com.innovative.util.ArrayUtils;

/**
 *
 * @param <T>
 * @param <R>
 * @param <W> 
 * @author Dylon Edwards
 */
public abstract class AbstractSerializer<T, R extends Reader<T>, W extends Writer<T>> implements SerializerConstants, Serializer<T, R, W> {

    protected final R reader;

    protected final W writer;

    protected final FileChooser fileReader;

    protected final FileChooser fileWriter;

    public AbstractSerializer(final R reader, final W writer) {
        this.reader = reader;
        this.writer = writer;
        fileReader = new FileChooser(reader.getFormats());
        fileReader.setMultiSelectionEnabled(false);
        fileWriter = new FileChooser(writer.getFormats());
        fileWriter.setMultiSelectionEnabled(false);
    }

    @Override
    public R getReader() {
        return reader;
    }

    @Override
    public W getWriter() {
        return writer;
    }

    @Override
    public String[] getFormats() {
        return ArrayUtils.unique(String.class, reader.getFormats(), writer.getFormats());
    }

    @Override
    public T read(final File file) throws Exception {
        return reader.read(file);
    }

    @Override
    public T read(final String path) throws Exception {
        final File file = new File(path);
        return read(file);
    }

    @Override
    public T read() throws Exception {
        T instance = null;
        fileReader.lock();
        try {
            if (fileReader.showOpenDialog(MainFrame.getInstance()) == FileChooser.APPROVE_OPTION) {
                final File file = fileReader.getSelectedFile();
                instance = read(file);
            }
        } finally {
            fileReader.unlock();
        }
        return instance;
    }

    public static void write(final Document doc, final File file) throws Exception {
        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        final OutputStreamWriter writer = new OutputStreamWriter(out, ENCODING);
        final StreamResult result = new StreamResult(writer);
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        if (Main.debug()) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        }
        final DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
    }

    @Override
    public void write(final T instance, final File file) throws Exception {
        writer.write(instance, file);
    }

    @Override
    public void write(final T instance, final String path) throws Exception {
        final File file = new File(path);
        write(instance, file);
    }

    @Override
    public void write(final T instance) throws Exception {
        fileWriter.lock();
        try {
            if (fileWriter.showSaveDialog(MainFrame.getInstance()) == FileChooser.APPROVE_OPTION) {
                final File file = fileWriter.getSelectedFile();
                write(instance, file);
            }
        } finally {
            fileWriter.unlock();
        }
    }
}
