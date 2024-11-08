package net.xiaoxishu.heiyou;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import net.xiaoxishu.heiyou.listener.ServerEventListener;
import net.xiaoxishu.heiyou.message.LogMessage;
import net.xiaoxishu.heiyou.message.ReceiveMessage;
import net.xiaoxishu.heiyou.message.Sender;
import net.xiaoxishu.heiyou.message.Message.Type;
import net.xiaoxishu.util.DialogHelper;
import org.apache.log4j.Logger;
import static net.xiaoxishu.util.MessageUtil.getMessage;

/**
 * Heiyou Server Thread
 * 
 * @author lushu
 *
 */
public class ServerThread {

    public enum Event {

        RUNNING, STOPPED
    }

    public static final String IMAGE_FILE_EXT = ".png";

    private static final Logger logger = Logger.getLogger(ServerThread.class);

    private ServerSocket socket;

    private Set<ServerEventListener> set = new HashSet<ServerEventListener>();

    private int port = 2600;

    private MessageShower shower;

    public ServerThread() {
        this(MessageShower.getShower(MessageShower.MESSAGE_AUTO_SHOW));
    }

    public ServerThread(MessageShower shower) {
        this.shower = shower;
    }

    public void setShower(int flag) {
        setShower(MessageShower.getShower(flag));
    }

    public MessageShower getShower() {
        return shower;
    }

    public void setShower(MessageShower shower) {
        this.shower = shower;
    }

    public boolean addServerEventListener(ServerEventListener listener) {
        return set.add(listener);
    }

    public boolean removeServerEventListener(ServerEventListener listener) {
        return set.remove(listener);
    }

    /**
     * 改变Server的状态
     * 
     * @param inService
     */
    public void changeServerStatus(boolean inService) {
        if (inService) stopService(); else startService();
    }

    public void startService() {
        if (socket == null) {
            Thread t = new Thread() {

                public void run() {
                    try {
                        logger.debug("Create a new Server Socket at: " + port);
                        socket = new ServerSocket(port);
                        Notifier.info(getMessage("start_ok"));
                        fireServerEvent(Event.RUNNING);
                        while (true) {
                            logger.debug("waiting for connection..");
                            Socket conn = socket.accept();
                            if (Config.inBlackList(conn.getInetAddress().getHostAddress())) {
                                logger.debug("receive msg from a blacklist host");
                                conn.close();
                                continue;
                            }
                            ReceiveMessage message = receive(conn);
                            if (message.getType() != Type.UNKNOWN && shower != MessageShower.IGNORE_SHOWER && shower != null) {
                                try {
                                    shower.showMessage(message);
                                } catch (RuntimeException e) {
                                    logger.warn("Error in showing a msg: ", e);
                                }
                            } else if (shower == MessageShower.IGNORE_SHOWER) {
                                for (ServerEventListener l : set) l.receiveMsg();
                            }
                            conn.close();
                            logger.debug("After connection$$");
                        }
                    } catch (BindException be) {
                        DialogHelper.warn(null, String.format(getMessage("start_err_port"), port));
                    } catch (IOException e) {
                        logger.warn("Failed to create a new socket: " + e);
                    } finally {
                        try {
                            if (socket != null) socket.close();
                        } catch (IOException e2) {
                        }
                        logger.debug("Release the Server Socket at: " + port);
                        socket = null;
                        fireServerEvent(Event.STOPPED);
                    }
                }
            };
            t.start();
        }
    }

    public void stopService() {
        if (socket != null) try {
            socket.close();
            Notifier.info(getMessage("stop_ok"));
        } catch (IOException e) {
            logger.warn("Error in stop service: ", e);
        }
    }

    private void fireServerEvent(Event e) {
        for (ServerEventListener l : set) l.serverStatusChanged(e);
    }

    private ReceiveMessage receive(final Socket conn) throws IOException {
        InputStream in = conn.getInputStream();
        byte[] buffer = new byte[512];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len;
        while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
        byte[] tmp = out.toByteArray();
        int index = 0;
        while (index < tmp.length) if (tmp[index++] == Sender.SEPERATOR) break;
        Type type;
        try {
            type = Type.valueOf(new String(tmp, 0, index - 1));
        } catch (IllegalArgumentException e) {
            type = Type.UNKNOWN;
        }
        if (logger.isDebugEnabled()) logger.debug("we recevie a message: " + type);
        if (Type.UNKNOWN == type || (Type.QUERY != type && index == tmp.length)) {
            logger.warn("[beep] receive unknown type message or [" + type + "]message without content.");
            return ReceiveMessage.INVALID_MESSAGE;
        }
        ReceiveMessage receive = new ReceiveMessage(type);
        if (Type.QUERY == type) {
            for (ServerEventListener l : set) l.getBuddyQuery(conn.getInetAddress().getHostAddress());
        } else if (Type.TEXT == type) {
            String msg = new String(tmp, index, tmp.length - index, "utf-8");
            String title = String.format(getMessage("msg_text_title"), conn.getInetAddress().getHostAddress());
            receive.setTitle(title);
            receive.setContent(msg);
        } else if (Type.LINK == type) {
            String link = new String(tmp, index, tmp.length - index, "utf-8");
            URL url;
            try {
                url = new URL(link);
            } catch (MalformedURLException e) {
                url = new URL("http", link, "");
            }
            String title = String.format(getMessage("msg_link_title"), conn.getInetAddress().getHostAddress());
            receive.setTitle(title);
            receive.setContent(url.toString());
            receive.setUrl(url);
        } else if (Type.IMAGE == type) {
            String title = String.format(getMessage("msg_image_title"), conn.getInetAddress().getHostAddress());
            File file = new File(Config.getLogPath() + System.currentTimeMillis() + IMAGE_FILE_EXT);
            FileOutputStream fileOut = new FileOutputStream(file);
            fileOut.write(tmp, index, tmp.length - index);
            fileOut.flush();
            fileOut.close();
            String msg = String.format(getMessage("msg_image_content"), file.toURL().getPath());
            receive.setTitle(title);
            receive.setContent(msg);
            receive.setUrl(file.toURL());
        }
        receive.setSource(conn.getInetAddress().getHostAddress());
        if (receive.getType() != Type.QUERY) Recorder.addMessage(LogMessage.getLog(receive));
        OutputStream response = conn.getOutputStream();
        response.write(Sender.RESP_OK_BYTES);
        response.flush();
        response.close();
        return receive;
    }
}
