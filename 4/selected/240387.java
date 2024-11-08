package de.fu_berlin.inf.dpp.ui.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;
import org.picocontainer.Disposable;
import org.picocontainer.annotations.Inject;
import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.User;
import de.fu_berlin.inf.dpp.editor.internal.EditorAPI;
import de.fu_berlin.inf.dpp.exceptions.ConnectionException;
import de.fu_berlin.inf.dpp.exceptions.ReceiverGoneException;
import de.fu_berlin.inf.dpp.exceptions.RemoteCancellationException;
import de.fu_berlin.inf.dpp.exceptions.SarosCancellationException;
import de.fu_berlin.inf.dpp.net.internal.StreamService;
import de.fu_berlin.inf.dpp.net.internal.StreamServiceManager;
import de.fu_berlin.inf.dpp.net.internal.StreamSession;
import de.fu_berlin.inf.dpp.project.SarosSessionManager;
import de.fu_berlin.inf.dpp.ui.Messages;
import de.fu_berlin.inf.dpp.ui.util.DialogUtils;
import de.fu_berlin.inf.dpp.ui.util.selection.SelectionUtils;
import de.fu_berlin.inf.dpp.ui.util.selection.retriever.SelectionRetrieverFactory;
import de.fu_berlin.inf.dpp.util.StopWatch;
import de.fu_berlin.inf.dpp.util.StreamJob;
import de.fu_berlin.inf.dpp.util.Utils;

/**
 * Whole feature for sending a file among Saros-users.
 * 
 * @author s-lau
 */
public class SendFileAction extends Action implements Disposable {

    private static final Logger log = Logger.getLogger(SendFileAction.class);

    public static final String ACTION_ID = SendFileAction.class.getName();

