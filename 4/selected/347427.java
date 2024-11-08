package jtmsmon.im;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.OscarTools;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.LoginFlapCmd;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.net.ConnDescriptor;
import net.kano.joscar.ratelim.RateLimitingQueueMgr;
import net.kano.joscar.rv.NewRvSessionEvent;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvProcessor;
import net.kano.joscar.rv.RvProcessorListener;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSessionListener;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.AbstractRequestRvCmd;
import net.kano.joscar.rvcmd.DefaultRvCommandFactory;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.addins.AddinsReqRvCmd;
import net.kano.joscar.rvcmd.chatinvite.ChatInvitationRvCmd;
import net.kano.joscar.rvcmd.directim.DirectIMReqRvCmd;
import net.kano.joscar.rvcmd.getfile.GetFileReqRvCmd;
import net.kano.joscar.rvcmd.icon.SendBuddyIconRvCmd;
import net.kano.joscar.rvcmd.sendbl.SendBuddyListRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.rvcmd.trillcrypt.AbstractTrillianCryptRvCmd;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.MiniUserInfo;
import net.kano.joscar.snaccmd.SnacFamilyInfoFactory;
import net.kano.joscar.snaccmd.buddy.BuddyOfflineCmd;
import net.kano.joscar.snaccmd.buddy.BuddyStatusCmd;
import net.kano.joscar.snaccmd.conn.ClientReadyCmd;
import net.kano.joscar.snaccmd.conn.ClientVersionsCmd;
import net.kano.joscar.snaccmd.conn.RateAck;
import net.kano.joscar.snaccmd.conn.RateChange;
import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snaccmd.conn.RateInfoCmd;
import net.kano.joscar.snaccmd.conn.RateInfoRequest;
import net.kano.joscar.snaccmd.conn.ServerReadyCmd;
import net.kano.joscar.snaccmd.conn.ServerVersionsCmd;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.conn.WarningNotification;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.snaccmd.icbm.RecvImIcbm;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.snaccmd.icbm.RvResponse;
import net.kano.joscar.snaccmd.rooms.RoomInfoReq;
import net.kano.joustsim.Screenname;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class description
 *
 *
 * @version    Enter version here..., 2006-12-02
 * @author     Enter your name here...
 */
public abstract class BasicConn extends AbstractFlapConn {

    /**
   * Constructs ...
   *
   *
   * @param cd
   * @param cookie
   * @param handler
   */
    protected BasicConn(ConnDescriptor cd, ByteBlock cookie, ConnectionHandler handler) {
        super(cd);
        this.cookie = cookie;
        this.handler = handler;
    }

    /**
   * Method description
   *
   *
   * @return
   */
    public int[] getSnacFamilies() {
        return snacFamilies;
    }

    /**
   * Method description
   *
   */
    protected void clientReady() {
        if (!sentClientReady) {
            sentClientReady = true;
            request(new ClientReadyCmd(snacFamilyInfos));
        }
    }

    /**
   * Method description
   *
   *
   * @param cmd
   *
   * @return
   */
    protected SnacRequest dispatchRequest(SnacCommand cmd) {
        return dispatchRequest(cmd, null);
    }

    /**
   * Method description
   *
   *
   * @param req
   */
    protected void dispatchRequest(SnacRequest req) {
    }

