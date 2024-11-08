package gg.arkheion.http;

import java.nio.channels.ClosedChannelException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http2.HttpChunk;
import org.jboss.netty.handler.codec.http2.HttpResponse;
import org.jboss.netty.handler.codec.http2.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;

/**
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @author <a href="http://openr66.free.fr/">Frederic Bregier</a>
 *
 * @version $Rev: 612 $, $Date: 2010-11-11 19:35:43 +0100 (jeu., 11 nov. 2010) $
 */
public class HttpResponseHandler extends SimpleChannelUpstreamHandler {

    private volatile boolean readingChunks;

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object obj = e.getMessage();
        if (!readingChunks && (obj instanceof HttpResponse)) {
            HttpResponse response = (HttpResponse) e.getMessage();
            HttpResponseStatus status = response.getStatus();
            System.out.println("STATUS: " + status);
            if (!response.getHeaderNames().isEmpty()) {
                for (String name : response.getHeaderNames()) {
                    for (String value : response.getHeaders(name)) {
                        System.out.println("HEADER: " + name + " = " + value);
                    }
                }
                System.out.println();
            }
            if (response.getStatus().getCode() == 200 && response.isChunked()) {
                readingChunks = true;
                HttpClient.ok.incrementAndGet();
                System.out.print("CHUNKED CONTENT {");
            } else if (response.getStatus().getCode() != 200) {
                HttpClient.ko.incrementAndGet();
                System.err.print("Error: ");
                ChannelBuffer content = response.getContent();
                System.err.println(content.toString(CharsetUtil.UTF_8));
                Channels.close(e.getChannel());
            } else {
                HttpClient.ok.incrementAndGet();
                System.out.print("CONTENT NOT CHUNKED: ");
                ChannelBuffer content = response.getContent();
                System.out.println(content.readableBytes());
                content.skipBytes(content.readableBytes());
                Channels.close(e.getChannel());
            }
        } else {
            readingChunks = true;
            HttpChunk chunk = (HttpChunk) e.getMessage();
            chunk.getContent().clear();
            if (chunk.isLast()) {
                readingChunks = false;
                System.out.println("} END OF CHUNKED CONTENT");
                Channels.close(e.getChannel());
            } else {
                System.out.print("o");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (e.getCause() instanceof ClosedChannelException) {
            System.err.println("Close before ending");
            return;
        }
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
