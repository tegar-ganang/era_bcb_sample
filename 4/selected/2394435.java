package gnu.saw.server.filesystem;

import gnu.saw.server.session.SAWServerSession;
import java.io.File;
import java.io.IOException;

public class SAWServerDirectoryCreateOperation implements Runnable {

    private volatile boolean finished;

    private File target;

    private SAWServerSession session;

    public SAWServerDirectoryCreateOperation(SAWServerSession session) {
        this.session = session;
        this.finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setTarget(File target) {
        this.target = target;
        if (!target.isAbsolute()) {
            this.target = new File(session.getWorkingDirectory(), target.getPath());
        }
    }

    public void run() {
        try {
            try {
                if (!target.exists()) {
                    if (target.mkdirs()) {
                        synchronized (this) {
                            session.getConnection().getResultWriter().write("\nSAW>SAWDIRECTORYCREATE:Directory '" + target.getPath() + "' created on server file system!\nSAW>");
                            session.getConnection().getResultWriter().flush();
                            finished = true;
                        }
                    } else {
                        synchronized (this) {
                            session.getConnection().getResultWriter().write("\nSAW>SAWDIRECTORYCREATE:Directory '" + target.getPath() + "' cannot be created on server file system!\nSAW>");
                            session.getConnection().getResultWriter().flush();
                            finished = true;
                        }
                    }
                } else {
                    synchronized (this) {
                        session.getConnection().getResultWriter().write("\nSAW>SAWDIRECTORYCREATE:Path '" + target.getPath() + "' already exists on server file system!\nSAW>");
                        session.getConnection().getResultWriter().flush();
                        finished = true;
                    }
                }
            } catch (SecurityException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWDIRECTORYCREATE:Security error detected!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            } catch (IOException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWDIRECTORYCREATE:Directory '" + target.getPath() + "' cannot be created on server file system!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            } catch (NullPointerException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWDIRECTORYCREATE:Directory '" + target.getPath() + "' cannot be created on server file system!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            }
        } catch (Exception e) {
        }
        finished = true;
    }
}
