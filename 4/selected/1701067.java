package pxl.types;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import pxl.errors.IoError;
import pxl.types.annotations.Exported;

public class PxlFile extends PxlObject {

    private String fileName;

    private BufferedReader reader;

    private BufferedWriter writer;

    public PxlFile(String fileName) {
        this.fileName = fileName;
        try {
            reader = new BufferedReader(new FileReader(fileName));
        } catch (IOException e) {
            throw new IoError("Cannot open file: " + e.getMessage());
        }
    }

    public PxlFile(String fileName, boolean read, boolean write) {
        this.fileName = fileName;
        try {
            reader = new BufferedReader(new FileReader(fileName));
        } catch (IOException e) {
            throw new IoError("Cannot open file: " + e.getMessage());
        }
    }

    @Override
    public String getType() {
        return "File";
    }

    @Override
    @Exported
    public PxlString __repr__() {
        StringBuilder sb = new StringBuilder();
        sb.append("<File '");
        sb.append(fileName);
        sb.append("'>");
        return new PxlString(sb.toString());
    }

    private static final Map<String, PxlObject> attrs = PxlObject.getExportedMethods(PxlFile.class);

    @Override
    protected PxlObject getAttribute(String name) {
        return attrs.get(name);
    }

    @Override
    protected Collection<String> getAttributeNames() {
        return attrs.keySet();
    }

    @Exported
    @Override
    public PxlIterator __getiterator__() {
        return new FileIterator(reader);
    }

    @Exported
    public PxlIterator lines() {
        return __getiterator__();
    }

    private static class FileIterator extends PxlIterator {

        private BufferedReader reader;

        public FileIterator(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public PxlBoolean hasNext() {
            try {
                return PxlBoolean.valueOf(reader.ready());
            } catch (IOException e) {
                throw new IoError("Read error: " + e.getMessage());
            }
        }

        @Override
        public PxlObject next() {
            try {
                return new PxlString(reader.readLine());
            } catch (IOException e) {
                throw new IoError("Read error: " + e.getMessage());
            }
        }

        @Override
        public void remove() {
        }
    }
}
