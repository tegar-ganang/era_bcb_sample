package org.jtools.shovel.format;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.jtools.shovel.api.Output;
import org.jtools.shovel.api.Record;
import org.jtools.shovel.meta.MetaData;
import org.jtools.shovel.meta.RecordDefinition;
import org.jtools.shovel.spi.ProcessableSPI;
import org.jtools.shovel.spi.SimpleSupportable;
import org.jtools.util.CommonUtils;

public class WriterOutput extends SimpleSupportable implements Output, ProcessableSPI {

    private Writer writer = null;

    private WriterFormat format = null;

    private WriterFormat.Factory writerFormatFactory = null;

    public WriterOutput() {
    }

    protected String writerName() {
        return "out";
    }

    protected Writer newWriter() throws IOException {
        Console console = System.console();
        Writer result = console == null ? null : console.writer();
        if (result == null) {
            result = new BufferedWriter(new OutputStreamWriter(System.out) {

                public void close() throws IOException {
                    flush();
                }
            });
        }
        return result;
    }

    protected Writer writer() throws IOException {
        if (writer == null) writer = newWriter();
        return writer;
    }

    protected WriterFormat newFormat(Writer writer) throws IOException {
        if (writerFormatFactory == null) throw new NullPointerException("writerFormatFactory");
        return writerFormatFactory.newInstance(writer);
    }

    protected WriterFormat format() throws IOException {
        if (format == null) format = newFormat(writer());
        return format;
    }

    public String toString() {
        String fName = format == null ? "<NO FORMAT>" : format.getName();
        String wName = writerName();
        return new StringBuilder(200).append(fName).append('[').append(wName).append(']').toString();
    }

    @Override
    public boolean start() throws IOException {
        format().start();
        return true;
    }

    @Override
    public Record process(Record dataRecord, boolean[] result) throws IOException {
        if (dataRecord != null) {
            format().execute(dataRecord);
            support().getResult().incWrite(1);
        }
        return dataRecord;
    }

    @Override
    public void close() throws IOException {
        CommonUtils.close(format());
        CommonUtils.close(writer());
    }

    @Override
    public void flush() throws IOException {
        format().flush();
        writer().flush();
    }

    @Override
    public ProcessableSPI getProcessableOutput() {
        return this;
    }

    @Override
    public RecordDefinition<MetaData> resolve(RecordDefinition<MetaData> metaData) {
        try {
            return format().resolve(metaData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(WriterFormat.Factory writerFormatFactory) {
        if (this.writerFormatFactory != null) throw new RuntimeException("writerFormatFactory already set");
        this.writerFormatFactory = support(writerFormatFactory);
    }
}
