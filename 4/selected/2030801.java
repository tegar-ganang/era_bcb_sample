package gov.nasa.jpf.doc;

public class ConfigIOImpl implements OptionsIO {

    protected OptionsReader reader;

    protected OptionsWriter writer;

    ConfigIOImpl(OptionsReader reader, OptionsWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    public Options read() throws ConfigIOException {
        return reader.read();
    }

    public void write(Options options) throws ConfigIOException {
        writer.write(options);
    }
}
