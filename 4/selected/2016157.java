package de.tud.kom.nat.im.model.files;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import de.tud.kom.nat.comm.ICommFacade;
import de.tud.kom.nat.comm.IMessageHandler;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IMessage;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.comm.msg.Peer;
import de.tud.kom.nat.im.model.IChatModel;
import de.tud.kom.nat.im.model.files.FileTransfer.State;
import de.tud.kom.nat.im.model.files.msg.FileCompleteMessage;
import de.tud.kom.nat.im.model.files.msg.FileDataMessage;
import de.tud.kom.nat.im.model.files.msg.SendFileAnswer;
import de.tud.kom.nat.im.model.files.msg.SendFileRequest;
import de.tud.kom.nat.util.Logger;

/**
 * This class manages the filetransfers. It opens a server socket on a specified port,
 * receives all kind of file-related messages and creates the <tt>FileTransfer</tt>s.
 *
 * @author Matthias Weinert
 */
public class FileTransferManager implements IMessageHandler {

    /** The <tt>ICommFacade</tt> which is used for communication. */
    private ICommFacade commFacade;

    /** All current filetransfers. */
    private Map<UUID, FileTransfer> fileTransfers = Collections.synchronizedMap(new HashMap<UUID, FileTransfer>());

    /** The TCP port. */
    private final int port;

    /** The chat model. */
    private IChatModel chatModel;

    /** Thats me. */
    private IPeer myself;

    /**
	 * Creates a <tt>FileTransferManager</tt>.
	 * @param commFacade the comm facade
	 * @param port the tcp port
	 * @param chatModel the chat model
	 * @throws BindException
	 */
    public FileTransferManager(ICommFacade commFacade, int port, IChatModel chatModel) throws BindException {
        this.port = port;
        this.commFacade = commFacade;
        this.chatModel = chatModel;
        myself = chatModel.getMyself().getPeer();
        openServerSocket();
        registerTypes();
    }

    /**
	 * Here, the server socket is opened on the given port.
	 * 
	 * @throws BindException
	 */
    private void openServerSocket() throws BindException {
        try {
            commFacade.openTCPServerSocket(port);
        } catch (BindException e) {
            throw e;
        } catch (IOException e) {
            Logger.logError(e, "Could not open tcp socket for file transfers!");
            throw new IllegalStateException("Unable to initialize file transfer daemon.");
        }
    }

    /**
	 * This method registers all message types related to the filetransfers.
	 */
    private void registerTypes() {
        commFacade.getMessageProcessor().registerMessageHandler(SendFileAnswer.class, this);
        commFacade.getMessageProcessor().registerMessageHandler(SendFileRequest.class, this);
        commFacade.getMessageProcessor().registerMessageHandler(FileDataMessage.class, this);
        commFacade.getMessageProcessor().registerMessageHandler(FileCompleteMessage.class, this);
    }

    /**
	 * This method initiates the sending of a file to another peer by using the given
	 * <tt>SocketChannel</tt>.
	 * @param file file we want to send
	 * @param sc socketchannel
	 */
    public void sendFile(File file, SocketChannel sc) {
        Peer p = new Peer(null, (InetSocketAddress) sc.socket().getRemoteSocketAddress());
        try {
            SendFileRequest sfq = new SendFileRequest(chatModel.getMyself().getPeerID(), null, chatModel.getMyself().getNickname(), file);
            FileTransfer ft = addPendingFiletransfer(sfq.getFiletransferID(), p, file);
            chatModel.onStartedFileTransfer(ft);
            ft.setState(FileTransfer.State.REQUESTED);
            commFacade.sendTCPMessage(sc, sfq);
        } catch (IOException e) {
            Logger.logError(e, "Could not initiate the file transfer!");
        }
    }

