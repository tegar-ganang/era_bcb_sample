package aimclient;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.OscarTools;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.LoginFlapCmd;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.SnacPacket;
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
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.conn.WarningNotification;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.snaccmd.icbm.RecvImIcbm;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.snaccmd.icbm.RvResponse;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joscar.snaccmd.rooms.RoomInfoReq;
import aimclient.security.SecureSession;
import aimclient.security.SecureSessionException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class BasicConn extends AbstractFlapConn {

    protected final ByteBlock cookie;

    protected boolean sentClientReady = false;

    protected int[] snacFamilies = null;

    protected SnacFamilyInfo[] snacFamilyInfos;

    protected RateLimitingQueueMgr rateMgr = new RateLimitingQueueMgr();

    protected RvProcessor rvProcessor = new RvProcessor(snacProcessor);

    protected RvProcessorListener rvListener = new RvProcessorListener() {

        public void handleNewSession(NewRvSessionEvent event) {
            System.out.println("new RV session: " + event.getSession());
            event.getSession().addListener(rvSessionListener);
        }
    };

    protected Map trillianEncSessions = new HashMap();

    protected RvSessionListener rvSessionListener = new RvSessionListener() {

        public void handleRv(RecvRvEvent event) {
            RvCommand cmd = event.getRvCommand();
            RvSession session = event.getRvSession();
            SnacCommand snaccmd = event.getSnacCommand();
            if (!(snaccmd instanceof RecvRvIcbm)) return;
            RecvRvIcbm icbm = (RecvRvIcbm) snaccmd;
            System.out.println("got rendezvous on session <" + session + ">");
            System.out.println("- command: " + cmd);
            if (cmd instanceof FileSendReqRvCmd) {
                FileSendReqRvCmd rv = (FileSendReqRvCmd) cmd;
                RvConnectionInfo connInfo = rv.getConnInfo();
                InetAddress ip = connInfo.getExternalIP();
                int port = connInfo.getPort();
                if (ip != null && port != -1) {
                    System.out.println("starting ft thread..");
                    long cookie = icbm.getIcbmMessageId();
                    new RecvFileThread(tester, ip, port, session, cookie, connInfo.isEncrypted()).start();
                }
            } else if (cmd instanceof AbstractTrillianCryptRvCmd) {
                String key = OscarTools.normalize(session.getScreenname());
                TrillianEncSession encSession = (TrillianEncSession) trillianEncSessions.get(key);
                if (encSession == null) {
                    encSession = new TrillianEncSession(session);
                    trillianEncSessions.put(key, encSession);
                }
                encSession.handleRv(event);
            } else if (cmd instanceof DirectIMReqRvCmd) {
                if (((DirectIMReqRvCmd) cmd).getRequestType() == AbstractRequestRvCmd.REQTYPE_INITIALREQUEST) {
                    new DirectIMSession(tester.getScreenname(), session, event);
                }
            } else if (cmd instanceof SendBuddyIconRvCmd) {
            } else if (cmd instanceof SendBuddyListRvCmd) {
                session.sendResponse(RvResponse.CODE_NOT_ACCEPTING);
            } else if (cmd instanceof GetFileReqRvCmd) {
                if (((GetFileReqRvCmd) cmd).getCode() != -1) {
                    new HostGetFileThread(session, event).start();
                }
            } else if (cmd instanceof AddinsReqRvCmd) {
                session.sendRv(cmd);
            } else if (cmd instanceof ChatInvitationRvCmd) {
                ChatInvitationRvCmd circ = (ChatInvitationRvCmd) cmd;
                ByteBlock securityInfo = circ.getSecurityInfo();
                if (securityInfo != null) {
                    String sn = icbm.getSender().getScreenname();
                    String cookie = circ.getRoomInfo().getCookie();
                    String roomName = OscarTools.getRoomNameFromCookie(cookie);
                    try {
                        SecureSession secureSession = tester.getSecureSession();
                        secureSession.setChatKey(roomName, secureSession.extractChatKey(sn, securityInfo));
                    } catch (SecureSessionException e) {
                        e.printStackTrace();
                    }
                }
                tester.request(new RoomInfoReq(circ.getRoomInfo()));
            }
        }

        public void handleSnacResponse(RvSnacResponseEvent event) {
            System.out.println("got SNAC response for <" + event.getRvSession() + ">: " + event.getSnacCommand());
        }
    };

    {
        snacProcessor.setSnacQueueManager(rateMgr);
        rvProcessor.registerRvCmdFactory(new DefaultRvCommandFactory());
        rvProcessor.addListener(rvListener);
    }

    public BasicConn(JoscarTester tester, ByteBlock cookie) {
        super(tester);
        this.cookie = cookie;
    }

    public BasicConn(String host, int port, JoscarTester tester, ByteBlock cookie) {
        super(host, port, tester);
        this.cookie = cookie;
    }

    public BasicConn(InetAddress ip, int port, JoscarTester tester, ByteBlock cookie) {
        super(ip, port, tester);
        this.cookie = cookie;
    }

    protected DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    protected PrintWriter imLogger = null;

    protected ImTestFrame frame;

    public void setLogIms(ImTestFrame frame) {
        this.frame = frame;
        if (frame != null) {
            try {
                String file = System.getProperty("user.home") + System.getProperty("file.separator") + "ims.log";
                System.out.println("writing to " + file);
                imLogger = new PrintWriter(new FileOutputStream(file, true), true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            imLogger.close();
        }
    }

    protected void handleFlapPacket(FlapPacketEvent e) {
        FlapCommand cmd = e.getFlapCommand();
        if (cmd instanceof LoginFlapCmd) {
            getFlapProcessor().sendFlap(new LoginFlapCmd(cookie));
        } else {
            System.out.println("got FLAP command on channel 0x" + Integer.toHexString(e.getFlapPacket().getChannel()) + ": " + cmd);
        }
    }

    protected void handleSnacPacket(SnacPacketEvent e) {
        SnacPacket packet = e.getSnacPacket();
        System.out.println("got snac packet type " + Integer.toHexString(packet.getFamily()) + "/" + Integer.toHexString(packet.getCommand()) + ": " + e.getSnacCommand());
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof ServerReadyCmd) {
            ServerReadyCmd src = (ServerReadyCmd) cmd;
            setSnacFamilies(src.getSnacFamilies());
            SnacFamilyInfo[] familyInfos = SnacFamilyInfoFactory.getDefaultFamilyInfos(src.getSnacFamilies());
            setSnacFamilyInfos(familyInfos);
            tester.registerSnacFamilies(this);
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
                SecureSession secureSession = tester.getSecureSession();
                if (secureSession.hasCert(sn)) {
                    try {
                        msg = secureSession.decodeEncryptedIM(sn, encData);
                    } catch (SecureSessionException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    System.out.println(sn + " tried sending an encrypted " + "message, but I don't have his/her certificate " + " - try typing 'getcertinfo " + sn + "'");
                }
            } else {
                msg = OscarTools.stripHtml(message.getMessage());
            }
            String str = dateFormat.format(new Date()) + " IM from " + sn + ": " + msg;
            if (imLogger != null) {
                imLogger.println(str);
            }
            if (frame != null) {
                frame.echo("");
                frame.echo(str);
            }
            if (msg != null) {
                tester.privateMessageReceived(sn, msg);
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
            ExtraInfoBlock[] extraInfos = info.getExtraInfoBlocks();
            if (extraInfos != null) {
                for (int i = 0; i < extraInfos.length; i++) {
                    ExtraInfoBlock extraInfo = extraInfos[i];
                    ExtraInfoData data = extraInfo.getExtraData();
                    if (extraInfo.getType() == ExtraInfoBlock.TYPE_AVAILMSG) {
                        ByteBlock msgBlock = data.getData();
                        int len = BinaryTools.getUShort(msgBlock, 0);
                        byte[] msgBytes = msgBlock.subBlock(2, len).toByteArray();
                        String msg;
                        try {
                            msg = new String(msgBytes, "UTF-8");
                        } catch (UnsupportedEncodingException e1) {
                            e1.printStackTrace();
                            return;
                        }
                        if (msg.length() > 0) {
                            System.out.println(info.getScreenname() + " availability: " + msg);
                        }
                    }
                }
            }
            if (info.getCapabilityBlocks() != null) {
                List known = Arrays.asList(new CapabilityBlock[] { CapabilityBlock.BLOCK_CHAT, CapabilityBlock.BLOCK_DIRECTIM, CapabilityBlock.BLOCK_FILE_GET, CapabilityBlock.BLOCK_FILE_SEND, CapabilityBlock.BLOCK_GAMES, CapabilityBlock.BLOCK_GAMES2, CapabilityBlock.BLOCK_ICON, CapabilityBlock.BLOCK_SENDBUDDYLIST, CapabilityBlock.BLOCK_TRILLIANCRYPT, CapabilityBlock.BLOCK_VOICE, CapabilityBlock.BLOCK_ADDINS, CapabilityBlock.BLOCK_ICQCOMPATIBLE, CapabilityBlock.BLOCK_SOMETHING });
                List caps = new ArrayList(Arrays.asList(info.getCapabilityBlocks()));
                caps.removeAll(known);
                if (!caps.isEmpty()) {
                    System.out.println(sn + " has " + caps.size() + " unknown caps:");
                    for (Iterator it = caps.iterator(); it.hasNext(); ) {
                        System.out.println("- " + it.next());
                    }
                }
            }
        } else if (cmd instanceof BuddyOfflineCmd) {
            BuddyOfflineCmd boc = (BuddyOfflineCmd) cmd;
        } else if (cmd instanceof RateChange) {
            RateChange rc = (RateChange) cmd;
            System.out.println("rate change: current avg is " + rc.getRateInfo().getCurrentAvg());
        }
    }

    protected void handleSnacResponse(SnacResponseEvent e) {
        SnacPacket packet = e.getSnacPacket();
        System.out.println("got snac response type " + Integer.toHexString(packet.getFamily()) + "/" + Integer.toHexString(packet.getCommand()) + ": " + e.getSnacCommand());
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof RateInfoCmd) {
            RateInfoCmd ric = (RateInfoCmd) cmd;
            RateClassInfo[] rateClasses = ric.getRateClassInfos();
            int[] classes = new int[rateClasses.length];
            for (int i = 0; i < rateClasses.length; i++) {
                classes[i] = rateClasses[i].getRateClass();
            }
            request(new RateAck(classes));
        }
    }

    public int[] getSnacFamilies() {
        return snacFamilies;
    }

    protected void setSnacFamilies(int[] families) {
        this.snacFamilies = (int[]) families.clone();
        Arrays.sort(snacFamilies);
    }

    protected void setSnacFamilyInfos(SnacFamilyInfo[] infos) {
        snacFamilyInfos = infos;
    }

    protected boolean supportsFamily(int family) {
        return Arrays.binarySearch(snacFamilies, family) >= 0;
    }

    protected void clientReady() {
        if (!sentClientReady) {
            sentClientReady = true;
            request(new ClientReadyCmd(snacFamilyInfos));
        }
    }

    protected SnacRequest dispatchRequest(SnacCommand cmd) {
        return dispatchRequest(cmd, null);
    }

    protected SnacRequest dispatchRequest(SnacCommand cmd, SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);
        dispatchRequest(req);
        return req;
    }

    protected void dispatchRequest(SnacRequest req) {
        tester.handleRequest(req);
    }

    protected SnacRequest request(SnacCommand cmd, SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);
        handleReq(req);
        return req;
    }

    private void handleReq(SnacRequest request) {
        int family = request.getCommand().getFamily();
        if (snacFamilies == null || supportsFamily(family)) {
            sendRequest(request);
        } else {
            tester.handleRequest(request);
        }
    }
}
