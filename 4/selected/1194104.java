package org.databene.platform.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.databene.benerator.engine.BeneratorContext;
import org.databene.commons.Assert;
import org.databene.commons.Context;
import org.databene.commons.ErrorHandler;
import org.databene.commons.IOUtil;
import org.databene.task.AbstractTask;
import org.databene.task.TaskResult;

/**
 * Joins several source files into a destination file. 
 * If the property 'append' is 'true' and the destination file already exists, 
 * it will append the source files' contents to the existing file.<br/>
 * <br/>
 * Created at 16.09.2009 15:50:25
 * @since 0.6.0
 * @author Volker Bergmann
 */
public class FileJoiner extends AbstractTask {

    private static final int BUFFER_SIZE = 65536;

    private String[] sources = new String[0];

    private String destination = null;

    private boolean append = false;

    private boolean deleteSources = false;

    public String[] getSources() {
        return sources;
    }

    public void setSources(String[] sources) {
        this.sources = sources;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public boolean isDeleteSources() {
        return deleteSources;
    }

    public void setDeleteSources(boolean deleteSources) {
        this.deleteSources = deleteSources;
    }

    public TaskResult execute(Context ctx, ErrorHandler errorHandler) {
        Assert.notNull(destination, "property 'destination'");
        BeneratorContext context = (BeneratorContext) ctx;
        byte[] buffer = new byte[BUFFER_SIZE];
        OutputStream out = null;
        try {
            File destFile = new File(context.resolveRelativeUri(destination));
            out = new FileOutputStream(destFile, append);
            for (String source : sources) appendFile(out, source, buffer, context);
            if (deleteSources) for (String source : sources) {
                File file = new File(source);
                if (!file.delete()) errorHandler.handleError("File could not be deleted: " + file + ". " + "Probably it is locked");
            }
        } catch (IOException e) {
            errorHandler.handleError("Error joining files: " + sources, e);
        } finally {
            IOUtil.close(out);
        }
        return TaskResult.FINISHED;
    }

    private void appendFile(OutputStream out, String source, byte[] buffer, BeneratorContext context) throws FileNotFoundException, IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(context.resolveRelativeUri(source));
            int partLength = 0;
            while ((partLength = in.read(buffer)) > 0) out.write(buffer, 0, partLength);
        } finally {
            IOUtil.close(in);
        }
    }
}