    /**
	 * This method initiates the sending of a file to another peer.
	 * 
	 * @param peer target
	 * @param file selected file
	 */
    public void sendFile(IPeer peer, File file) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open(peer.getAddress());
        } catch (IOException e1) {
            Logger.logWarning("Could not establish direct TCP connection to " + peer + ", trying to get one from the broker..");
        }
        if (sc == null || !sc.isOpen() || !sc.isConnected()) {
            sc = chatModel.getConnectionBroker().requestTCPChannel(peer);
        }
        if (sc == null) {
            Logger.log("Could not establish TCP connection to peer: " + peer);
            return;
        }
        try {
            if (sc != null && sc.isBlocking()) sc.configureBlocking(false);
        } catch (IOException e1) {
        }
        commFacade.getChannelManager().registerChannel(sc);
        sendFile(file, sc);
    }

    public void sendFile(IPeer peer, File file, InetSocketAddress relayAddress) {
        SocketChannel sc = chatModel.getConnectionBroker().requestRelayedTCPChannel(peer, relayAddress);
        if (sc == null) {
            Logger.log("Could not establish relayed TCP connection to peer: " + peer);
            return;
        }
        try {
            sc.configureBlocking(false);
        } catch (IOException e) {
        }
        commFacade.getChannelManager().registerChannel(sc);
        sendFile(file, sc);
    }

    public void onMessageReceived(IEnvelope msg) {
        if (msg.getMessage() instanceof SendFileRequest) {
            handleFileRequest(msg);
        } else if (msg.getMessage() instanceof SendFileAnswer) {
            handleFileAnswer(msg);
        } else if (msg.getMessage() instanceof FileDataMessage) {
            handleFileDataMessage(msg);
        } else if (msg.getMessage() instanceof FileCompleteMessage) {
            handleFileCompleteMessage(msg);
        }
    }

    /**
	 * Returns true when a transfer with the given peer and state exists. If state is
	 * <tt>null</tt>, all states are valid and return true [if transfer with peer exists].
	 * 
	 * @param peer
	 * @param state
	 * @return
	 */
    private FileTransfer getTransferInfo(UUID filetransferID, State state, boolean incoming) {
        FileTransfer ft = fileTransfers.get(filetransferID);
        if (state != null && !ft.getState().equals(state)) return null;
        if (ft.isIncoming() != incoming) return null;
        return ft;
    }

    /**
	 * This method creates an incoming filetransfer and adds it to the list of current transfers.
	 * @param peer the remote peer
	 * @param peerNick the nick of the remote peer
	 * @param filename the filename
	 * @param size the size of the file
	 * @param saveTo the file where to save the received data to
	 * @return the filetransfer object
	 */
    private FileTransfer addIncomingFiletransfer(UUID filetransferID, IPeer peer, String peerNick, String filename, long size, File saveTo) {
        FileTransfer ft = new IncomingFileTransfer(filetransferID, peer, peerNick, filename, size, saveTo);
        fileTransfers.put(filetransferID, ft);
        return ft;
    }

    /**
	 * This method creates an outgoing filetransfer to a given peer and returns it.
	 * 
	 * @param peer remote peer
	 * @param f file to send
	 * @return the created <tt>FileTransfer</tt> object
	 */
    private FileTransfer addPendingFiletransfer(UUID filetransferID, Peer peer, File f) {
        FileTransfer ft = new OutgoingFileTransfer(filetransferID, myself, peer, peer.getAddress().toString(), f, commFacade);
        fileTransfers.put(filetransferID, ft);
        return ft;
    }

    /**
	 * Another client asks you whether he can send you a file. Now is decided whether you
	 * want to receive it.
	 * 
	 * @param msg the incoming envelope
	 */
    private void handleFileRequest(IEnvelope request) {
        SendFileRequest sfr = (SendFileRequest) request.getMessage();
        File saveTo = null;
        IPeer from = request.getSender();
        do {
            saveTo = chatModel.onSendFileRequest(from, sfr.getNickname(), sfr.getFilename(), sfr.getSize());
        } while (saveTo != null && !isValidSaveTarget(saveTo));
        SendFileAnswer sfa = new SendFileAnswer(myself.getPeerID(), sfr.getSenderPeerID(), sfr.getFiletransferID(), saveTo != null, chatModel.getMyself().getNickname());
        try {
            if (saveTo != null) {
                FileTransfer ft = addIncomingFiletransfer(sfr.getFiletransferID(), from, sfr.getNickname(), sfr.getFilename(), sfr.getSize(), saveTo);
                chatModel.onStartedFileTransfer(ft);
                ft.setState(State.SENDING);
            }
            commFacade.sendTCPMessage((SocketChannel) request.getChannel(), sfa);
        } catch (IOException e) {
            Logger.logError(e, "Error sending answer to file offer!");
        }
    }

    /**
	 * This function returns true if the given file is a valid file to save
	 * receiving data to.
	 * 
	 * @param saveTo local target file
	 * @return true if, and only if, the file is a valid target file
	 */
    private boolean isValidSaveTarget(File saveTo) {
        if (saveTo.isDirectory()) return false;
        if (saveTo.exists() && saveTo.canWrite()) return true;
        try {
            if (saveTo.createNewFile()) return true;
        } catch (IOException e) {
        }
        return false;
    }

    /**
	 * You have sent a request to a client to send him a file. This is his answer.
	 * 
	 * @param msg the incoming envelope
	 */
    private void handleFileAnswer(IEnvelope msg) {
        SendFileAnswer answer = (SendFileAnswer) msg.getMessage();
        FileTransfer info = getTransferInfo(answer.getFiletransferID(), State.REQUESTED, false);
        SendFileAnswer sfa = (SendFileAnswer) msg.getMessage();
        if (info == null) {
            Logger.logError(new IllegalStateException(msg.getSender() + " acknowlegded a file receival which we did not initiate!"), "Error in file transfer! Closing connection...");
            try {
                msg.getChannel().close();
            } catch (IOException e) {
                Logger.logError(e, "Error closing the connection to the peer which violated protocol: " + msg.getSender() + ". Ignoring...");
            }
            return;
        }
        info.setPeerNick(sfa.getNickname());
        if (sfa.canSend()) {
            info.setState(State.SENDING);
            internalSendFile((OutgoingFileTransfer) info, (SocketChannel) msg.getChannel());
        } else {
            info.setState(State.DENIED);
            try {
                msg.getChannel().close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * This sends the data to the channel.
	 * 
	 * @param info the info about the file transfer
	 * @param channel the socket channel to use
	 */
    private void internalSendFile(OutgoingFileTransfer info, SocketChannel channel) {
        info.startFileSending(channel);
    }

    /**
	 * There are incoming data of the file, the transfer is already active.
	 * @param msg the envelope which came in
	 */
    private void handleFileDataMessage(IEnvelope env) {
        FileDataMessage fdm = (FileDataMessage) env.getMessage();
        IPeer from = env.getSender();
        IncomingFileTransfer ft = (IncomingFileTransfer) getTransferInfo(fdm.getFiletransferID(), State.SENDING, true);
        if (ft == null) {
            Logger.logError(new IllegalStateException("Illegal message: Peer " + env.getSender() + " just tries to send us a file! Closing connection..."), "Message not valid due to protocol!");
            try {
                env.getChannel().close();
            } catch (IOException e) {
            }
        }
        boolean sendCompleteMessage = ft.handleFileDataMessage(fdm);
        if (sendCompleteMessage) {
            try {
                IMessage msg = new FileCompleteMessage(myself.getPeerID(), from.getPeerID(), fdm.getFiletransferID());
                commFacade.sendTCPMessage((SocketChannel) env.getChannel(), msg);
                ft.setState(State.FINISHED);
            } catch (IOException e) {
            }
        }
    }

    /**
	 * When a <tt>FileCompleteMessage</tt> is sent from a guy we just sent a file, this indicates that he got
	 * all data. The channel can be closed and the transfer can be marked as complete.
	 * 
	 * @param env the envelope of the <tt>FileCompleteMessage</tt>
	 */
    private void handleFileCompleteMessage(IEnvelope env) {
        FileCompleteMessage msg = (FileCompleteMessage) env.getMessage();
        OutgoingFileTransfer ft = (OutgoingFileTransfer) this.getTransferInfo(msg.getFiletransferID(), State.SENDING, false);
        ft.setTransferred(ft.getSize());
        ft.receiveCompleteMessage();
        try {
            env.getChannel().close();
        } catch (IOException e) {
        }
    }
}
