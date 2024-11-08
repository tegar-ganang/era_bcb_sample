package com.peterhi.net.server.sgs;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Logger;
import com.peterhi.net.conv.impl.*;
import com.peterhi.net.conv.Converter;
import com.peterhi.net.conv.Convertible;
import com.peterhi.exceptions.Results;
import com.peterhi.net.server.sgs.ref.SGSClassListener;
import com.peterhi.util.Str;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.NameExistsException;
import com.sun.sgs.app.NameNotBoundException;

public class SGSBusinessProcess {

    private static final Logger logger = Logger.getLogger("sgsbus");

    private static final int KEY_NAME_SPLIT_AT = 36;

    private static final String N_SERVER_SESSION_NAME = "nserver";

    /**
	 * The session name is a combination of a GUID and your account
	 * here we get the account part
	 * @param name The session name
	 * @return The account part
	 */
    public static final String accFromSesName(String name) {
        return name.substring(KEY_NAME_SPLIT_AT);
    }

    /**
	 * The session name is a combination of a GUID and your account
	 * here we get the GUID part
	 * @param name The session name
	 * @return The GUID part
	 */
    public static final String keyFromSesName(String name) {
        return name.substring(0, KEY_NAME_SPLIT_AT);
    }

    /**
	 * Whether the session object is identified as a NServer client
	 * @param session The session object
	 * @return <c>true</c> if this session object is NServer client, otherwise <c>false</c>
	 */
    public static final boolean isNServerSession(ClientSession session) {
        return session.getName().equals(N_SERVER_SESSION_NAME);
    }

    /**
	 * Whether the given session name is identified as a NServer client
	 * @param sessionName The session name
	 * @return <c>true</c> if this session name is the name of
	 * the NServer client, otherwise <c>false</c>
	 */
    public static final boolean isNServerSession(String sessionName) {
        return N_SERVER_SESSION_NAME.equals(sessionName);
    }

