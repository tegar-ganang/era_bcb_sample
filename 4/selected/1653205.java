package org.jactr.eclipse.remote.client.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ecf.core.ContainerFactory;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.datashare.IChannel;
import org.eclipse.ecf.datashare.IChannelContainerAdapter;
import org.eclipse.ecf.datashare.IChannelListener;
import org.eclipse.ecf.datashare.events.IChannelConnectEvent;
import org.eclipse.ecf.datashare.events.IChannelDisconnectEvent;
import org.eclipse.ecf.datashare.events.IChannelEvent;
import org.eclipse.ecf.datashare.events.IChannelMessageEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.jactr.eclipse.remote.client.RemoteIterativeService;
import org.jactr.eclipse.remote.client.file.FileFetch;
import org.jactr.eclipse.remote.client.file.FileFetch.IFileFetchListener;
import org.jactr.eclipse.remote.client.internal.Activator;
import org.jactr.eclipse.remote.client.jobs.ExtractJob;
import org.jactr.eclipse.remote.message.CompletionMessage;
import org.jactr.eclipse.remote.message.ControlRequest;
import org.jactr.eclipse.remote.message.HelloMessage;
import org.jactr.eclipse.remote.message.IMessage;
import org.jactr.eclipse.remote.message.IResponse;
import org.jactr.eclipse.remote.message.MessageManager;
import org.jactr.eclipse.remote.message.RunRequest;
import org.jactr.eclipse.remote.message.RunRequestResponse;
import org.jactr.eclipse.remote.message.ServiceInfoMessage;
import org.jactr.eclipse.remote.message.StateUpdate;
import org.jactr.eclipse.remote.state.IIterativeRunState;
import org.jactr.eclipse.remote.state.IIterativeRunState.State;

public class ServiceConnection {

    /**
   * Logger definition
   */
    private static final transient Log LOGGER = LogFactory.getLog(ServiceConnection.class);

    private ServiceDescriptor _descriptor;

    private IContainer _clientContainer;

    private IChannelContainerAdapter _channelAdapter;

    private IChannel _channel;

    private IChannelListener _channelListener;

    private boolean _isShortTermConnection;

    private Map<IMessage, FutureResponse> _pendingRequests;

    private Map<IMessage, IterativeRunState> _pendingStates;

    public ServiceConnection(ServiceDescriptor descriptor) {
        _descriptor = descriptor;
        _pendingRequests = new HashMap<IMessage, FutureResponse>();
        _pendingStates = new HashMap<IMessage, IterativeRunState>();
        _channelListener = new IChannelListener() {

            public void handleChannelEvent(IChannelEvent event) {
                synchronized (ServiceConnection.this) {
                    if (event instanceof IChannelMessageEvent) try {
                        IChannelMessageEvent msg = (IChannelMessageEvent) event;
                        IMessage message = MessageManager.toMessage(msg);
                        ID serviceID = _descriptor.getServiceID();
                        ID from = MessageManager.toCanonical(msg.getFromContainerID());
                        if (serviceID == null || from.equals(serviceID)) handleMessage(message); else {
                            Activator.warn("Message was received from unexpected source :" + msg.getFromContainerID(), null);
                            if (LOGGER.isWarnEnabled()) LOGGER.warn("Message was received from someone else? expected: " + serviceID + " received " + from + "/" + msg.getFromContainerID());
                        }
                    } catch (Exception e) {
                        Activator.error("Failed to process message ", e);
                        LOGGER.error("Exception while retrieving message ", e);
                    } else if (event instanceof IChannelConnectEvent) {
                        IChannelConnectEvent connect = (IChannelConnectEvent) event;
                        if (LOGGER.isDebugEnabled()) LOGGER.debug("Connection on " + connect.getChannelID() + " from " + connect.getTargetID());
                    } else if (event instanceof IChannelDisconnectEvent) disconnected((IChannelDisconnectEvent) event);
                }
            }
        };
    }

