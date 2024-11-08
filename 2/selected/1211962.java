package ca.sqlpower.wabit.swingui.action;

import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.swingui.SPSwingWorker;
import ca.sqlpower.wabit.WabitSession;
import ca.sqlpower.wabit.dao.OpenWorkspaceXMLDAO;
import ca.sqlpower.wabit.swingui.OpenProgressWindow;
import ca.sqlpower.wabit.swingui.WabitSwingSession;
import ca.sqlpower.wabit.swingui.WabitSwingSessionContext;
import ca.sqlpower.wabit.swingui.WabitSwingSessionContextImpl;

/**
 * This action will load in workspaces from a user selected file to a given
 * context.
 */
public class OpenWorkspaceAction extends AbstractAction {

    private static final Logger logger = Logger.getLogger(OpenWorkspaceAction.class);

    /**
	 * This is the context within Wabit that will have the workspaces
	 * loaded into.
	 */
    private final WabitSwingSessionContext context;

    public OpenWorkspaceAction(WabitSwingSessionContext context) {
        super("Open Workspace...", WabitSwingSessionContextImpl.OPEN_WABIT_ICON);
        this.context = context;
    }

    public void actionPerformed(ActionEvent e) {
        final File importFile;
        if (!e.getActionCommand().startsWith("file:")) {
            File defaultFile = null;
            if (context.getActiveSession() != null) {
                defaultFile = context.getActiveSwingSession().getCurrentURIAsFile();
            }
            JFileChooser fc = new JFileChooser(defaultFile);
            fc.setDialogTitle("Select the file to load from.");
            fc.addChoosableFileFilter(SPSUtils.WABIT_FILE_FILTER);
            int fcChoice = fc.showOpenDialog(context.getFrame());
            if (fcChoice != JFileChooser.APPROVE_OPTION) {
                return;
            }
            importFile = fc.getSelectedFile();
        } else {
            importFile = new File(e.getActionCommand().substring("file:".length()));
        }
        loadFiles(context, importFile.toURI());
    }

    /**
     * Attempts to read the workspaces at the given URIs, adding them to the
     * given context only after every workspace has been loaded successfully.
     * Any URIs that could not be resolved are discarded after warning the user
     * that the URI(s) was/were unrecognized. The work of loading the workspaces
     * itself is done on a separate worker thread <i>after</i> this method
     * returns; the results of loading are integrated into the context on the
     * Swing Event Dispatch Thread after the worker thread has terminated.
     * <p>
     * The progress of the worker is made visible by use of a dialog with a
     * progress bar and a reasonably fine-grained status message. While that
     * dialog is visible, the session's frame is made unresponsive to mouse and
     * keyboard input.
     * <p>
     * If any of the workspaces whose URIs were deemed valid could not be opened
     * (probably due to IO errors or file corruption), a message to that effect
     * will be displayed to the user. No sessions will be added to the context,
     * even the ones that were loaded successfully before the IO error was
     * encountered.
     * 
     * @param context
     *            The context to open new workspaces into
     * @param importFiles
     *            The URIs to read workspace data from (Wabit XML format)
     */
    public static void loadFiles(final WabitSwingSessionContext context, final URI... importFiles) {
        final List<InputStream> ins = new ArrayList<InputStream>();
        final Map<URI, OpenWorkspaceXMLDAO> workspaceLoaders = new HashMap<URI, OpenWorkspaceXMLDAO>();
        List<String> invalidWorkspaces = new ArrayList<String>();
        for (URI importFile : importFiles) {
            BufferedInputStream in = null;
            OpenWorkspaceXMLDAO workspaceLoader = null;
            try {
                URL importURL = importFile.toURL();
                URLConnection urlConnection = importURL.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                ins.add(in);
                workspaceLoader = new OpenWorkspaceXMLDAO(context, in, urlConnection.getContentLength());
                workspaceLoaders.put(importFile, workspaceLoader);
            } catch (Exception e) {
                logger.info("Can't deal with URI " + importFile, e);
                StringBuilder message = new StringBuilder();
                message.append(importFile.toString());
                if (e instanceof FileNotFoundException) {
                    message.append(" (File not found)");
                } else {
                    message.append(" (").append(e.toString()).append(")");
                }
                invalidWorkspaces.add(message.toString());
            }
        }
        if (!invalidWorkspaces.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("The following workspace locations cannot be opened:\n");
            for (String s : invalidWorkspaces) {
                message.append("\n").append(s);
            }
            JOptionPane.showMessageDialog(context.getFrame(), message.toString(), "Some workspaces not opened", JOptionPane.WARNING_MESSAGE);
        }
        SPSwingWorker worker = new SPSwingWorker(context.getLoadingRegistry()) {

            private volatile OpenWorkspaceXMLDAO currentDAO;

            @Override
            public void doStuff() throws Exception {
                for (OpenWorkspaceXMLDAO dao : workspaceLoaders.values()) {
                    currentDAO = dao;
                    dao.loadWorkspacesFromStream();
                }
            }

            @Override
            public void cleanup() throws Exception {
                for (InputStream in : ins) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.warn("Failed to close a workspace input stream. " + "Squishing this exception: " + e);
                    }
                }
                if (getDoStuffException() != null) {
                    SPSUtils.showExceptionDialogNoReport(context.getFrame(), "Wabit had trouble opening your workspace", getDoStuffException());
                    return;
                }
                if (!isCancelled()) {
                    for (Map.Entry<URI, OpenWorkspaceXMLDAO> entry : workspaceLoaders.entrySet()) {
                        WabitSession registeredSession = null;
                        try {
                            registeredSession = entry.getValue().addLoadedWorkspacesToContext();
                            if (registeredSession != null) {
                                ((WabitSwingSession) registeredSession).setCurrentURI(entry.getKey());
                            }
                            try {
                                context.putRecentFileName(new File(entry.getKey()).getAbsolutePath());
                            } catch (IllegalArgumentException ignored) {
                            }
                        } catch (Exception e) {
                            SPSUtils.showExceptionDialogNoReport(context.getFrame(), "Wabit had trouble after opening the workspace located at " + entry.getKey(), e);
                        }
                    }
                    context.setEditorPanel();
                }
            }

