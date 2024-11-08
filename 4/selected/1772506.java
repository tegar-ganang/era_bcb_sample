package ch.usi.jpat.da.proj;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import ch.usi.jpat.da.proj.Proposal;

@ChannelPipelineCoverage("one")
public class AcceptorHandler extends AbstractPaxosHandler {

    static Logger logger = Logger.getLogger("ch.usi.jpat.da.proj");

    public AcceptorHandler(ProcessContext pCtx) {
        super(pCtx);
    }

    public void broadcastToLearners(Proposal p, MessageEvent e) {
        for (PaxosProcess l : pCtx.getLearners().values()) {
            logger.info("Sending ballot to learner: " + l.getIpAddr() + ":" + l.getPort());
            e.getChannel().write(PaxosUtils.createStringFromProposal(p), l.getInetSocketAddress());
        }
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        String incoming = (String) e.getMessage();
        Proposal propIn = PaxosUtils.createProposalFromString(incoming);
        Proposal propResp = new Proposal();
        propResp.setSrcId(pCtx.getSrcId());
        propResp.setBallot(propIn.getBallot());
        if (propIn.getBallot() > pCtx.getBallot()) {
            logger.info("Got ballot " + propIn.getBallot());
            pCtx.setBallot(propIn.getBallot());
            e.getChannel().write(PaxosUtils.createStringFromProposal(propResp), e.getRemoteAddress());
            pCtx.setValue(null);
        } else if (propIn.getBallot() == pCtx.getBallot() && pCtx.getValue() == null) {
            logger.info("Learned value : " + propIn.getValue());
            pCtx.setValue(propIn.getValue());
            propResp.setValue(propIn.getValue());
            pCtx.getProposalHistory().put(propIn.getBallot(), propIn);
            propIn.setSrcId(pCtx.getSrcId());
            broadcastToLearners(propIn, e);
            e.getChannel().write(PaxosUtils.createStringFromProposal(propResp), e.getRemoteAddress());
        } else {
            logger.info("Ignoring received ballotId " + propIn.getBallot());
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        Channel ch = e.getChannel();
        ch.close();
    }
}