    /**
   * Method description
   *
   *
   * @param cmd
   * @param listener
   *
   * @return
   */
    protected SnacRequest dispatchRequest(SnacCommand cmd, SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);
        dispatchRequest(req);
        return req;
    }

    /**
   * Method description
   *
   *
   * @param e
   */
    protected void handleFlapPacket(FlapPacketEvent e) {
        FlapCommand cmd = e.getFlapCommand();
        if (cmd instanceof LoginFlapCmd) {
            getFlapProcessor().sendFlap(new LoginFlapCmd(cookie));
        } else {
            System.out.println("got FLAP command on channel 0x" + Integer.toHexString(e.getFlapPacket().getChannel()) + ": " + cmd);
        }
    }

    /**
   * Method description
   *
   *
   * @param e
   */
    protected void handleSnacPacket(SnacPacketEvent e) {
        SnacPacket packet = e.getSnacPacket();
        System.out.println("got snac packet type " + Integer.toHexString(packet.getFamily()) + "/" + Integer.toHexString(packet.getCommand()) + ": " + e.getSnacCommand());
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof ServerReadyCmd) {
            ServerReadyCmd src = (ServerReadyCmd) cmd;
            int[] families = src.getSnacFamilies();
            setSnacFamilies(families);
            List<SnacFamilyInfo> familyInfos = SnacFamilyInfoFactory.getDefaultFamilyInfos(families);
            setSnacFamilyInfos(familyInfos);
            handler.registerSnacFamilies(this);
            request(new ClientVersionsCmd(familyInfos));
            request(new RateInfoRequest());
        } else if (cmd instanceof RecvImIcbm) {
            RecvImIcbm icbm = (RecvImIcbm) cmd;
            String sn = icbm.getSenderInfo().getScreenname();
            InstantMessage message = icbm.getMessage();
            String msg = null;
            if (message.isEncrypted()) {
                ByteBlock encData = message.getEncryptedData();
                System.out.println("got [" + encData.getLength() + "]");
                {
                    System.out.println(sn + " tried sending an encrypted " + "message, but I don't have his/her certificate " + " - try typing 'getcertinfo " + sn + "'");
                }
            } else {
                msg = OscarTools.stripHtml(message.getMessage());
            }
            String str = dateFormat.format(new Date()) + " IM from " + sn + ": " + msg;
            if (imLogger != null) {
                imLogger.println(str);
            }
            if (msg != null) {
                String encFlag = (message.isEncrypted() ? "**ENCRYPTED** " : "");
                System.out.println(encFlag + "*" + sn + "* " + msg);
            }
        } else if (cmd instanceof WarningNotification) {
            WarningNotification wn = (WarningNotification) cmd;
            MiniUserInfo warner = wn.getWarner();
            if (warner == null) {
                System.out.println("*** You were warned anonymously to " + wn.getNewLevel() + "%");
            } else {
                System.out.println("*** " + warner.getScreenname() + " warned you up to " + wn.getNewLevel() + "%");
            }
        } else if (cmd instanceof BuddyStatusCmd) {
            BuddyStatusCmd bsc = (BuddyStatusCmd) cmd;
            FullUserInfo info = bsc.getUserInfo();
            String sn = info.getScreenname();
            List<ExtraInfoBlock> extraInfos = info.getExtraInfoBlocks();
            if (extraInfos != null) {
                for (ExtraInfoBlock extraInfo : extraInfos) {
                    ExtraInfoData data = extraInfo.getExtraData();
                    if (extraInfo.getType() == ExtraInfoBlock.TYPE_AVAILMSG) {
                        String msg = ExtraInfoData.readAvailableMessage(data);
                        if (msg.length() > 0) {
                            System.out.println(info.getScreenname() + " availability: " + msg);
                        }
                    }
                }
            }
            if (info.getCapabilityBlocks() != null) {
                List<CapabilityBlock> known = Arrays.asList(CapabilityBlock.BLOCK_CHAT, CapabilityBlock.BLOCK_DIRECTIM, CapabilityBlock.BLOCK_FILE_GET, CapabilityBlock.BLOCK_FILE_SEND, CapabilityBlock.BLOCK_GAMES, CapabilityBlock.BLOCK_GAMES2, CapabilityBlock.BLOCK_ICON, CapabilityBlock.BLOCK_SENDBUDDYLIST, CapabilityBlock.BLOCK_TRILLIANCRYPT, CapabilityBlock.BLOCK_VOICE, CapabilityBlock.BLOCK_ADDINS, CapabilityBlock.BLOCK_ICQCOMPATIBLE, CapabilityBlock.BLOCK_SOMETHING);
                List<CapabilityBlock> caps = new ArrayList<CapabilityBlock>(info.getCapabilityBlocks());
                caps.removeAll(known);
                if (!caps.isEmpty()) {
                    System.out.println(sn + " has " + caps.size() + " unknown caps:");
                    for (CapabilityBlock cap : caps) {
                        System.out.println("- " + cap);
                    }
                }
            }
        } else if (cmd instanceof BuddyOfflineCmd) {
            BuddyOfflineCmd boc = (BuddyOfflineCmd) cmd;
        } else if (cmd instanceof RateChange) {
            RateChange rc = (RateChange) cmd;
            System.out.println("rate change: current avg is " + rc.getRateInfo().getCurrentAvg());
        } else if (cmd instanceof ServerVersionsCmd) {
            ServerVersionsCmd svc = (ServerVersionsCmd) cmd;
            List<SnacFamilyInfo> familyInfos = svc.getSnacFamilyInfos();
        }
    }

    /**
   * Method description
   *
   *
   * @param e
   */
    protected void handleSnacResponse(SnacResponseEvent e) {
        SnacPacket packet = e.getSnacPacket();
        System.out.println("got snac response type " + Integer.toHexString(packet.getFamily()) + "/" + Integer.toHexString(packet.getCommand()) + ": " + e.getSnacCommand());
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof RateInfoCmd) {
            RateInfoCmd ric = (RateInfoCmd) cmd;
            List<RateClassInfo> rateClasses = ric.getRateClassInfos();
            int[] classes = new int[rateClasses.size()];
            for (int i = 0; i < rateClasses.size(); i++) {
                RateClassInfo rateClass = rateClasses.get(i);
                classes[i] = rateClass.getRateClass();
                System.out.println("- " + rateClass + ": " + rateClass.getCommands());
            }
            request(new RateAck(classes));
        }
    }

    /**
   * Method description
   *
   *
   * @param cmd
   * @param listener
   *
   * @return
   */
    protected SnacRequest request(SnacCommand cmd, SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);
        handleReq(req);
        return req;
    }

    /**
   * Method description
   *
   *
   * @param families
   */
    protected void setSnacFamilies(int[] families) {
        this.snacFamilies = families.clone();
        Arrays.sort(snacFamilies);
    }

    /**
   * Method description
   *
   *
   * @param infos
   */
    protected void setSnacFamilyInfos(Collection<SnacFamilyInfo> infos) {
        snacFamilyInfos = infos;
    }

    /**
   * Method description
   *
   *
   * @param family
   *
   * @return
   */
    protected boolean supportsFamily(int family) {
        return Arrays.binarySearch(snacFamilies, family) >= 0;
    }

    /**
   * Method description
   *
   *
   * @param request
   */
    private void handleReq(SnacRequest request) {
        int family = request.getCommand().getFamily();
        if ((snacFamilies == null) || supportsFamily(family)) {
            sendRequest(request);
        } else {
        }
    }

    /** Field description */
    protected boolean sentClientReady = false;

    /** Field description */
    protected int[] snacFamilies = null;

    /** Field description */
    protected RvProcessor rvProcessor = new RvProcessor(snacProcessor);

    /** Field description */
    protected RvProcessorListener rvListener = new RvProcessorListener() {

        public void handleNewSession(NewRvSessionEvent event) {
            System.out.println("new RV session: " + event.getSession());
            event.getSession().addListener(rvSessionListener);
        }
    };

    /** Field description */
    protected RateLimitingQueueMgr rateMgr = new RateLimitingQueueMgr();

    /** Field description */
    protected RvSessionListener rvSessionListener = new RvSessionListener() {

        public void handleRv(RecvRvEvent event) {
            RvCommand cmd = event.getRvCommand();
            RvSession session = event.getRvSession();
            SnacCommand snaccmd = event.getSnacCommand();
            if (!(snaccmd instanceof RecvRvIcbm)) {
                return;
            }
            RecvRvIcbm icbm = (RecvRvIcbm) snaccmd;
            System.out.println("got rendezvous on session <" + session + ">");
            System.out.println("- command: " + cmd);
            if (cmd instanceof FileSendReqRvCmd) {
                FileSendReqRvCmd rv = (FileSendReqRvCmd) cmd;
                RvConnectionInfo connInfo = rv.getConnInfo();
                InetAddress ip = connInfo.getExternalIP();
                int port = connInfo.getPort();
                if ((ip != null) && (port != -1)) {
                    System.out.println("starting ft thread..");
                    long cookie = icbm.getIcbmMessageId();
                }
            } else if (cmd instanceof AbstractTrillianCryptRvCmd) {
                String key = Screenname.normalize(session.getScreenname());
            } else if (cmd instanceof DirectIMReqRvCmd) {
                if (((DirectIMReqRvCmd) cmd).getRequestIndex() == AbstractRequestRvCmd.REQINDEX_FIRST) {
                }
            } else if (cmd instanceof SendBuddyIconRvCmd) {
            } else if (cmd instanceof SendBuddyListRvCmd) {
                session.sendResponse(RvResponse.CODE_NOT_ACCEPTING);
            } else if (cmd instanceof GetFileReqRvCmd) {
                if (((GetFileReqRvCmd) cmd).getCode() != -1) {
                }
            } else if (cmd instanceof AddinsReqRvCmd) {
                session.sendRv(cmd);
            } else if (cmd instanceof ChatInvitationRvCmd) {
                ChatInvitationRvCmd circ = (ChatInvitationRvCmd) cmd;
                ByteBlock securityInfo = circ.getSecurityInfo();
                if (securityInfo != null) {
                    String sn = icbm.getSenderInfo().getScreenname();
                    String cookie = circ.getRoomInfo().getCookie();
                    String roomName = OscarTools.getRoomNameFromCookie(cookie);
                }
            }
        }

        public void handleSnacResponse(RvSnacResponseEvent event) {
            System.out.println("got SNAC response for <" + event.getRvSession() + ">: " + event.getSnacCommand());
        }
    };

    /** Field description */
    protected PrintWriter imLogger = null;

    /** Field description */
    protected DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    /** Field description */
    protected final ByteBlock cookie;

    /** Field description */
    ConnectionHandler handler;

    /** Field description */
    protected Collection<SnacFamilyInfo> snacFamilyInfos;

    {
        snacProcessor.setSnacQueueManager(rateMgr);
        rvProcessor.registerRvCmdFactory(new DefaultRvCommandFactory());
        rvProcessor.addListener(rvListener);
    }
}
