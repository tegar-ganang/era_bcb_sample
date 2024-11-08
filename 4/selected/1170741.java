package com.db4o.nb.api.impl;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.ext.ExtClient;
import com.db4o.messaging.MessageSender;
import com.db4o.nb.api.Db4oDatabase;
import com.db4o.nb.api.Db4oServer;
import com.db4o.nb.api.exception.Db4oServerException;
import com.db4o.nb.util.PreferencesUtil;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.openide.execution.ExecutionEngine;
import org.openide.execution.NbProcessDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

/**
 *
 * @author klevgert
 */
public class Db4oServerImpl implements Db4oServer {

    private final int TERM_WAIT = 6;

    private Db4oDatabase database;

    private boolean stop = false;

    private Process proc;

    private InputOutput io;

    private int state = STOPPED;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(Db4oServerImpl.class);

    /** Tales all started server. Subject for change */
    public static List serverInstances = new ArrayList();

    /**
   * Creates a new instance of Db4oServerImpl
   */
    public Db4oServerImpl() {
    }

    /**
   * Creates a new instance initialized with a 
   * database to host.
   * @param db   db4o database to host.
   */
    public Db4oServerImpl(Db4oDatabase db) {
        this.database = db;
    }

    /**
   */
    public Db4oDatabase getDatabase() {
        return database;
    }

    /**
   * Checks the running state of a process.
   * @return true, if process is running, otherwise false.
   */
    public boolean isStarted() {
        return (proc != null);
    }

    /**
   * Gets the running state
   */
    public int getState() {
        return this.state;
    }

    /**
   * Gets the running state
   */
    public void setState(int state) {
        int oldState = this.state;
        this.state = state;
        pcs.firePropertyChange("state", oldState, state);
    }

    /**
   * Starts a server hosting process.
   * A seperate vm for the server is started. This method starts another thread to monitor the 
   * connection.
   */
    public void start() {
        this.setState(STARTING);
        RequestProcessor.getDefault().post(new ServerRunner());
    }

    /**
   * Terminates a running server process.
   * First, a connection is made to the remote db4o server and a message is send
   * to safely terminate. If no connection can be made, theprocess is simply 
   * killed.
   */
    public void stop(boolean forced) {
        if (forced) {
            forceTermination();
        } else {
            this.setState(STOPPING);
            pcs.firePropertyChange("started", true, false);
            io.getOut().println("Server terminating...");
            RequestProcessor.getDefault().post(new ServerTerminator());
        }
    }

    /**
   * Registers a listener which will be notified when the
   * properties are modified.
   */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            pcs.addPropertyChangeListener(listener);
        }
    }

    /**
   * Unregisters a listener.
   *
   * If <code>listener</code> has not been registered previously,
   * nothing happens.  Also, no exception is thrown if
   * <code>listener</code> is <code>null</code>.
   */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) pcs.removePropertyChangeListener(listener);
    }

    /**
   * Builds the arguments for the process creation call.
   * @return arguments  stringified arguments 
   */
    private String buildArguments() throws UnsupportedEncodingException {
        Class c = StartServer.class;
        URL u = c.getProtectionDomain().getCodeSource().getLocation();
        String path1 = URLDecoder.decode(u.getPath().substring(1), System.getProperty("file.encoding"));
        Class c2 = com.db4o.Db4o.class;
        URL u2 = c2.getProtectionDomain().getCodeSource().getLocation();
        String path2 = URLDecoder.decode(u2.getPath().substring(1), System.getProperty("file.encoding"));
        StringBuffer sb = new StringBuffer();
        sb.append("-cp ").append('"').append(path1).append('"').append(";").append('"').append(path2).append('"').append(" ").append(StartServer.class.getName()).append(" ").append(this.getDatabase().getFile()).append(" ").append(PreferencesUtil.getServerPort()).append(" ").append(PreferencesUtil.getServerUser()).append(" ").append(PreferencesUtil.getServerPassword());
        String args = sb.toString();
        return args;
    }

    /**
   * Determines the java executable name for the process creation call.
   * The java executable name is obtained from default platform configured in
   * NetBeans.
   * @return name of java executable. 
   * @throws FileStateInvalidException, UnsupportedEncodingException
   */
    private String buildExecutableName() throws FileStateInvalidException, UnsupportedEncodingException {
        JavaPlatformManager pm = JavaPlatformManager.getDefault();
        JavaPlatform platform = pm.getDefaultPlatform();
        FileObject fo = platform.findTool("java");
        String executable = (fo != null) ? URLDecoder.decode(fo.getURL().getPath(), System.getProperty("file.encoding")) : "java";
        return executable;
    }

    private void forceTermination() {
        io.getOut().println("Forced termination...");
        synchronized (this) {
            if (proc != null) {
                proc.destroy();
            }
        }
    }

    /**
   * Starts the server and monitors the running state
   */
    private class ServerRunner implements Runnable {

        public void run() {
            try {
                io = IOProvider.getDefault().getIO("Db4o Output", false);
                serverInstances.add(Db4oServerImpl.this);
                io.select();
                OutputWriter writer = io.getOut();
                String executable = buildExecutableName();
                String arguments = buildArguments();
                NbProcessDescriptor nbe = new NbProcessDescriptor(executable, arguments);
                proc = nbe.exec();
                if (proc != null) {
                    writer.println("Server started hosting " + Db4oServerImpl.this.getDatabase().getFile());
                    Db4oServerImpl.this.setState(STARTED);
                    pcs.firePropertyChange("started", false, true);
                    InputStream is = proc.getInputStream();
                    final BufferedReader outRdr = new BufferedReader(new InputStreamReader(is));
                    readOutput(writer, is);
                    try {
                        proc.waitFor();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    writer.println("Server terminated (" + proc.exitValue() + ")");
                    writer.flush();
                    synchronized (this) {
                        proc = null;
                    }
                    writer.close();
                    Db4oServerImpl.this.setState(STOPPED);
                    pcs.firePropertyChange("started", true, false);
                    serverInstances.remove(Db4oServerImpl.this);
                } else {
                    writer.println("Could not start server process ");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new Db4oServerException(ex);
            }
        }

        private void readOutput(OutputWriter writer, InputStream is) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String outputString = null;
                while ((outputString = reader.readLine()) != null) {
                    writer.println(outputString);
                }
            } catch (Exception ex) {
                writer.println("Could not read process output " + ex);
            }
        }
    }

    /**
   * Terminate the server
   */
    private class ServerTerminator implements Runnable {

        public void run() {
            try {
                ObjectContainer objectContainer = Db4o.openClient(PreferencesUtil.getServerHost(), Integer.parseInt(PreferencesUtil.getServerPort()), PreferencesUtil.getServerUser(), PreferencesUtil.getServerPassword());
                ExtClient monitorClient = (ExtClient) objectContainer.ext();
                if (monitorClient != null) {
                    MessageSender messageSender = monitorClient.configure().clientServer().getMessageSender();
                    messageSender.send("terminate");
                }
                for (int i = 1; i <= TERM_WAIT; i++) {
                    if (monitorClient != null && !monitorClient.isAlive()) {
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                if (monitorClient == null || monitorClient.isAlive()) {
                    forceTermination();
                }
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                forceTermination();
            } catch (IOException ex) {
                ex.printStackTrace();
                forceTermination();
            }
        }
    }
}
