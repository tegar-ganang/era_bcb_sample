package net.sf.filePiper.processors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.sf.filePiper.model.FileProcessorEnvironment;
import net.sf.filePiper.model.OneToOneByteFileProcessor;

/**
 * Processor that simply copy the input stream to the ouput stream without content modification.
 * 
 * @author berol
 */
public class CopyProcessor extends OneToOneByteFileProcessor {

    public String getProcessorName() {
        return "Copy";
    }

    public String getProposedNameSuffix() {
        return "copy";
    }

    public String getProcessorDescription() {
        return "Copy input file to ouput";
    }

    @Override
    public void process(InputStream is, OutputStream os, FileProcessorEnvironment env) throws IOException {
        byte[] buffer = new byte[1024];
        int readCount;
        while (((readCount = is.read(buffer)) >= 0) && env.shouldContinue()) {
            os.write(buffer, 0, readCount);
            bytesProcessed(readCount);
        }
    }
}
