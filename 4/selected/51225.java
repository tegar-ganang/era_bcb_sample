package sand;

import java.awt.EventQueue;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sand.gui.BaseWindow;
import sand.gui.IRCDocument;
import sand.gui.Window;
import sand.gui.WindowUtilites;
import jerklib.Channel;
import jerklib.events.IRCEvent;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.JoinEvent;
import jerklib.events.MessageEvent;
import jerklib.events.MotdEvent;
import jerklib.events.NickChangeEvent;
import jerklib.events.NoticeEvent;
import jerklib.events.PartEvent;
import jerklib.events.QuitEvent;
import jerklib.events.listeners.IRCEventListener;

public class IRCEventHandler implements IRCEventListener {

    private Map<IRCEvent.Type, EventRunnable> stratMap = new HashMap<IRCEvent.Type, EventRunnable>();

    private SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");

    public IRCEventHandler() {
        initStratMap();
    }

    @Override
    public void receiveEvent(final IRCEvent e) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (stratMap.containsKey(e.getType())) {
                    stratMap.get(e.getType()).run(e);
                } else {
                    System.out.println(e.getRawEventData());
                }
            }
        });
    }

    private void channelMsg(IRCEvent e) {
        MessageEvent chanEvent = (MessageEvent) e;
        Window window = WindowUtilites.getWindowForChannel(chanEvent.getChannel(), chanEvent.getSession(), BaseWindow.getWindowList());
        window.insertMsg(chanEvent.getNick(), chanEvent.getMessage());
    }

    private void connectComplete(IRCEvent e) {
        WindowUtilites.getBaseWindow().insertDefault("Connected to " + e.getSession().getRequestedConnection().getHostName());
        IRCClient.getInstance().selectedSession = e.getSession();
    }

    private void quit(IRCEvent e) {
        QuitEvent qe = (QuitEvent) e;
        List<Channel> chans = qe.getChannelList();
        for (Channel chan : chans) {
            Window win = WindowUtilites.getWindowForChannel(chan, e.getSession(), BaseWindow.getWindowList());
            win.insertDefault(qe.getWho() + " has quit - " + qe.getQuitMessage());
        }
    }

    private void part(IRCEvent e) {
        PartEvent pe = (PartEvent) e;
        Window win = WindowUtilites.getWindowForChannel(pe.getChannel(), e.getSession(), BaseWindow.getWindowList());
        win.insertDefault(pe.getWho() + " has left " + pe.getChannelName() + " - " + pe.getPartMessage());
    }

    private void privMsg(IRCEvent e) {
        MessageEvent pmg = (MessageEvent) e;
        Window win = WindowUtilites.getWindowForPrivateMsg(pmg.getNick(), pmg.getSession(), BaseWindow.getWindowList());
        if (win == null) {
            Window newWin = new Window(pmg.getSession(), null, pmg.getNick(), IRCDocument.Type.PRIV);
            BaseWindow bw = BaseWindow.getInstance();
            BaseWindow.getWindowList().add(newWin);
            bw.pane.add(pmg.getNick(), newWin);
        } else {
            win.insertDefault("<" + pmg.getNick() + "> " + pmg.getMessage());
        }
    }

    private void joinComplete(IRCEvent e) {
        JoinCompleteEvent je = (JoinCompleteEvent) e;
        Window win = new Window(je.getSession(), je.getChannel(), "", IRCDocument.Type.CHANNEL);
        BaseWindow bw = BaseWindow.getInstance();
        BaseWindow.getWindowList().add(win);
        bw.pane.add(je.getChannel().getName(), win);
    }

    private void join(IRCEvent e) {
        JoinEvent je = (JoinEvent) e;
        Window window = WindowUtilites.getWindowForChannel(je.getChannel(), je.getSession(), BaseWindow.getWindowList());
        window.insertDefault(je.getNick() + " has joined " + je.getChannelName());
    }

    private void motd(IRCEvent e) {
        MotdEvent me = (MotdEvent) e;
        Window window = WindowUtilites.getBaseWindow();
        window.insertDefault(me.getMotdLine());
    }

    private void nickChanged(IRCEvent e) {
        NickChangeEvent nce = (NickChangeEvent) e;
        List<Window> windows = WindowUtilites.getWindowsForSession(e.getSession(), BaseWindow.getWindowList());
        if (windows.size() >= 1) {
            List<Window> pmWindows = WindowUtilites.getWindowsForNick(nce.getOldNick(), e.getSession(), BaseWindow.getWindowList());
            List<Window> channelWindows = WindowUtilites.getWindowsForNick(nce.getNewNick(), e.getSession(), BaseWindow.getWindowList());
            channelWindows.addAll(pmWindows);
            for (Window win : channelWindows) {
                if (nce.getNewNick().equals(e.getSession().getNick())) {
                    win.insertDefault("You are now known as " + nce.getNewNick());
                } else {
                    win.insertDefault(nce.getOldNick() + " is now known as " + nce.getNewNick());
                }
            }
        }
        if (nce.getNewNick().equals(e.getSession().getNick())) {
            WindowUtilites.getBaseWindow().insertDefault("You are now known as " + nce.getNewNick());
        }
    }

    private void notice(IRCEvent e) {
        NoticeEvent ne = (NoticeEvent) e;
        Window win = WindowUtilites.getBaseWindow();
        win.insertDefault(ne.getNoticeMessage());
        win.insertDefault(ne.getRawEventData());
    }

    private void initStratMap() {
        stratMap.put(IRCEvent.Type.CHANNEL_MESSAGE, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                channelMsg(e);
            }
        });
        stratMap.put(IRCEvent.Type.CONNECT_COMPLETE, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                connectComplete(e);
            }
        });
        stratMap.put(IRCEvent.Type.PRIVATE_MESSAGE, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                privMsg(e);
            }
        });
        stratMap.put(IRCEvent.Type.JOIN_COMPLETE, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                joinComplete(e);
            }
        });
        stratMap.put(IRCEvent.Type.JOIN, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                join(e);
            }
        });
        stratMap.put(IRCEvent.Type.MOTD, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                motd(e);
            }
        });
        stratMap.put(IRCEvent.Type.NICK_CHANGE, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                nickChanged(e);
            }
        });
        stratMap.put(IRCEvent.Type.NOTICE, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                notice(e);
            }
        });
        stratMap.put(IRCEvent.Type.QUIT, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                quit(e);
            }
        });
        stratMap.put(IRCEvent.Type.PART, new EventRunnable() {

            @Override
            public void run(IRCEvent e) {
                part(e);
            }
        });
    }

    private interface EventRunnable {

        public void run(IRCEvent e);
    }
}
