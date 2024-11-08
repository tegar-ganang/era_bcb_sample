package org.eralyautumn.message.netty;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import net.sf.cglib.reflect.FastMethod;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yxg.common.util.Constants;
import com.yxg.common.util.SpringContainer;
import com.yxg.common.util.Util;
import com.yxg.common.util.WarnGameMessage;
import com.yxg.common.annotations.Action;
import com.yxg.common.annotations.VisitorRole;
import com.yxg.common.persistence.key.SessionKey;
import com.yxg.common.service.CommonService;
import com.yxg.main.ServerInit;
import com.yxg.message.format.GameMessage;
import com.yxg.message.session.Session;
import com.yxg.message.session.impl.NettySession;

/**
 * 收到消息后的处理
 * 
 * @author <a href="mailto:fmlou@163.com">HongzeZhang</a>
 * 
 * @version 1.0
 * 
 * @since Jun 2, 2008
 */
@Component()
public class DelimiterHandler extends SimpleChannelHandler {

    @Autowired
    private CommonService commonService;

    private static final Logger logger = Logger.getLogger(DelimiterHandler.class);

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent msg) {
        String mess = (String) msg.getMessage();
        logger.info("handler receive message=" + mess);
        if (mess.startsWith(Constants.FLASH_REQUEST)) {
            byte[] reps = null;
            try {
                reps = Constants.FLASH_ANSWER.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            ChannelBuffer wb = ChannelBuffers.directBuffer(reps.length);
            wb.writeBytes(reps);
            Channels.write(ctx, msg.getFuture(), wb);
            return;
        }
        long t = System.currentTimeMillis();
        if (mess.trim().equals("") || !mess.contains("@")) {
            logger.error("error message:" + mess);
            return;
        }
        Channel channel = ctx.getChannel();
        Session session = NettySession.getInstance(channel);
        GameMessage message = GameMessage.getInstance(channel.getId(), mess);
        if (message.getMiddleServer()) {
            this.executeMiddelMessagte(message, session);
        } else this.executeMessage(message, session);
        long t2 = (System.currentTimeMillis() - t);
        if (t2 > 500) {
            logger.info("message=" + mess);
            logger.info("execute time=" + t2);
        }
    }

    private void executeMiddelMessagte(GameMessage message, Session selfSession) {
        logger.debug("middle message=" + message);
        if (message.getReceiverKey().equals(Constants.CLOSE_SESSION)) {
            int username = message.getInt(0);
            Session session = commonService.getUserSession(username);
            if (session != null && session.isConnected()) session.close();
            return;
        }
        commonService.middleServerSendMessage(selfSession, message);
    }

    private void executeMessage(GameMessage message, Session session) {
        Object controller = SpringContainer.getBean(message.getControllerKey());
        String receiveKey = message.getReceiverKey();
        Method method = ServerInit.controller.get(receiveKey);
        if (controller == null || method == null) {
            throw new NullPointerException("No find controller, type:" + receiveKey + ".");
        } else {
            VisitorRole role = null;
            if (session.getAttribute(SessionKey.LOGIN_USER) != null || ((role = method.getAnnotation(VisitorRole.class)) != null && role.value() == Constants.ROLE_ANYONE)) {
                Action action = method.getAnnotation(Action.class);
                if (this.checkParmas(method, message, action)) {
                    try {
                        GameMessage gm = (GameMessage) method.invoke(controller, session, message);
                        if (gm == null) {
                            if (action.returnResult()) gm = GameMessage.getInstance("0|-4").append(receiveKey);
                        } else if (gm.getReceiverKey().equals("0|-3")) {
                            if (gm.length() == 1) gm = GameMessage.getInstance("0|-3").append(gm.getObject(0)).append(receiveKey); else gm = GameMessage.getInstance("0|-3").append(receiveKey);
                        }
                        session.write(gm);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    session.write(WarnGameMessage.PARAMS_ERROR_MESSAGE);
                }
            } else {
                session.write(WarnGameMessage.NO_ROLE_MESSAGE);
            }
        }
    }

    private boolean checkParmas(Method method, GameMessage message, Action action) {
        int[] params = action.params();
        if (params.length == 0) return true;
        int[] minValues = action.minValues();
        int[] maxValues = action.maxValues();
        for (int i = 0; i < params.length; i++) {
            int param = params[i];
            if (message.size() <= param) return false;
            double value = message.getDouble(param);
            if (minValues.length > 0 && value < (double) minValues[i]) return false;
            if (maxValues.length > 0 && value > (double) maxValues[i]) return false;
        }
        return true;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent event) {
        logger.debug("channel Connected...");
        NettyMessageServer.allChannelsGroup.add(event.getChannel());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        logger.debug("channelClosed.");
        Session session = NettySession.getInstance(e.getChannel());
        if (session.getAttribute(SessionKey.LOGIN_KEY) != null) {
            NettyMessageServer.allChannelsGroup.remove(e.getChannel());
            Boolean removeMem = (Boolean) session.getAttribute(SessionKey.REMOVE_MEM_INFO);
            removeMem = (removeMem == null) ? true : removeMem;
            commonService.leaveGame(session, removeMem);
            logger.debug("Remove user session info.");
        }
    }
}
