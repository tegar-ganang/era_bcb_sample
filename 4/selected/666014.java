package sand;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTabbedPane;
import sand.gui.BaseWindow;
import sand.gui.IRCDocument;
import sand.gui.Window;
import sand.gui.WindowUtilites;
import sand.gui.IRCDocument.Type;
import jerklib.Channel;
import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.ProfileImpl;
import jerklib.Session;

public class UserInputHandler implements InputListener {

    private Map<String, InputRunnable> stratMap = new HashMap<String, InputRunnable>();

    private ConnectionManager manager;

    public UserInputHandler(ConnectionManager manager) {
        this.manager = manager;
        initMap();
    }

    @Override
    public void receiveInput(String input) {
        String[] tokens = input.split("\\s+");
        if (stratMap.containsKey(tokens[0])) {
            stratMap.get(tokens[0]).run(input);
        } else {
            if (tokens[0].startsWith("/")) {
                WindowUtilites.getBaseWindow().insertDefault("Unreconized Command: " + tokens[0]);
            } else {
                JTabbedPane pane = BaseWindow.getInstance().pane;
                Window window = (Window) pane.getSelectedComponent();
                if (!window.equals(WindowUtilites.getBaseWindow())) {
                    Channel chan = window.getDocument().getChannel();
                    if (chan != null) {
                        chan.say(input);
                        window.insertMsg(window.getDocument().getSession().getNick(), input);
                    } else if (window.getDocument().getNick() != null) {
                        String ourNick = window.getDocument().getSession().getNick();
                        window.insertMsg(ourNick, input);
                        window.getDocument().getSession().sayPrivate(window.getDocument().getNick(), input);
                    }
                }
            }
        }
    }

    private void connect(String data) {
        String[] tokens = data.split("\\s+");
        if (tokens.length >= 2) {
            WindowUtilites.getBaseWindow().insertDefault("<> Attempting to connect to " + tokens[1]);
            if (tokens.length == 3) {
                manager.requestConnection(tokens[1], Integer.parseInt(tokens[2])).addIRCEventListener(new IRCEventHandler());
            } else {
                manager.requestConnection(tokens[1]).addIRCEventListener(new IRCEventHandler());
            }
        } else {
            WindowUtilites.getBaseWindow().insertDefault("You hath failed it - example: /connect irc.freenode.net");
        }
    }

    private void join(String data) {
        BaseWindow winder = BaseWindow.getInstance();
        String[] tokens = data.split("\\s+");
        if (tokens.length == 2) {
            Session selectedSession = IRCClient.getInstance().selectedSession;
            if (selectedSession != null) {
                if (selectedSession.isConnected()) {
                    winder.mainWindow.insertDefault("Joining " + tokens[1]);
                    selectedSession.joinChannel(tokens[1]);
                } else {
                    winder.mainWindow.insertDefault("Not currently connected to " + selectedSession.getRequestedConnection().getHostName());
                }
            } else {
                winder.mainWindow.insertDefault("No selected session");
            }
        } else {
            winder.mainWindow.insertDefault("Thou Hath Failed It! Example: /join #swing");
        }
    }

    private void pm(String input) {
        Session selectedSession = IRCClient.getInstance().selectedSession;
        String[] tokens = input.split("\\s+");
        if (tokens.length > 2) {
            Window win = WindowUtilites.getWindowForPrivateMsg(tokens[1], selectedSession, BaseWindow.getWindowList());
            if (win == null) {
                win = new Window(selectedSession, null, tokens[1], IRCDocument.Type.PRIV);
            }
            Pattern p = Pattern.compile("^/msg\\s+.*?\\s+(.*)$");
            Matcher m = p.matcher(input);
            if (m.matches()) {
                String ourNick = selectedSession.getNick();
                BaseWindow bw = BaseWindow.getInstance();
                BaseWindow.getWindowList().add(win);
                bw.pane.add(tokens[1], win);
                win.insertDefault("<" + ourNick + "> " + m.group(1));
                selectedSession.sayPrivate(tokens[1], m.group(1));
            } else {
                new Exception().printStackTrace();
            }
        }
    }

    private void changeNick(String input) {
        Session selectedSession = IRCClient.getInstance().selectedSession;
        String[] tokens = input.split("\\s+");
        if (tokens.length == 2) {
            Profile current = selectedSession.getRequestedConnection().getProfile();
            Profile newProfile = new ProfileImpl(current.getName(), tokens[1], current.getSecondNick(), current.getThirdNick());
            selectedSession.changeProfile(newProfile);
        }
    }

    private void names(String input) {
        Window win = (Window) BaseWindow.getInstance().pane.getSelectedComponent();
        Channel chan = win.getDocument().getChannel();
        if (chan != null) {
            win.printNicks(chan.getNicks());
        }
    }

    private void closeWindow(String input) {
        Window win = (Window) BaseWindow.getInstance().pane.getSelectedComponent();
        Type type = win.getDocument().getType();
        if (type == Type.MAIN) {
        } else if (type == Type.CHANNEL) {
            String msg = "Leaving";
            if (!input.toLowerCase().matches("^/window close\\s*$")) {
                msg = input.substring(12);
            }
            win.getDocument().getSession().partChannel(win.getDocument().getChannel(), msg);
            BaseWindow.getWindowList().remove(win);
            BaseWindow.getInstance().pane.remove(win);
        } else {
            BaseWindow.getWindowList().remove(win);
            BaseWindow.getInstance().pane.remove(win);
        }
    }

    private void initMap() {
        stratMap.put("/connect", new InputRunnable() {

            @Override
            public void run(String input) {
                connect(input);
            }
        });
        stratMap.put("/join", new InputRunnable() {

            @Override
            public void run(String input) {
                join(input);
            }
        });
        stratMap.put("/j", new InputRunnable() {

            @Override
            public void run(String input) {
                join(input);
            }
        });
        stratMap.put("/msg", new InputRunnable() {

            @Override
            public void run(String input) {
                pm(input);
            }
        });
        stratMap.put("/nick", new InputRunnable() {

            @Override
            public void run(String input) {
                changeNick(input);
            }
        });
        stratMap.put("/names", new InputRunnable() {

            @Override
            public void run(String input) {
                names(input);
            }
        });
        stratMap.put("/window", new InputRunnable() {

            @Override
            public void run(String input) {
                closeWindow(input);
            }
        });
    }

    private interface InputRunnable {

        public void run(String input);
    }
}
