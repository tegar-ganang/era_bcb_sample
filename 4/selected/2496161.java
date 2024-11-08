package gnu.saw.server.filesystem;

import gnu.saw.server.session.SAWServerSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import org.apache.commons.io.FileUtils;

public class SAWServerFileCopyOperation implements Runnable {

    private volatile boolean finished;

    private File sourceFile;

    private File destinationFile;

    private SAWServerSession session;

    public SAWServerFileCopyOperation(SAWServerSession session) {
        this.session = session;
        this.finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
        if (!sourceFile.isAbsolute()) {
            this.sourceFile = new File(session.getWorkingDirectory(), sourceFile.getPath());
        }
    }

    public void setDestinationFile(File destinationFile) {
        this.destinationFile = destinationFile;
        if (!destinationFile.isAbsolute()) {
            this.destinationFile = new File(session.getWorkingDirectory(), destinationFile.getPath());
        }
    }

    public void run() {
        try {
            try {
                if (sourceFile.isFile()) {
                    FileUtils.copyFile(sourceFile, destinationFile, false);
                } else {
                    FileUtils.copyDirectory(sourceFile, destinationFile, false);
                }
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWFILECOPY:File '" + sourceFile.getPath() + "' copied to '" + destinationFile.getPath() + "' on server file system!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            } catch (SecurityException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWFILECOPY:Security error detected!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            } catch (FileNotFoundException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWFILECOPY:File '" + sourceFile.getPath() + "' cannot be copied to '" + destinationFile.getPath() + "' on server file system!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            } catch (ClosedByInterruptException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWFILECOPY:File copy of file '" + sourceFile.getPath() + "' to '" + destinationFile.getPath() + "' interrupted!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            } catch (IOException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWFILECOPY:File '" + sourceFile.getPath() + "' cannot be copied to '" + destinationFile.getPath() + "' on server file system!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            }
        } catch (Exception e) {
        }
        finished = true;
    }
}
