package org.soda.dpws.fault;

import org.soda.dpws.DPWSException;
import org.soda.dpws.exchange.AbstractMessage;
import org.soda.dpws.exchange.MessageExchange;
import org.soda.dpws.exchange.OutMessage;
import org.soda.dpws.handler.AbstractHandler;
import org.soda.dpws.handler.Phase;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.transport.Channel;

/**
 * Sends messages out via the out channel on the message exchange.
 */
public class FaultSender extends AbstractHandler {

    /**
   *
   */
    public FaultSender() {
        super();
        setPhase(Phase.SEND);
    }

    public void invoke(DPWSContextImpl context) throws DPWSFault {
        MessageExchange exchange = context.getExchange();
        AbstractMessage faultMessage = exchange.getFaultMessage();
        Channel faultChannel = faultMessage.getChannel();
        try {
            if (faultChannel != null) {
                OutMessage outMessage = (OutMessage) faultMessage;
                faultChannel.send(context, outMessage);
            } else System.out.println("... Current faultChannel is null ... reject fault Message!");
        } catch (DPWSException e) {
        }
    }
}
