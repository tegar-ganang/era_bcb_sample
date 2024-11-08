package gnu.saw.server.filesystem;

import gnu.saw.server.session.SAWServerSession;
import java.io.File;
import java.io.IOException;

public class SAWServerFileCreateOperation implements Runnable {

    private volatile boolean finished;

    private File target;

    private SAWServerSession session;

    public SAWServerFileCreateOperation(SAWServerSession session) {
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
                    if (target.createNewFile()) {
                        synchronized (this) {
                            session.getConnection().getResultWriter().write("\nSAW>SAWFILECREATE:File '" + target.getPath() + "' created on server file system!\nSAW>");
                            session.getConnection().getResultWriter().flush();
                            finished = true;
                        }
                    } else {
                        synchronized (this) {
                            session.getConnection().getResultWriter().write("\nSAW>SAWFILECREATE:File '" + target.getPath() + "' cannot be created on server file system!\nSAW>");
                            session.getConnection().getResultWriter().flush();
                            finished = true;
                        }
                    }
                } else {
                    synchronized (this) {
                        session.getConnection().getResultWriter().write("\nSAW>SAWFILECREATE:Path '" + target.getPath() + "' already exists on server file system!\nSAW>");
                        session.getConnection().getResultWriter().flush();
                        finished = true;
                    }
                }
            } catch (SecurityException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWFILECREATE:Security error detected!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            } catch (IOException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWFILECREATE:File '" + target.getPath() + "' cannot be created on server file system!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            } catch (NullPointerException e) {
                synchronized (this) {
                    session.getConnection().getResultWriter().write("\nSAW>SAWFILECREATE:File '" + target.getPath() + "' cannot be created on server file system!\nSAW>");
                    session.getConnection().getResultWriter().flush();
                    finished = true;
                }
            }
        } catch (Exception e) {
        }
        finished = true;
    }
}