    public static final void doPutKey(SGSServer server, ClientSession session, PutKey pk) {
        PutKeyDone pkd = new PutKeyDone();
        pkd.account = pk.account;
        pkd.result = Results.UNKNOWN;
        if (server.isLoggedInOrPendingLogin(pk.account)) {
            pkd.result = Results.DUPLICATE;
        } else {
            server.putPendingLogin(pk.account, pk.key, pk.nid);
            pkd.result = Results.OK;
        }
        try {
            session.send(Converter.getInstance().convert(pkd));
            logger.info(String.format("put key '%s' for account '%s', result: %s", pk.key, pk.account, pkd.result));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void doOpenClass(SGSServer server, ClientSession session, OpenClass oc) {
        OpenClassDone ocd = new OpenClassDone();
        ocd.cnid = oc.cnid;
        ocd.result = Results.UNKNOWN;
        if (isNServerSession(session)) {
            Hashtable<String, Serializable> props = new Hashtable<String, Serializable>();
            props.put(SGSChannelData.NSERVER_CHANNEL_ID, Integer.valueOf(oc.cnid));
            props.put(SGSChannelData.ALLOW_OR_BLOCK, Boolean.valueOf(oc.allowOrBlock));
            if (oc.password != null) {
                props.put(SGSChannelData.PASSWORD, oc.password);
            }
            HashSet<String> set = new HashSet<String>();
            if (oc.list != null) {
                if (oc.list != null) {
                    for (int i = 0; i < oc.list.length; i++) {
                        set.add(oc.list[i]);
                    }
                }
            }
            props.put(SGSChannelData.NAME_LIST, (Serializable) set);
            props.put(SGSChannelData.MODERATOR, oc.moderator);
            HashSet<String> teachers = new HashSet<String>();
            if (oc.teachers != null) {
                if (oc.teachers != null) {
                    for (int i = 0; i < oc.teachers.length; i++) {
                        teachers.add(oc.teachers[i]);
                    }
                }
            }
            props.put(SGSChannelData.TEACHER_LIST, (Serializable) teachers);
            if (createClassListener(server, oc.name, props)) {
                ocd.result = Results.OK;
            } else {
                ocd.result = Results.FAIL;
            }
        } else {
            ocd.result = Results.AUTH_FAIL;
        }
        try {
            session.send(Converter.getInstance().convert(ocd));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void doCloseClass(SGSServer server, ClientSession session, CloseClass cc) {
        CloseClassDone ccd = new CloseClassDone();
        ccd.name = cc.name;
        ccd.result = Results.OK;
        destroyClassListener(server, cc.name);
        try {
            session.send(Converter.getInstance().convert(ccd));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void doQueryChannel(SGSServer server, ClientSession session, QueryChannel channel) {
        DataManager dman = AppContext.getDataManager();
        QueryChannelDone qcd = new QueryChannelDone();
        String account = accFromSesName(session.getName());
        Vector<String> classrooms = new Vector<String>();
        for (Iterator<String> itor = server.channelIterator(); itor.hasNext(); ) {
            String name = itor.next();
            SGSChannelData o = dman.getBinding(name, SGSChannelData.class);
            if (canEnter(o, account)) {
                classrooms.add(o.get(SGSChannelData.NAME, String.class));
            }
        }
        if (classrooms.size() > 0) {
            qcd.channels = new String[classrooms.size()];
            classrooms.toArray(qcd.channels);
        }
        try {
            session.send(Converter.getInstance().convert(qcd));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void doEnterChannel(SGSServer server, ClientSession session, EnterChannel channel) {
        try {
            EnterChannelDone ecd = new EnterChannelDone();
            String you = accFromSesName(session.getName());
            ecd.name = channel.name;
            ecd.result = Results.UNKNOWN;
            SGSClientData l = server.getClientData(you);
            SGSChannelData cl = server.getChannelData(channel.name);
            if (cl != null) {
                if (canEnter(cl, you)) {
                    if (authenticate(cl, channel.password)) {
                        if (cl.get(SGSChannelData.MODERATOR, String.class).equals(you)) {
                            ecd.role = "moderator";
                            l.set(SGSClientData.ROLE, "moderator");
                        } else if (containsTeacher(cl, you)) {
                            ecd.role = "teacher";
                            l.set(SGSClientData.ROLE, "teacher");
                        } else {
                            ecd.role = "student";
                            l.set(SGSClientData.ROLE, "student");
                        }
                        join(server, cl, you);
                        l.set(SGSClientData.CURRENT_CHANNEL_NAME, channel.name);
                        l.set(SGSClientData.NSERVER_CHANNEL_ID, Integer.valueOf(cl.get(SGSChannelData.NSERVER_CHANNEL_ID, Integer.class)));
                        ecd.result = Results.OK;
                        ecd.cnid = cl.get(SGSChannelData.NSERVER_CHANNEL_ID, Integer.class);
                        NNewPeer nnp = new NNewPeer();
                        nnp.nid = l.get(SGSClientData.NSERVER_ID, Integer.class);
                        nnp.cnid = cl.get(SGSChannelData.NSERVER_CHANNEL_ID, Integer.class);
                        server.ndispatch(nnp);
                        NewPeer np = new NewPeer();
                        np.account = l.getAccount();
                        np.nid = l.get(SGSClientData.NSERVER_ID, Integer.class);
                        np.role = l.get(SGSClientData.ROLE, String.class);
                        channelwideDispatch(np, session, cl.getChannel());
                    } else {
                        ecd.result = Results.AUTH_FAIL;
                    }
                } else {
                    ecd.result = Results.NOT_ALLOWED;
                }
            } else {
                ecd.result = Results.NOT_FOUND;
            }
            session.send(Converter.getInstance().convert(ecd));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void doLeaveChannel(SGSServer server, ClientSession session, LeaveChannel lc) {
        try {
            String you = accFromSesName(session.getName());
            SGSClientData l = server.getClientData(you);
            String channelName = l.get(SGSClientData.CURRENT_CHANNEL_NAME, String.class);
            if (!Str.nostr(channelName)) {
                ChannelManager man = AppContext.getChannelManager();
                Channel chnl = man.getChannel(channelName);
                chnl.leave(session);
                onLeaveChannelNotify(server, l.get(SGSClientData.NSERVER_ID, Integer.class), l.get(SGSClientData.NSERVER_CHANNEL_ID, Integer.class), l.get(SGSClientData.CLIENT_SESSION, ClientSession.class), chnl);
                l.set(SGSClientData.CURRENT_CHANNEL_NAME, null);
                l.set(SGSClientData.NSERVER_CHANNEL_ID, Integer.valueOf(Integer.MIN_VALUE));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void onLeaveChannelNotify(SGSServer server, int nid, int cnid, ClientSession session, Channel chnl) {
        try {
            NKillPeer nnp = new NKillPeer();
            nnp.nid = nid;
            nnp.cnid = cnid;
            server.ndispatch(nnp);
            KillPeer kp = new KillPeer();
            kp.nid = nid;
            channelwideDispatch(kp, session, chnl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void doLogout(SGSServer server, ClientSession session, Logout logout) {
        try {
            session.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void doQueryPeer(SGSServer server, ClientSession session, QueryPeer peer) {
        try {
            String you = accFromSesName(session.getName());
            SGSClientData l = server.getClientData(you);
            QueryPeerDone qpd = new QueryPeerDone();
            if (Str.nostr(l.get(SGSClientData.CURRENT_CHANNEL_NAME, String.class))) {
                qpd.result = Results.NOT_IN_A_CHANNEL;
            } else {
                ChannelManager man = AppContext.getChannelManager();
                Channel channel = man.getChannel(l.get(SGSClientData.CURRENT_CHANNEL_NAME, String.class));
                Set<ClientSession> sessions = channel.getSessions();
                Vector<String> accounts = new Vector<String>();
                Vector<Integer> nids = new Vector<Integer>();
                Vector<String> roles = new Vector<String>();
                Vector<Boolean> talkings = new Vector<Boolean>();
                for (Iterator<ClientSession> itor = sessions.iterator(); itor.hasNext(); ) {
                    ClientSession cur = itor.next();
                    if (cur == session) {
                        continue;
                    }
                    String him = accFromSesName(cur.getName());
                    SGSClientData himl = server.getClientData(him);
                    accounts.add(himl.getAccount());
                    nids.add(himl.get(SGSClientData.NSERVER_ID, Integer.class));
                    roles.add(himl.get(SGSClientData.ROLE, String.class));
                    talkings.add(himl.get(SGSClientData.TALKING, Boolean.class));
                }
                qpd.accounts = new String[accounts.size()];
                qpd.nids = new int[nids.size()];
                qpd.roles = new String[roles.size()];
                qpd.talkings = new boolean[talkings.size()];
                accounts.toArray(qpd.accounts);
                roles.toArray(qpd.roles);
                for (int i = 0; i < qpd.nids.length; i++) {
                    qpd.nids[i] = nids.get(i);
                    qpd.talkings[i] = talkings.get(i);
                }
            }
            session.send(Converter.getInstance().convert(qpd));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void channelwideDispatch(Convertible conv, ClientSession you, Channel l) {
        try {
            Set<ClientSession> sessions = l.getSessions();
            for (Iterator<ClientSession> itor = sessions.iterator(); itor.hasNext(); ) {
                ClientSession cur = itor.next();
                if (cur == you) {
                    continue;
                }
                l.send(cur, Converter.getInstance().convert(conv));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static boolean createClassListener(SGSServer server, String name, Hashtable<String, Serializable> props) {
        try {
            ChannelManager manager = AppContext.getChannelManager();
            SGSChannelData data = new SGSChannelData(props);
            SGSClassListener l = new SGSClassListener();
            if (server.addChannel(name, data)) {
                try {
                    manager.createChannel(name, l, Delivery.RELIABLE);
                    data.set(SGSChannelData.NAME, name);
                    data.set(SGSChannelData.WHITEBOARD_DATA, new HashSet<AbstractDrawing>());
                    return true;
                } catch (NameExistsException ex) {
                    server.removeChannel(name);
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static void destroyClassListener(SGSServer server, String name) {
        try {
            ChannelManager manager = AppContext.getChannelManager();
            Channel channel = null;
            try {
                channel = manager.getChannel(name);
                channel.close();
            } catch (NameNotBoundException ex) {
            }
            server.removeChannel(name);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static boolean canEnter(SGSChannelData o, String account) {
        try {
            boolean allowOrBlock = o.get(SGSChannelData.ALLOW_OR_BLOCK, Boolean.class);
            HashSet<?> list = o.get(SGSChannelData.NAME_LIST, HashSet.class);
            boolean canEnter = false;
            if (allowOrBlock) {
                if (list.contains(account)) {
                    canEnter = true;
                }
            } else {
                if (!list.contains(account)) {
                    canEnter = true;
                }
            }
            return canEnter;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean authenticate(SGSChannelData d, String pass) {
        try {
            char[] password = d.get(SGSChannelData.PASSWORD, char[].class);
            if (password == null) {
                return true;
            }
            if (Str.nostr(pass)) {
                return false;
            }
            return new String(password).equals(pass);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean containsTeacher(SGSChannelData d, String a) {
        try {
            HashSet<?> teachers = d.get(SGSChannelData.TEACHER_LIST, HashSet.class);
            return teachers.contains(a);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static void join(SGSServer server, SGSChannelData d, String a) {
        try {
            ClientSession session = server.getClientSession(a);
            Channel channel = d.getChannel();
            channel.join(session, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
