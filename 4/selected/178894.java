package net.sourceforge.insim4j.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.sourceforge.insim4j.client.interfaces.IClient;
import net.sourceforge.insim4j.client.interfaces.IResponseHandler;
import net.sourceforge.insim4j.i18n.ExceptionMessages;
import net.sourceforge.insim4j.i18n.LogMessages;
import net.sourceforge.insim4j.insim.interfaces.IInSimRequestPacket;
import net.sourceforge.insim4j.utils.FormatUtils;
import net.sourceforge.insim4j.utils.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractInSimClient implements IClient {

    /**
   * Log for this class. NOT static. For more details see
   * http://commons.apache.org/logging/guide.html#Obtaining%20a%20Log%20Object
   */
    private final Log log = LogFactory.getLog(AbstractInSimClient.class);

    protected static final FormatUtils FORMAT_UTILS = FormatUtils.getInstance();

    protected static final StringUtils STRING_UTILS = StringUtils.getInstance();

    private boolean fStop = false;

    protected ByteBuffer fReadBuffer = ByteBuffer.allocate(8192);

    private Selector fSelector;

    protected List<ChangeRequest> fChangeRequests = new LinkedList<ChangeRequest>();

    protected Map<SelectableChannel, List<ByteBuffer>> fPendingRequestData = new HashMap<SelectableChannel, List<ByteBuffer>>();

    private Map<SelectableChannel, ChannelInfo> fChannelInfo = Collections.synchronizedMap(new HashMap<SelectableChannel, ChannelInfo>());

    private Map<InSimHost, SelectableChannel> fChannels = Collections.synchronizedMap(new HashMap<InSimHost, SelectableChannel>());

    public AbstractInSimClient() throws IOException {
        fSelector = this.initSelector();
    }

    public InSimHost connect(final IResponseHandler pHandler, final int pLocalPort, final InSimHost pHost) throws IOException {
        final InSimHost host = new InSimHost(pHost);
        final SelectableChannel channel = this.initiateConnection(pLocalPort, host);
        fChannels.put(host, channel);
        final Thread handlerThread = new Thread(pHandler, "Response Handler Thread (" + host + ")");
        handlerThread.setDaemon(true);
        handlerThread.start();
        fChannelInfo.put(channel, new ChannelInfo(host, pHandler));
        return host;
    }

    public void disconnect(final InSimHost pHost) throws IOException {
        final SelectableChannel channel = fChannels.remove(pHost);
        if (channel != null) {
            synchronized (fChangeRequests) {
                fChangeRequests.add(new ChangeRequest(channel, ChangeRequest.CANCEL, 0));
            }
        }
        final ChannelInfo info = fChannelInfo.remove(channel);
        final IResponseHandler handler = info.getResponseHandler();
        if (handler != null) {
            handler.setStop(true);
        }
        fSelector.wakeup();
        log.info(FORMAT_UTILS.format(LogMessages.getString("Client.disconnected"), pHost));
    }

    public void send(final InSimHost pHost, final IInSimRequestPacket pRequestPacket) throws IOException {
        final SelectableChannel channel = fChannels.get(pHost);
        synchronized (fPendingRequestData) {
            List<ByteBuffer> queue = fPendingRequestData.get(channel);
            if (queue == null) {
                queue = new ArrayList<ByteBuffer>();
                fPendingRequestData.put(channel, queue);
            }
            final ByteBuffer buf = pRequestPacket.compile();
            buf.rewind();
            queue.add(buf);
        }
        synchronized (fChangeRequests) {
            fChangeRequests.add(new ChangeRequest(channel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
        }
        log.debug(FORMAT_UTILS.format(LogMessages.getString("Client.send.packetQueued"), pHost, pRequestPacket));
        fSelector.wakeup();
    }

    public synchronized boolean isStop() {
        return fStop;
    }

    public synchronized void setStop(final boolean pStop) {
        fStop = pStop;
        fSelector.wakeup();
    }

    public void run() {
        while (!isStop()) {
            try {
                synchronized (fChangeRequests) {
                    final Iterator<ChangeRequest> changes = fChangeRequests.iterator();
                    while (changes.hasNext()) {
                        final ChangeRequest change = changes.next();
                        switch(change.getType()) {
                            case ChangeRequest.CHANGEOPS:
                                final SelectionKey key = change.getChannel().keyFor(fSelector);
                                if (key != null) {
                                    key.interestOps(change.getOps());
                                } else {
                                    log.debug(FORMAT_UTILS.format(LogMessages.getString("Client.channel.nullKey"), change));
                                }
                                break;
                            case ChangeRequest.REGISTER:
                                change.getChannel().register(fSelector, change.getOps());
                                break;
                            case ChangeRequest.CANCEL:
                                change.getChannel().keyFor(fSelector).cancel();
                                change.getChannel().close();
                                break;
                        }
                    }
                    fChangeRequests.clear();
                }
                fSelector.select();
                final Iterator<SelectionKey> selectedKeys = fSelector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    final SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isConnectable()) {
                        this.finishConnection(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void handleResponse(final SelectableChannel pChannel, final byte[] pData, final int pNumRead) throws IOException {
        final byte[] rspData = new byte[pNumRead];
        System.arraycopy(pData, 0, rspData, 0, pNumRead);
        final ChannelInfo info = fChannelInfo.get(pChannel);
        final IResponseHandler handler = info.getResponseHandler();
        final InSimHost host = info.getHost();
        handler.handleResponse(new InSimResponse(host, rspData));
    }

    private Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    protected abstract SelectableChannel initiateConnection(final int pLocalPort, final InSimHost pHost) throws IOException;

    protected abstract void finishConnection(final SelectionKey pSelectionKey) throws IOException;

    protected abstract void read(final SelectionKey key) throws IOException;

    protected abstract void write(final SelectionKey pSelectionKey) throws IOException;

    /**
	 * Channel Information. 
	 *
	 * @author Jiří Sotona
	 *
	 */
    private class ChannelInfo {

        private InSimHost fHost;

        private IResponseHandler fResponseHandler;

        /**
		 * Constructor.
		 * 
		 * @param pHost
		 * @param pResponseHandler
		 * 
		 * @throws IllegalArgumentException
		 *                 if Host is null
		 * @throws IllegalArgumentException
		 *                 if ResponseHandler is null
		 */
        public ChannelInfo(final InSimHost pHost, final IResponseHandler pResponseHandler) {
            setHost(pHost);
            setResponseHandler(pResponseHandler);
        }

        /**
		 * Setter.
		 * 
		 * @param pHost
		 * 
		 * @throws IllegalArgumentException
		 *                 if Host is null
		 */
        private void setHost(InSimHost pHost) {
            if (pHost == null) {
                throw new IllegalArgumentException(FORMAT_UTILS.format(ExceptionMessages.getString("Object.iae.nonNull"), "Host"));
            }
            this.fHost = pHost;
        }

        /**
		 * Setter.
		 * 
		 * @param pResponseHandler
		 * @throws IllegalArgumentException
		 *                 if ResponseHandler is null
		 */
        private void setResponseHandler(final IResponseHandler pResponseHandler) {
            if (pResponseHandler == null) {
                throw new IllegalArgumentException(FORMAT_UTILS.format(ExceptionMessages.getString("Object.iae.nonNull"), "Response Handler"));
            }
            this.fResponseHandler = pResponseHandler;
        }

        /**
		 * Getter.
		 * 
		 * @return the host
		 */
        public InSimHost getHost() {
            return fHost;
        }

        /**
		 * Getter.
		 * 
		 * @return the responseHandler
		 */
        public IResponseHandler getResponseHandler() {
            return fResponseHandler;
        }
    }
}
