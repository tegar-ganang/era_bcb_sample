package org.jboss.netty.example.qotm;

import java.util.Random;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @version $Rev: 2121 $, $Date: 2010-02-02 09:38:07 +0900 (Tue, 02 Feb 2010) $
 */
public class QuoteOfTheMomentServerHandler extends SimpleChannelUpstreamHandler {

    private static final Random random = new Random();

    private static final String[] quotes = new String[] { "Where there is love there is life.", "First they ignore you, then they laugh at you, then they fight you, then you win.", "Be the change you want to see in the world.", "The weak can never forgive. Forgiveness is the attribute of the strong." };

    private static String nextQuote() {
        int quoteId;
        synchronized (random) {
            quoteId = random.nextInt(quotes.length);
        }
        return quotes[quoteId];
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        String msg = (String) e.getMessage();
        if (msg.equals("QOTM?")) {
            e.getChannel().write("QOTM: " + nextQuote(), e.getRemoteAddress());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
    }
}
