package org.soda.dpws.exchange;

import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.transport.Channel;
import org.soda.dpws.transport.Transport;
import org.soda.dpws.transport.TransportManager;
import org.soda.dpws.transport.dead.DeadLetterTransport;
import org.soda.dpws.wsdl.OperationInfo;

/**
 * A MessageExchange encapsulates the orchestration of a message exchange
 * pattern. This makes it easy to handle various interactions - like robust
 * in-out, robust in, in, out, WS-Addressing MEPs, etc.
 */
public class MessageExchange {

    private OperationInfo operation;

    private DPWSContextImpl context;

    private InMessage inMessage;

    private OutMessage outMessage;

    private AbstractMessage faultMessage;

    private boolean hasOutput = false;

    private boolean hasInput = true;

    private boolean hasFault = true;

    /**
   * @param context
   */
    public MessageExchange(DPWSContextImpl context) {
        this.context = context;
        if (context.getExchange() != null) {
            setInMessage(context.getExchange().getInMessage());
        }
        context.setExchange(this);
    }

    /**
   * @return {@link DPWSContextImpl}
   */
    public DPWSContextImpl getContext() {
        return context;
    }

    /**
   * @return the {@link OperationInfo}
   */
    public OperationInfo getOperation() {
        return operation;
    }

    /**
   * @param operation
   */
    public void setOperation(OperationInfo operation) {
        this.operation = operation;
        hasOutput = operation.hasOutput();
        hasInput = operation.hasInput();
    }

    /**
   * @return the {@link InMessage}
   * @throws UnsupportedOperationException
   */
    public InMessage getInMessage() throws UnsupportedOperationException {
        return inMessage;
    }

    /**
   * @return the {@link OutMessage}
   */
    public OutMessage getOutMessage() {
        if (outMessage == null && hasOutMessage()) {
            outMessage = new OutMessage(Channel.BACKCHANNEL_URI);
            outMessage.setChannel(getOutChannel());
            outMessage.setSoapVersion(getInMessage().getSoapVersion());
            setOutMessage(outMessage);
        }
        return outMessage;
    }

    /**
   * @return the {@link AbstractMessage}
   * @throws UnsupportedOperationException
   */
    public AbstractMessage getFaultMessage() throws UnsupportedOperationException {
        if (faultMessage == null) {
            faultMessage = new OutMessage(Channel.BACKCHANNEL_URI);
            faultMessage.setChannel(getFaultChannel());
            faultMessage.setSoapVersion(getInMessage().getSoapVersion());
            setFaultMessage(faultMessage);
        }
        return faultMessage;
    }

    /**
   * @param faultMessage
   */
    public void setFaultMessage(AbstractMessage faultMessage) {
        this.faultMessage = faultMessage;
    }

    /**
   * @param inMessage
   */
    public void setInMessage(InMessage inMessage) {
        this.inMessage = inMessage;
    }

    /**
   * @param outMessage
   */
    public void setOutMessage(OutMessage outMessage) {
        this.outMessage = outMessage;
    }

    /**
   * @return has it fault message
   */
    public boolean hasFaultMessage() {
        return hasFault;
    }

    /**
   * @return has it in message
   */
    public boolean hasInMessage() {
        return hasInput;
    }

    /**
   * @return has it out message
   */
    public boolean hasOutMessage() {
        return hasOutput;
    }

    /**
   * @return the in channel
   */
    public Channel getInChannel() {
        if (hasInMessage()) {
            return getInMessage().getChannel();
        }
        return getDeadLetterChannel();
    }

    /**
   * @return the out channel
   */
    public Channel getOutChannel() {
        if (hasOutMessage()) {
            return getInMessage().getChannel();
        }
        return getDeadLetterChannel();
    }

    /**
   * @return the fault channel
   */
    public Channel getFaultChannel() {
        if (hasFaultMessage()) {
            return getInMessage().getChannel();
        }
        return getDeadLetterChannel();
    }

    /**
   * @return the dead letter channel
   */
    public Channel getDeadLetterChannel() {
        TransportManager tm = getContext().getDpws().getTransportManager();
        Transport transport = tm.getTransport(DeadLetterTransport.NAME);
        try {
            return transport.createChannel();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
