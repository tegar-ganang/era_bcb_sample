package proguard.io;

import proguard.classfile.*;
import java.io.*;

/**
 * This DataEntryReader writes the resource data entries that it reads to a
 * given DataEntryWriter, updating their contents based on the renamed classes
 * in the given ClassPool.
 *
 * @author Eric Lafortune
 */
public class DataEntryRewriter extends DataEntryCopier {

    private final ClassPool classPool;

    /**
     * Creates a new DataEntryRewriter.
     */
    public DataEntryRewriter(ClassPool classPool, DataEntryWriter dataEntryWriter) {
        super(dataEntryWriter);
        this.classPool = classPool;
    }

    protected void copyData(InputStream inputStream, OutputStream outputStream) throws IOException {
        Reader reader = new BufferedReader(new InputStreamReader(inputStream));
        Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        copyData(reader, writer);
        writer.flush();
        outputStream.flush();
    }

    /**
     * Copies all data that it can read from the given reader to the given
     * writer.
     */
    protected void copyData(Reader reader, Writer writer) throws IOException {
        StringBuffer word = new StringBuffer();
        while (true) {
            int i = reader.read();
            if (i < 0) {
                break;
            }
            char c = (char) i;
            if (Character.isJavaIdentifierPart(c) || c == '.' || c == '-') {
                word.append(c);
            } else {
                writeUpdatedWord(writer, word.toString());
                word.setLength(0);
                writer.write(c);
            }
        }
        writeUpdatedWord(writer, word.toString());
    }

    /**
     * Writes the given word to the given writer, after having adapted it,
     * based on the renamed class names.
     */
    private void writeUpdatedWord(Writer writer, String word) throws IOException {
        if (word.length() > 0) {
            String newWord = word;
            boolean containsDots = word.indexOf('.') >= 0;
            String className = containsDots ? word.replace('.', ClassConstants.INTERNAL_PACKAGE_SEPARATOR) : word;
            Clazz clazz = classPool.getClass(className);
            if (clazz != null) {
                String newClassName = clazz.getName();
                if (!className.equals(newClassName)) {
                    newWord = containsDots ? newClassName.replace(ClassConstants.INTERNAL_PACKAGE_SEPARATOR, '.') : newClassName;
                }
            }
            writer.write(newWord);
        }
    }
}
