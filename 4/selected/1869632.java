package jmodnews.nntp;

import java.io.IOException;
import jmodnews.db.Server;

/**
 * A task that requires a NNTP server connection.
 * 
 * @author Michael Schierl <schierlm@gmx.de>
 */
public abstract class NNTPTask {

    public Server getServer() {
        return server;
    }

    protected Server server;

    private NNTPThread nntpThread;

    private String title;

    private boolean stopped;

    public NNTPTask(Server server, String title) {
        this.server = server;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public abstract void runTask() throws IOException, NNTPException, StopNNTPThreadException;

    public void failedTask() {
    }

    public void stopTask() {
        setThreadStatus("stopping");
        stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }

    protected void writeLine(String line) throws IOException {
        nntpThread.writeLine(line);
    }

    protected String readLine() throws IOException {
        return nntpThread.readLine();
    }

    /**
     * @param thread
     */
    protected void setNNTPThread(NNTPThread thread) {
        if (nntpThread != null) throw new IllegalStateException();
        nntpThread = thread;
    }

    protected NNTPThread getNNTPThread() {
        return nntpThread;
    }

    protected NNTPController getController() {
        return nntpThread.getController();
    }

    protected OverviewFormat getOverviewFormat() throws IOException, NNTPException {
        return nntpThread.getOverviewFormat();
    }

    protected void nntpError(String errorWhile, String description, String line) throws NNTPException {
        nntpThread.nntpError(errorWhile, description, line);
    }

    protected void setThreadStatus(String status) {
        if (stopped) return;
        nntpThread.setStatus(title + " (" + status + ")");
    }

    protected void ensureStatusCode(String line, int code, String errorWhile) throws NNTPException {
        nntpThread.ensureStatusCode(line, code, errorWhile);
    }
}