    protected ISelectionListener selectionListener = new ISelectionListener() {

        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
            updateEnablement();
        }
    };

    @Inject
    protected Saros saros;

    @Inject
    protected SarosSessionManager sessionManager;

    @Inject
    protected StreamServiceManager streamServiceManager;

    @Inject
    protected SendFileStreamService sendFileService;

    public SendFileAction() {
        super(Messages.SendFileAction_title);
        SarosPluginContext.initComponent(this);
        sendFileService.hookAction(this);
        setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
        setId(ACTION_ID);
        setToolTipText(Messages.SendFileAction_tooltip);
        setEnabled(false);
        SarosPluginContext.initComponent(this);
        SelectionUtils.getSelectionService().addSelectionListener(selectionListener);
        updateEnablement();
    }

    protected void updateEnablement() {
        List<User> participants = SelectionRetrieverFactory.getSelectionRetriever(User.class).getSelection();
        setEnabled(sessionManager.getSarosSession() != null && participants.size() == 1 && !participants.get(0).isLocal());
    }

    @Override
    public void run() {
        List<User> participants = null;
        try {
            participants = SelectionRetrieverFactory.getSelectionRetriever(User.class).getSelection();
            if (participants.size() != 1) {
                log.warn("More than one participant selected.");
                return;
            }
        } catch (NullPointerException e) {
            this.setEnabled(false);
        } catch (Exception e) {
            if (!PlatformUI.getWorkbench().isClosing()) log.error("Unexcepted error while updating enablement", e);
        }
        if (participants == null) return;
        FileDialog fd = new FileDialog(EditorAPI.getShell(), SWT.OPEN);
        fd.setText(Messages.SendFileAction_filedialog_text);
        String filename = fd.open();
        if (filename == null) return;
        File file = new File(filename);
        InputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            errorPopup(Messages.SendFileAction_error_cannot_read_file_title, MessageFormat.format(Messages.SendFileAction_error_cannot_read_file_message, filename), e, null);
            return;
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                log.warn("unable to close file:" + filename, e);
            }
        }
        new SendFileJob(participants.get(0), file).schedule();
    }

    /**
     * Shows an error windows and sets monitors subTask to <code>message</code>
     * or exceptions message.
     * 
     * @param title
     *            Title of error window
     * @param message
     *            Message of error window
     * @param e
     *            Exception caused this error, may be <code>null</code>
     * @param monitor
     *            May be <code>null</code>
     */
    protected static void errorPopup(final String title, final String message, Exception e, SubMonitor monitor) {
        Utils.runSafeSWTAsync(log, new Runnable() {

            public void run() {
                DialogUtils.openErrorMessageDialog(EditorAPI.getShell(), title, message);
            }
        });
        if (monitor != null) {
            if (e != null && e.getMessage() != null && !(e.getMessage().length() == 0)) monitor.subTask(e.getMessage()); else monitor.subTask(message);
        }
    }

    protected class SendFileJob extends StreamJob {

        protected User user;

        protected boolean sendSuccessfully = false;

        protected File fileToSend;

        public SendFileJob(User user, File fileToSend) {
            super(MessageFormat.format(Messages.SendFileAction_send_file_job_text, fileToSend.getName(), user.getHumanReadableName()));
            this.user = user;
            this.fileToSend = fileToSend;
        }

        @Override
        protected IStatus run(IProgressMonitor ipmonitor) {
            SubMonitor monitor = SubMonitor.convert(ipmonitor);
            monitor.beginTask(Messages.SendFileAction_monitor_set_up_session_text, 101);
            setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
            log.info("Asking " + user + " to accept our transfer.");
            monitor.subTask(MessageFormat.format(Messages.SendFileAction_monitor_notifying_text, user.getHumanReadableName()));
            try {
                setStreamSession(streamServiceManager.createSession(sendFileService, user, FileDescription.fromFile(fileToSend), null));
                monitor.worked(1);
            } catch (RemoteCancellationException e) {
                errorPopup(getName(), Messages.SendFileAction_error_file_transfer_rejected_text, e, monitor);
                return Status.CANCEL_STATUS;
            } catch (ConnectionException e) {
                errorPopup(getName(), Messages.SendFileAction_error_no_connection_establish_text, e, monitor);
                return Status.CANCEL_STATUS;
            } catch (TimeoutException e) {
                errorPopup(getName(), Messages.SendFileAction_error_timed_out_text, e, monitor);
                return Status.CANCEL_STATUS;
            } catch (ExecutionException e) {
                monitor.subTask(MessageFormat.format(Messages.SendFileAction_unexpected_error, e.getMessage()));
                log.error("Unexpected error: ", e);
                return new Status(IStatus.ERROR, Saros.SAROS, Messages.SendFileAction_status_could_not_create_session_text, e);
            } catch (InterruptedException e) {
                return Status.CANCEL_STATUS;
            }
            monitor.setTaskName(Messages.SendFileAction_monitor_sending_text);
            try {
                send(getStreamSession(), fileToSend, monitor.newChild(100, SubMonitor.SUPPRESS_SETTASKNAME));
            } catch (InterruptedIOException e) {
                monitor.subTask(Messages.SendFileAction_monitor_canceled_text);
                return Status.CANCEL_STATUS;
            } catch (IOException e) {
                if (streamException == null) {
                    log.error("Error while sending file: ", e);
                    return new Status(IStatus.ERROR, Saros.SAROS, "Error while sending file", e);
                } else {
                    if (streamException instanceof ReceiverGoneException) {
                        monitor.subTask(Messages.SendFileAction_monitor_receiver_left_during_transfer_text);
                        return Status.CANCEL_STATUS;
                    }
                    if (streamException instanceof ConnectionException) {
                        monitor.subTask(Messages.SendFileAction_monitor_lost_connection_text);
                        return Status.CANCEL_STATUS;
                    }
                    log.error("Unexpected error: ", streamException);
                    return new Status(IStatus.ERROR, Saros.SAROS, Messages.SendFileAction_status_unexpected_error, streamException);
                }
            } catch (RemoteCancellationException e) {
                monitor.subTask(Messages.SendFileAction_monitor_canceled_text);
                return Status.CANCEL_STATUS;
            } catch (SarosCancellationException e) {
                monitor.subTask(Messages.SendFileAction_monitor_canceled_text);
                return Status.CANCEL_STATUS;
            } finally {
                readyToStop.countDown();
                monitor.done();
            }
            monitor.setTaskName(sendSuccessfully ? Messages.SendFileAction_monitor_successful_sent_text : Messages.SendFileAction_monitor_not_sent_whole_file_text);
            return sendSuccessfully ? Status.OK_STATUS : Status.CANCEL_STATUS;
        }

        @Override
        protected void canceling() {
            if (getThread() != null) getThread().interrupt();
            getStreamSession().stopSession();
        }

        /**
         * 
         * @throws IOException
         *             Error while reading file or writing to stream.
         * @throws RemoteCancellationException
         *             buddy canceled.
         * @throws SarosCancellationException
         *             Local user canceled monitor.
         */
        protected void send(StreamSession session, File file, SubMonitor monitor) throws RemoteCancellationException, IOException, SarosCancellationException {
            monitor.subTask(Messages.SendFileAction_monitor_start_sending_text);
            monitor.setWorkRemaining((file.length() + 1) > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) file.length() + 1);
            byte[] buffer = new byte[sendFileService.getChunkSize()[0]];
            OutputStream out = session.getOutputStream(0);
            FileInputStream fileInputStream = new FileInputStream(file);
            int numRead = 0;
            long sendBytes = 0;
            monitor.worked(1);
            StopWatch watch = new StopWatch();
            watch.start();
            try {
                while ((numRead = fileInputStream.read(buffer)) > 0 && streamException == null) {
                    if (monitor.isCanceled()) throw new SarosCancellationException();
                    if (Thread.interrupted() || stopped) throw new RemoteCancellationException();
                    out.write(buffer, 0, numRead);
                    sendBytes += numRead;
                    monitor.worked(numRead);
                    monitor.subTask(watch.throughput(sendBytes));
                }
                out.flush();
                sendSuccessfully = sendBytes == file.length();
            } finally {
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(fileInputStream);
                monitor.done();
            }
        }
    }

    protected static class ReceiveFileJob extends StreamJob {

        protected boolean receivedSuccessfully = false;

        protected FileDescription fileDescription;

        public ReceiveFileJob(StreamSession session) {
            super(Messages.SendFileAction_receive_title);
            setStreamSession(session);
        }

        @Override
        protected IStatus run(IProgressMonitor ipmonitor) {
            SubMonitor monitor = SubMonitor.convert(ipmonitor);
            setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
            try {
                if (getStreamSession().getInitiationDescription() instanceof FileDescription) {
                    fileDescription = (FileDescription) getStreamSession().getInitiationDescription();
                } else {
                    log.error("Got no filedescription!");
                    return new Status(IStatus.ERROR, Saros.SAROS, Messages.SendFileAction_status_cannot_receive_file_text);
                }
                setName(MessageFormat.format(Messages.SendFileAction_status_receiving_text, fileDescription.name, getStreamSession().getRemoteJID()));
                monitor.beginTask(Messages.SendFileAction_monitor_choose_location_text, 102);
                File file;
                try {
                    file = saveFile(fileDescription.name, monitor.newChild(1, SubMonitor.SUPPRESS_SETTASKNAME));
                } catch (SarosCancellationException e) {
                    monitor.subTask(Messages.SendFileAction_monitor_canceled_text);
                    return Status.CANCEL_STATUS;
                }
                monitor.setTaskName(Messages.SendFileAction_monitor_receiving_text);
                FileOutputStream fileOutputStream;
                try {
                    monitor.subTask(Messages.SendFileAction_monitor_opening_text);
                    fileOutputStream = new FileOutputStream(file);
                    monitor.worked(1);
                } catch (FileNotFoundException e) {
                    errorPopup(Messages.SendFileAction_error_cannot_open_file_title, Messages.SendFileAction_error_cannot_open_file_message, e, monitor);
                    return new Status(IStatus.ERROR, Saros.SAROS, Messages.SendFileAction_status_cannot_open_file, e);
                }
                try {
                    receive(getStreamSession(), fileOutputStream, monitor.newChild(100, SubMonitor.SUPPRESS_SETTASKNAME));
                } catch (RemoteCancellationException e) {
                    monitor.subTask(Messages.SendFileAction_monitor_canceled_text);
                    return Status.CANCEL_STATUS;
                } catch (IOException e) {
                    if (stopped) {
                        monitor.subTask(Messages.SendFileAction_monitor_canceled_text);
                        return Status.CANCEL_STATUS;
                    }
                    if (streamException == null) {
                        log.error("Error while receiving file: ", e);
                        return new Status(IStatus.ERROR, Saros.SAROS, Messages.SendFileAction_error_receiving_file_text, e);
                    } else {
                        if (streamException instanceof ReceiverGoneException) {
                            monitor.subTask(Messages.SendFileAction_monitor_receiver_left_during_transfer_text);
                            return Status.CANCEL_STATUS;
                        }
                        if (streamException instanceof ConnectionException) {
                            monitor.subTask(Messages.SendFileAction_monitor_lost_connection_text);
                            return Status.CANCEL_STATUS;
                        }
                        log.error("Unexpected error: ", streamException);
                        return new Status(IStatus.ERROR, Saros.SAROS, Messages.SendFileAction_status_unexpected_error_text, streamException);
                    }
                } catch (SarosCancellationException e) {
                    monitor.subTask(Messages.SendFileAction_monitor_canceled_text);
                    return Status.CANCEL_STATUS;
                }
            } finally {
                if (!stopped && streamException == null) getStreamSession().stopSession();
                readyToStop.countDown();
                monitor.done();
            }
            monitor.setTaskName(receivedSuccessfully ? Messages.SendFileAction_monitor_successful_received_file_text : Messages.SendFileAction_monitor_could_not_receive_whole_file_text);
            return receivedSuccessfully ? Status.OK_STATUS : Status.CANCEL_STATUS;
        }

        protected void receive(final StreamSession session, FileOutputStream fileOutputStream, SubMonitor monitor) throws IOException, SarosCancellationException, RemoteCancellationException {
            monitor.subTask(Messages.SendFileAction_monitor_start_receive_file);
            long fileSize = ((FileDescription) session.getInitiationDescription()).size;
            monitor.setWorkRemaining(fileSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) fileSize);
            long received = 0;
            InputStream in = session.getInputStream(0);
            byte[] buffer = new byte[session.getService().getChunkSize()[0]];
            StopWatch watch = new StopWatch();
            watch.start();
            try {
                int readBytes;
                boolean canceled = false;
                while ((readBytes = in.read(buffer)) > 0) {
                    received += readBytes;
                    fileOutputStream.write(buffer, 0, readBytes);
                    monitor.worked(readBytes);
                    monitor.subTask(watch.throughput(received));
                    if (monitor.isCanceled() && !canceled) {
                        getStreamSession().stopSession();
                        canceled = true;
                    }
                    if (Thread.interrupted() && !monitor.isCanceled()) throw new RemoteCancellationException();
                }
                fileOutputStream.flush();
                if (monitor.isCanceled()) throw new SarosCancellationException();
                receivedSuccessfully = received == fileSize;
            } finally {
                IOUtils.closeQuietly(fileOutputStream);
                IOUtils.closeQuietly(in);
                monitor.done();
            }
        }

        /**
         * Prompts the user to choose a location for saving a file. If the file
         * exists, user is asked for confirmation to overwrite it, if he
         * declines he will be prompted again.
         * 
         * @return chosen file
         * @throws SarosCancellationException
         *             Dialog was canceled
         */
        protected File saveFile(final String targetFilename, SubMonitor monitor) throws SarosCancellationException {
            Callable<String> getFile = new Callable<String>() {

                public String call() throws Exception {
                    FileDialog fd = new FileDialog(EditorAPI.getShell(), SWT.SAVE);
                    fd.setText(Messages.SendFileAction_dialog_choose_location_title);
                    fd.setFileName(targetFilename);
                    File file = null;
                    String destinationFilename;
                    boolean again = false;
                    do {
                        destinationFilename = fd.open();
                        if (destinationFilename == null) return null;
                        file = new File(destinationFilename);
                        if (file.exists()) {
                            again = !DialogUtils.openQuestionMessageDialog(EditorAPI.getShell(), Messages.SendFileAction_dialog_file_exists_title, Messages.SendFileAction_dialog_file_exists_message);
                        }
                    } while (again);
                    return destinationFilename;
                }
            };
            monitor.setWorkRemaining(1);
            String filename = null;
            try {
                filename = Utils.runSWTSync(getFile);
            } catch (Exception e) {
                log.error("Unexpected error: ", e);
            } finally {
                monitor.worked(1);
                monitor.done();
            }
            if (filename == null) throw new SarosCancellationException();
            return new File(filename);
        }
    }

    public static class SendFileStreamService extends StreamService {

        protected SendFileAction sendFileAction;

        @Override
        public int[] getChunkSize() {
            return new int[] { 1024 * 1024 };
        }

        @Override
        public String getServiceName() {
            return "SendFile";
        }

        @Override
        public int getStreamsPerSession() {
            return 1;
        }

        @Override
        public long[] getMaximumDelay() {
            return new long[] { 500 };
        }

        @Override
        public boolean sessionRequest(final User from, final Object file) {
            if (sendFileAction == null) return false;
            log.info(from + " wants to send us a file.");
            final FileDescription description;
            if (file instanceof FileDescription) {
                description = (FileDescription) file;
            } else {
                log.error("Other party send no FileDescription!");
                return false;
            }
            Callable<Boolean> askUser = new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return DialogUtils.openQuestionMessageDialog(EditorAPI.getShell(), Messages.SendFileAction_dialog_incoming_file_transfer_title, MessageFormat.format(Messages.SendFileAction_dialog_incoming_file_transfer_message, description.name, Utils.formatByte(description.size), from));
                }
            };
            try {
                return Utils.runSWTSync(askUser);
            } catch (Exception e) {
                log.error("Unexpected exception: ", e);
                return false;
            }
        }

        @Override
        public void startSession(final StreamSession newSession) {
            if (sendFileAction == null) {
                newSession.stopSession();
                return;
            }
            log.info("Starting FileTransferSession from " + newSession.getRemoteJID());
            new ReceiveFileJob(newSession).schedule();
        }

        protected void hookAction(SendFileAction sendFileAction) {
            this.sendFileAction = sendFileAction;
        }
    }

    static class FileDescription implements Serializable {

        private static final long serialVersionUID = -1385642437497697528L;

        String name;

        long size;

        protected static FileDescription fromFile(File file) {
            FileDescription self = new FileDescription();
            self.name = file.getName();
            self.size = file.length();
            return self;
        }
    }

    public void dispose() {
        SelectionUtils.getSelectionService().removeSelectionListener(selectionListener);
    }
}
