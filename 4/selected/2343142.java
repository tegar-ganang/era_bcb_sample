package org.soda.dpws.handler;

import org.soda.dpws.DPWSException;
import org.soda.dpws.exchange.OutMessage;
import org.soda.dpws.fault.DPWSFault;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.transport.Channel;

/**
 * Sends messages out via the out channel on the message exchange.
 */
public class OutMessageSender extends AbstractHandler {

    /**
   * 
   */
    public OutMessageSender() {
        super();
        setPhase(Phase.SEND);
    }

    public void invoke(DPWSContextImpl context) throws DPWSFault {
        try {
            Channel channel = context.getExchange().getOutMessage().getChannel();
            OutMessage message = context.getExchange().getOutMessage();
            channel.send(context, message);
        } catch (DPWSException e) {
            throw DPWSFault.createFault(e);
        }
    }
}