            @Override
            protected Integer getJobSizeImpl() {
                int jobSize = 0;
                for (OpenWorkspaceXMLDAO workspaceDAO : workspaceLoaders.values()) {
                    jobSize += workspaceDAO.getJobSize();
                }
                return jobSize;
            }

            @Override
            protected String getMessageImpl() {
                OpenWorkspaceXMLDAO myCurrentDAO = currentDAO;
                if (myCurrentDAO != null) {
                    return myCurrentDAO.getMessage();
                } else {
                    return "";
                }
            }

            @Override
            protected int getProgressImpl() {
                int progress = 0;
                for (OpenWorkspaceXMLDAO workspaceDAO : workspaceLoaders.values()) {
                    progress += workspaceDAO.getProgress();
                }
                return progress;
            }

            @Override
            protected boolean hasStartedImpl() {
                boolean started = false;
                for (OpenWorkspaceXMLDAO workspaceDAO : workspaceLoaders.values()) {
                    started = started || workspaceDAO.hasStarted();
                }
                return started;
            }

            @Override
            protected boolean isFinishedImpl() {
                if (getDoStuffException() != null) {
                    return true;
                }
                boolean finished = true;
                for (OpenWorkspaceXMLDAO workspaceDAO : workspaceLoaders.values()) {
                    finished = finished && workspaceDAO.isFinished();
                }
                return finished;
            }

            @Override
            public synchronized boolean isCancelled() {
                boolean cancelled = false;
                for (OpenWorkspaceXMLDAO workspaceDAO : workspaceLoaders.values()) {
                    cancelled = cancelled || workspaceDAO.isCancelled();
                }
                return cancelled;
            }

            @Override
            public synchronized void setCancelled(boolean cancelled) {
                for (OpenWorkspaceXMLDAO workspaceDAO : workspaceLoaders.values()) {
                    workspaceDAO.setCancelled(cancelled);
                }
            }
        };
        OpenProgressWindow.showProgressWindow(context.getFrame(), worker);
        new Thread(worker).start();
    }
}
