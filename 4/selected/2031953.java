package com.hongbo.cobweb.nmr.core;

import com.hongbo.cobweb.nmr.api.Role;
import com.hongbo.cobweb.nmr.api.internal.Flow;
import com.hongbo.cobweb.nmr.api.internal.InternalEndpoint;
import com.hongbo.cobweb.nmr.api.internal.InternalExchange;

/**
 * The StraightThrough flow is the simpliest possible flow.
 * It will just put the exchange to its destination endpoint's
 * channel by calling:
 * <code><pre>
 *     exchange.getDestination().getChannel().deliver(exchange);
 * </pre></code>
 *
 * @version $Revision: $
 * @since 4.0
 */
public class StraightThroughFlow implements Flow {

    /**
     * Check if this flow can be used to dispatch the given Exchange
     *
     * @param exchange the exchange to check
     * @return <code>true</code> if the flow can be used, <code>false</code> otherwise
     */
    public boolean canDispatch(InternalExchange exchange, InternalEndpoint endpoint) {
        return true;
    }

    /**
     * Dispatch the Exchange using this flow.
     *
     * @param exchange the exchange to dispatch
     */
    public void dispatch(InternalExchange exchange) {
        InternalEndpoint endpoint = exchange.getRole() == Role.Consumer ? exchange.getDestination() : exchange.getSource();
        endpoint.getChannel().deliver(exchange);
    }
}
