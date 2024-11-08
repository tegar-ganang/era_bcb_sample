package net.sourceforge.sandirc.gui;

import jerklib.Channel;
import net.sourceforge.sandirc.InputListener;
import net.sourceforge.sandirc.TabCompleteInfo;
import net.sourceforge.sandirc.actions.SelectColorAction;
import net.sourceforge.sandirc.gui.text.IRCDocument;
import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import net.sourceforge.sandirc.actions.FormatBoldAction;
import net.sourceforge.sandirc.actions.FormatItalicAction;
import net.sourceforge.sandirc.actions.FormatNormalAction;
import net.sourceforge.sandirc.actions.FormatUnderlineAction;
import net.sourceforge.sandirc.controllers.TextComponentHistory;

public class InputBar extends JPanel {

    public static InputBar me;

    private JComponent styleBar = new JPanel(new FlowLayout(FlowLayout.LEFT));

    private JTextPane field = new JTextPane();

    private TextComponentHistory history;

    Map<IRCWindow, TabCompleteInfo> tmpNicks = new HashMap<IRCWindow, TabCompleteInfo>();

    private InputBar() {
        super(new BorderLayout());
        history = new TextComponentHistory(field);
        initGui();
        enableStyleComponents(false);
    }

    public JTextPane getField() {
        return field;
    }

    public void enableStyleComponents(boolean b) {
        for (int i = 0; i < styleBar.getComponentCount(); i++) {
            styleBar.getComponent(i).setEnabled(b);
        }
    }

    public void toggleStyleBarVisible() {
        styleBar.setVisible(!styleBar.isVisible());
    }

    /**
     *
     * @return
     */
    public static InputBar getInstance() {
        if (me == null) {
            me = new InputBar();
        }
        return me;
    }

    private void fireInputEvent(String input) {
        SandIRCContainer container = SandIRCFrame.getInstance().getCurrentView();
        container.receiveInput(input);
    }

    public void addMouseInputListener(MouseListener l) {
        field.addMouseListener(l);
    }

    private void initGui() {
        field.setBackground(new Color(239, 235, 231));
        field.getCaret().setBlinkRate(0);
        initActions(field);
        JPanel p = new JPanel(new GridLayout());
        p.add(field);
        styleBar.add(new ImageButton(new FormatBoldAction()));
        styleBar.add(new ImageButton(new FormatItalicAction()));
        styleBar.add(new ImageButton(new FormatNormalAction()));
        styleBar.add(new ImageButton(new FormatUnderlineAction()));
        styleBar.add(new ImageButton(new SelectColorAction()));
        add(styleBar, BorderLayout.NORTH);
        add(p, BorderLayout.CENTER);
    }

    private void findNick() {
        IRCWindowContainer container = SandIRCFrame.getInstance().getCurrentWindowContainer();
        if (container == null) {
            return;
        }
        IRCWindow window = (IRCWindow) container.getSelectedTab().getContentComponent();
        IRCDocument doc = window.getDocument();
        Channel chan = doc.getChannel();
        if (chan != null) {
            String data = field.getText();
            int postion = field.getCaretPosition();
            data = data.substring(0, postion);
            String[] tokens = data.split("\\s+");
            String completMe = tokens[tokens.length - 1];
            List<String> nicks = new ArrayList<String>(chan.getNicks());
            if (tmpNicks.containsKey(window)) {
                System.out.println("Found window");
                TabCompleteInfo info = tmpNicks.get(window);
                if ((info != null) && info.current.equals(completMe)) {
                    if ((new Date().getTime() - info.date.getTime()) < 1500) {
                        nicks = info.tmpNicks;
                        int diff = info.current.length() - info.orig.length();
                        try {
                            field.getDocument().remove(postion - diff, diff);
                            System.out.println("Set completeMe to " + info.orig);
                            completMe = info.orig;
                            field.setCaretPosition(postion - diff);
                            postion = postion - diff;
                        } catch (BadLocationException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("too old or no info");
                        tmpNicks.remove(window);
                    }
                }
            } else {
                System.out.println("Did not find window");
            }
            for (Iterator<String> it = nicks.iterator(); it.hasNext(); ) {
                String nick = it.next();
                if (nick.toLowerCase().startsWith(completMe.toLowerCase())) {
                    try {
                        System.out.println("trying to remove " + completMe + " for " + nick);
                        System.out.println(field.getText());
                        field.getDocument().remove(postion - completMe.length(), completMe.length());
                        field.getDocument().insertString(postion - completMe.length(), nick, null);
                        it.remove();
                        TabCompleteInfo info = new TabCompleteInfo();
                        info.current = nick;
                        info.orig = completMe;
                        info.date = new Date();
                        info.tmpNicks = nicks;
                        tmpNicks.put(window, info);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
            tmpNicks.remove(window);
        }
    }

    private void initActions(final JTextPane field) {
        InputMap imap = field.getInputMap();
        ActionMap amap = field.getActionMap();
        for (int i = 0; i < 10; i++) {
            final int x = i;
            String name = "alt " + (x + 1);
            field.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(name), name);
            amap.put(name, new AbstractAction(name) {

                public void actionPerformed(ActionEvent e) {
                    IRCWindowContainer con = SandIRCFrame.getInstance().getCurrentWindowContainer();
                    if (con == null) return;
                    if (con.getTabCount() >= x + 1) {
                        con.setSelectedTab(con.getTabAt(x));
                    }
                }
            });
        }
        imap.put(KeyStroke.getKeyStroke("ENTER"), "enter");
        amap.put("enter", new AbstractAction("enter") {

            public void actionPerformed(ActionEvent e) {
                fireInputEvent(field.getText());
                history.add();
            }
        });
        field.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new HashSet<AWTKeyStroke>());
        imap.put(KeyStroke.getKeyStroke("TAB"), "TAB");
        amap.put("TAB", new AbstractAction("TAB") {

            public void actionPerformed(ActionEvent e) {
                findNick();
            }
        });
        imap.put(KeyStroke.getKeyStroke("control C"), "clear");
        amap.put("clear", new AbstractAction("clear") {

            public void actionPerformed(ActionEvent e) {
                field.setText("");
            }
        });
        imap.put(KeyStroke.getKeyStroke("control N"), "nicklist");
        amap.put("nicklist", new AbstractAction("nicklist") {

            public void actionPerformed(ActionEvent e) {
                SandIRCFrame frame = SandIRCFrame.getInstance();
                if (frame.getCurrentWindowContainer() == null) return;
                IRCWindow win = frame.getCurrentWindowContainer().getSelectedWindow();
                win.showUserList(!win.isUserListShowing());
            }
        });
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        amap.put("up", new AbstractAction("up") {

            public void actionPerformed(ActionEvent e) {
                history.back();
            }
        });
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        amap.put("down", new AbstractAction("down") {

            public void actionPerformed(ActionEvent e) {
                history.forward();
            }
        });
    }
}
