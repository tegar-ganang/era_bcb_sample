package edu.nps.moves;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant task to take a timestamp from one file and jam it into another;
 * Requires 2 file parameters: source (of timestamp) and receiver (of timestamp)
 * @author Mike Bailey
 */
public class SyncTimeStamps extends Task {

    private File receiver;

    private File source;

    public File getSource() {
        return source;
    }

    public void setSource(File source) {
        this.source = source;
    }

    public File getReceiver() {
        return receiver;
    }

    public void setReceiver(File receiver) {
        this.receiver = receiver;
    }

    @Override
    public void execute() throws BuildException {
        try {
            long time = source.lastModified();
            receiver.setLastModified(time);
        } catch (SecurityException sex) {
            throw new BuildException("Can't read from source or write to target", sex);
        } catch (IllegalArgumentException illArgEx) {
            throw new BuildException("Illegal timestamp read from source", illArgEx);
        }
    }

    private void checkAttributes() {
        if (source == null) throw new BuildException("Source file is required");
        if (!source.exists()) throw new BuildException("Source file must exist");
        if (!source.canRead()) throw new BuildException("Source file must be readable");
        if (receiver == null) throw new BuildException("Receiver file is required");
        if (!receiver.exists()) throw new BuildException("Receiver file must exist");
        if (!receiver.canWrite()) throw new BuildException("Receiver file must be writable");
    }
}