    private void disconnected(IChannelDisconnectEvent event) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Disconnected " + event);
        ID canonical = MessageManager.clearAssigned(event.getTargetID());
        if (!_descriptor.getServiceID().equals(canonical)) return;
        Activator.log("Disconnected from " + canonical, null);
        _descriptor.setConnection(null);
    }

    void connect(boolean isShortTerm) {
        _isShortTermConnection = isShortTerm;
        try {
            ID self = Activator.getDefault().getService().getID();
            _clientContainer = ContainerFactory.getDefault().createContainer("ecf.generic.client", self);
            _clientContainer.connect(_descriptor.getServerID(), null);
            _channelAdapter = (IChannelContainerAdapter) _clientContainer.getAdapter(IChannelContainerAdapter.class);
            if (_channelAdapter == null) throw new IllegalStateException("Could not acquire channel adapter");
            ID channelID = IDFactory.getDefault().createStringID("masterChannel");
            _channel = _channelAdapter.createChannel(channelID, _channelListener, null);
            _descriptor.setConnection(this);
        } catch (Exception e) {
            Activator.error("Failed to connect ", e);
            disconnect();
            throw new IllegalStateException("Could not connect to service ", e);
        }
    }

    void disconnect() {
        _descriptor.setConnection(null);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Disconnecting");
        if (_channel != null) {
            _channelAdapter.removeChannel(_channel.getID());
            _channelAdapter = null;
            _channel = null;
        }
        if (_clientContainer != null) {
            _clientContainer.disconnect();
            _clientContainer.dispose();
            _clientContainer = null;
        }
    }

    private FutureResponse allocateResponse(IMessage request) {
        FutureResponse response = new FutureResponse();
        _pendingRequests.put(request, response);
        return response;
    }

    private IMessage getRequest(IResponse response) {
        for (IMessage tmp : _pendingRequests.keySet()) if (tmp.getMessageId() == response.getOriginalMessageId()) return tmp;
        if (LOGGER.isWarnEnabled()) LOGGER.warn("Could not find a matching request for response ");
        return null;
    }

    private FutureResponse clearResponse(IMessage request) {
        return _pendingRequests.remove(request);
    }

    public synchronized void request(ControlRequest request) {
        try {
            _channel.sendMessage(MessageManager.toAssigned(_descriptor.getServiceID()), MessageManager.toBytes(request));
        } catch (Exception e) {
            LOGGER.error("Failed to request control change ", e);
            Activator.error("Failed to send control message ", e);
        }
    }

    public synchronized Future<IResponse> request(RunRequest request) {
        RemoteIterativeService service = Activator.getDefault().getService();
        IterativeRunState runState = new IterativeRunState(request.getProjectName(), request.getRunConfigName(), service.getID(), _descriptor);
        try {
            FutureResponse response = allocateResponse(request);
            _pendingStates.put(request, runState);
            _channel.sendMessage(MessageManager.toAssigned(_descriptor.getServiceID()), MessageManager.toBytes(request));
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Sent request");
            return response;
        } catch (Exception e) {
            abortRequest(request);
            Activator.error("Could not send to service " + _descriptor.getServiceID(), e);
            throw new RuntimeException("Could not send to service ", e);
        }
    }

    public void abortRequest(RunRequest request) {
        FutureResponse response = clearResponse(request);
        IterativeRunState runState = _pendingStates.remove(request);
        if (runState != null) _descriptor.removeIterativeRunState(runState);
        if (response != null) response.cancel(true);
    }

    private void handleMessage(IMessage message) {
        if (message instanceof ServiceInfoMessage) handleServiceInfoMessage((ServiceInfoMessage) message); else if (message instanceof RunRequestResponse) handleRequestResponse((RunRequestResponse) message); else if (message instanceof StateUpdate) handleStateUpdate((StateUpdate) message);
    }

    /**
   * @param bm
   */
    private void handleServiceInfoMessage(ServiceInfoMessage bm) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Updating with descriptor info");
        _descriptor.update(bm);
        try {
            _channel.sendMessage(MessageManager.toAssigned(bm.getSender()), MessageManager.toBytes(new HelloMessage(Activator.getDefault().getService().getID())));
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Sent hello");
        } catch (Exception e) {
            LOGGER.error("RemoteIterativeServiceConnection.handleBundlesMessage threw Exception : ", e);
            Activator.error("Failed to send hello message ", e);
        }
        if (_isShortTermConnection) disconnect();
    }

    private void handleStateUpdate(StateUpdate update) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Updating with run info");
        Activator.getDefault().getService();
        final IIterativeRunState runState = _descriptor.get(update.getRunID());
        if (runState == null) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("No iterative run found for " + update.getRunID());
            return;
        }
        final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(runState.getProjectName());
        if (project == null) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("No iterative run found for " + update.getRunID());
            return;
        }
        ((IterativeRunState) runState).update(update);
        _descriptor.update(runState);
        if (update instanceof CompletionMessage) {
            final CompletionMessage terminal = (CompletionMessage) update;
            if (terminal.getResultURL() != null) {
                final File dir = project.getFolder("runs").getLocation().toFile();
                dir.mkdirs();
                File destination = null;
                try {
                    destination = File.createTempFile("run-", ".zip", dir);
                } catch (IOException e) {
                    destination = new File(dir, "tmp.zip");
                }
                FileFetch.retrieve(terminal.getResultURL(), destination, new IFileFetchListener() {

                    public void fetchCompleted(URL fileToFetch, File destination) {
                        ((IterativeRunState) runState).update(terminal);
                        _descriptor.update(runState);
                        ExtractJob eJob = new ExtractJob(runState, new Path("runs"));
                        eJob.addJobChangeListener(new JobChangeAdapter() {

                            @Override
                            public void done(IJobChangeEvent event) {
                                final IPath root = ((ExtractJob) event.getJob()).getRoot();
                                ((IterativeRunState) runState).setResourcePath(root);
                                announce(runState, root, terminal.getException());
                            }
                        });
                        eJob.schedule();
                    }

                    public void fetchException(URL fileToFetch, File destination, Throwable e) {
                        destination.delete();
                        Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to receive " + fileToFetch, e));
                    }

                    public void fetchStarted(URL fileToFetch, File destination) {
                        ((IterativeRunState) runState).setState(State.TRANS_RESULTS);
                        ((IterativeRunState) runState).setResourcePath(new Path(destination.getAbsolutePath()));
                        _descriptor.update(runState);
                    }
                });
            } else announce(runState, null, terminal.getException());
        }
    }

    private void announce(final IIterativeRunState runState, final IPath root, final Throwable thrown) {
        _descriptor.update(runState);
        _descriptor.removeIterativeRunState(runState);
        RemoteIterativeService ris = Activator.getDefault().getService();
        ris.getCompletedContainer().add(runState);
        if (ris.shouldAnnounceCompletion()) Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                if (thrown != null) MessageDialog.openError(Display.getDefault().getActiveShell(), "Remote run ", runState.getProjectName() + "." + runState.getConfigurationName() + " failed to run because " + thrown.getMessage() + ". Results may be available under " + root.toString()); else if (runState.getState().equals(State.COMPLETED)) MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Remote run ", runState.getProjectName() + "." + runState.getConfigurationName() + " has completed. Results are available under " + root.toString()); else if (root != null) MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Remote run ", runState.getProjectName() + "." + runState.getConfigurationName() + " has aborted. Results are available under " + root.toString()); else MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Remote run ", runState.getProjectName() + "." + runState.getConfigurationName() + " has aborted. No results available.");
            }
        });
    }

    /**
   * @param requestResponse
   */
    private void handleRequestResponse(RunRequestResponse requestResponse) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Got response");
        RunRequest request = (RunRequest) getRequest(requestResponse);
        FutureResponse response = clearResponse(request);
        IterativeRunState runState = _pendingStates.remove(request);
        response.setResponse(requestResponse);
        if (!requestResponse.wasAccepted()) _descriptor.removeIterativeRunState(runState); else {
            runState.setRunID(requestResponse.getRunID());
            runState.setState(State.QUEUED);
            _descriptor.addIterativeRunState(runState);
        }
    }

    private class FutureResponse extends FutureTask<IResponse> {

        public FutureResponse() {
            super(new Callable<IResponse>() {

                public IResponse call() throws Exception {
                    return null;
                }
            });
        }

        public void setResponse(IResponse response) {
            set(response);
        }
    }
}
