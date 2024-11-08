package proguard;

import proguard.classfile.ClassPool;
import proguard.io.*;
import java.io.IOException;

/**
 * This class writes the output class files.
 *
 * @author Eric Lafortune
 */
public class OutputWriter {

    private Configuration configuration;

    /**
     * Creates a new OutputWriter to write output class files as specified by
     * the given configuration.
     */
    public OutputWriter(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Writes the given class pool to class files, based on the current
     * configuration.
     */
    public void execute(ClassPool programClassPool) throws IOException {
        ClassPath programJars = configuration.programJars;
        ClassPathEntry firstEntry = programJars.get(0);
        if (firstEntry.isOutput()) {
            throw new IOException("The output jar [" + firstEntry.getName() + "] must be specified after an input jar, or it will be empty.");
        }
        for (int index = 0; index < programJars.size() - 1; index++) {
            ClassPathEntry entry = programJars.get(index);
            if (entry.isOutput()) {
                if (entry.getFilter() == null && entry.getJarFilter() == null && entry.getWarFilter() == null && entry.getEarFilter() == null && entry.getZipFilter() == null && programJars.get(index + 1).isOutput()) {
                    throw new IOException("The output jar [" + entry.getName() + "] must have a filter, or all subsequent jars will be empty.");
                }
                for (int inIndex = 0; inIndex < programJars.size(); inIndex++) {
                    ClassPathEntry otherEntry = programJars.get(inIndex);
                    if (!otherEntry.isOutput() && entry.getFile().equals(otherEntry.getFile())) {
                        throw new IOException("The output jar [" + entry.getName() + "] must be different from all input jars.");
                    }
                }
            }
        }
        int firstInputIndex = 0;
        int lastInputIndex = 0;
        for (int index = 0; index < programJars.size(); index++) {
            ClassPathEntry entry = programJars.get(index);
            if (!entry.isOutput()) {
                lastInputIndex = index;
            } else {
                int nextIndex = index + 1;
                if (nextIndex == programJars.size() || !programJars.get(nextIndex).isOutput()) {
                    writeOutput(programClassPool, programJars, firstInputIndex, lastInputIndex + 1, nextIndex);
                    firstInputIndex = nextIndex;
                }
            }
        }
    }

    /**
     * Transfers the specified input jars to the specified output jars.
     */
    private void writeOutput(ClassPool programClassPool, ClassPath classPath, int fromInputIndex, int fromOutputIndex, int toOutputIndex) throws IOException {
        try {
            DataEntryWriter writer = DataEntryWriterFactory.createDataEntryWriter(classPath, fromOutputIndex, toOutputIndex);
            DataEntryReader reader = new ClassFileFilter(new ClassFileRewriter(programClassPool, writer), new DataEntryCopier(writer));
            new InputReader(configuration).readInput("  Copying resources from program ", classPath, fromInputIndex, fromOutputIndex, reader);
            writer.close();
        } catch (IOException ex) {
            throw new IOException("Can't write [" + classPath.get(fromOutputIndex).getName() + "] (" + ex.getMessage() + ")");
        }
    }
}
