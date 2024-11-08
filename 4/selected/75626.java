package net.sf.filePiper.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.sf.sfac.file.FilePathUtils;
import net.sf.sfac.file.FileUtils;
import org.apache.log4j.Logger;

/**
 * Last component of the pipeline. <br>
 * This components creates output streams to final files (or to the console).
 * 
 * @author BEROL
 */
public class PipelineEnd implements PipeComponent {

    private static Logger log = Logger.getLogger(PipelineEnd.class);

    private Pipeline pipeline;

    private FilePathUtils toBaseDir;

    private PipelineEnvironment mainReporting;

    private File tempFile;

    private File output;

    public PipelineEnd(Pipeline thePipeline, FilePathUtils toDir, PipelineEnvironment reporting) {
        pipeline = thePipeline;
        toBaseDir = toDir;
        mainReporting = reporting;
    }

    private void copyTempFile() throws IOException {
        if (tempFile != null) {
            try {
                copyFile(tempFile, output);
                boolean success = tempFile.delete();
                if (!success) throw new IllegalStateException("Unable to delete temporary file: " + tempFile);
            } catch (Exception e) {
                IOException ioe = new IOException("Unable to rename '" + tempFile + "' to '" + output + "'");
                ioe.initCause(e);
                throw ioe;
            }
        }
        tempFile = null;
        output = null;
    }

    private void copyFile(File from, File to) throws IOException {
        FileUtils.ensureParentDirectoryExists(to);
        byte[] buffer = new byte[1024];
        int read;
        FileInputStream is = new FileInputStream(from);
        FileOutputStream os = new FileOutputStream(to);
        while ((read = is.read(buffer)) > 0) {
            os.write(buffer, 0, read);
        }
        is.close();
        os.close();
    }

    public File getOutputFile(File input, InputFileInfo info) throws IOException {
        if (pipeline.getOutputCardinality() == FileProcessor.MANY) {
            switch(pipeline.getOutputNameChoice()) {
                case Pipeline.OUTPUT_NAME_CURRENT:
                    String sourceRel = info.getInputRelativePath();
                    String dest = toBaseDir.getAbsoluteFilePath(sourceRel);
                    return new File(dest);
                case Pipeline.OUTPUT_NAME_PROPOSED:
                    String proposedRelativePath = info.getProposedRelativePath();
                    String destPath = toBaseDir.getAbsoluteFilePath(proposedRelativePath);
                    return new File(destPath);
                case Pipeline.OUTPUT_NAME_NEW:
                    throw new IOException("Cannot specify new name for multiple files");
                default:
                    throw new InternalError("Unknown outputNameChoice value = " + pipeline.getOutputNameChoice());
            }
        } else {
            switch(pipeline.getOutputNameChoice()) {
                case Pipeline.OUTPUT_NAME_CURRENT:
                    return input;
                case Pipeline.OUTPUT_NAME_PROPOSED:
                    return new File(info.getProposedFullPath());
                case Pipeline.OUTPUT_NAME_NEW:
                    if (pipeline.getOutputFile() == null) throw new IllegalArgumentException("You must define a destination file");
                    return pipeline.getOutputFile();
                default:
                    throw new InternalError("Unknown outputNameChoice value = " + pipeline.getOutputNameChoice());
            }
        }
    }

    /**
     * An output stream writing nothing.
     */
    static class Sink extends OutputStream {

        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }

        @Override
        public void write(byte[] b) {
        }
    }

    public void processInputStream(InputStream input, InputFileInfo info) {
        throw new UnsupportedOperationException("Last node cannot handle input stream");
    }

    public OutputStream createOutputStream(InputFileInfo info) throws IOException {
        copyTempFile();
        if (pipeline.getOutputDestination() == Pipeline.OUTPUT_TO_FILE) {
            File input = info.getInput();
            output = getOutputFile(input, info);
            File out = output;
            if (out.exists()) {
                boolean overwrite = mainReporting.canOverwriteFile(out);
                if (!overwrite) {
                    mainReporting.fileSkipped(out);
                    return new Sink();
                }
            }
            if (output.equals(input)) {
                tempFile = File.createTempFile("PiperTmp", "tmp");
                out = tempFile;
            }
            FileUtils.ensureParentDirectoryExists(output);
            if (log.isDebugEnabled()) log.debug("   === " + this + " Create output for input: " + info);
            mainReporting.outputtingToFile(out);
            return new FileOutputStream(out);
        } else {
            OutputStream out = mainReporting.getConsoleStream();
            mainReporting.outputtingToFile(null);
            out.write("\n".getBytes());
            out.write(info.getProposedFullPath().getBytes());
            out.write("\n".getBytes());
            out.write("---------------------------------------------------------------------------------".getBytes());
            out.write("\n".getBytes());
            return out;
        }
    }

    public void finished() throws IOException {
        copyTempFile();
    }
}
