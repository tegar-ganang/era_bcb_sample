package frontend;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import backend.Frontend;
import backend.PositionedObject;
import backend.Verb;
import backend.World;

public class MainDisplay extends Applet implements ActionListener, KeyListener, MouseListener, ListSelectionListener, Frontend {

    private static final long serialVersionUID = 1L;

    private static final int delay = 40;

    private TerrianDisplay game;

    private InventoryDisplay inventory;

    private Timer timer;

    private JPanel login;

    private JTextField uname;

    private JTextField pname;

    private JButton go;

    private JScrollPane dropDown;

    private World worldRef;

    private JTextField inputField;

    private Vector<String> commandList = new Vector<String>();

    private String lastCommand;

    private JList theList;

    private JTextArea chatField;

    private PopupMenu pMenu;

    private PositionedObject clickedObject = null;

    private Verb currentVerb = null;

    private JComboBox layerCombo = null;

    private LandTypeSelector landTypeSelector = null;

    private JTabbedPane rightPane = null;

    public void init() {
        login = new JPanel();
        go = new JButton("Login");
        go.addActionListener(this);
        uname = new JTextField();
        pname = new JPasswordField();
        uname.setPreferredSize(new Dimension(100, 20));
        pname.setPreferredSize(new Dimension(100, 20));
        uname.addKeyListener(this);
        pname.addKeyListener(this);
        login.add(new JLabel("Username: "));
        login.add(uname);
        login.add(new JLabel("Password: "));
        login.add(pname);
        login.add(go);
        login.setPreferredSize(new Dimension(185, 200));
        final int cOff = 100;
        Insets i = getInsets();
        Dimension d = login.getPreferredSize();
        login.setBounds(i.left + cOff, i.top, d.width, d.height);
        setSize(600, 400);
    }

    private JComponent createGameView() {
        JPanel mainPanel = new JPanel();
        Border border = BorderFactory.createLineBorder(Color.BLACK, 1);
        Border border2 = BorderFactory.createLoweredBevelBorder();
        mainPanel.setBorder(border);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JPanel commandPanel = new JPanel();
        commandPanel.setBorder(border);
        inputField = new JTextField(15);
        JButton sendButton = new JButton("Send");
        sendButton.addMouseListener(this);
        commandPanel.add(new JLabel("Say:"), BorderLayout.WEST);
        commandPanel.add(inputField, BorderLayout.CENTER);
        commandPanel.add(sendButton, BorderLayout.EAST);
        commandPanel.setMaximumSize(new Dimension(2000, sendButton.getHeight()));
        inputField.addKeyListener(this);
        game = new TerrianDisplay(worldRef, this);
        game.setBorder(border2);
        mainPanel.add(game, BorderLayout.CENTER);
        mainPanel.add(commandPanel, BorderLayout.PAGE_END);
        final int cOff = 0;
        Insets i = getInsets();
        Dimension d = mainPanel.getPreferredSize();
        mainPanel.setBounds(i.left + cOff, i.top, d.width, d.height);
        inventory = new InventoryDisplay(worldRef, this);
        inventory.setBorder(border2);
        Box editPanel = new Box(BoxLayout.Y_AXIS);
        String[] layers = { "Bottom layer", "Top layer" };
        layerCombo = new JComboBox(layers);
        layerCombo.setMaximumSize(layerCombo.getPreferredSize());
        editPanel.add(layerCombo);
        landTypeSelector = new LandTypeSelector(worldRef);
        editPanel.add(landTypeSelector);
        Container extendGrid = new Container();
        extendGrid.setLayout(new GridLayout(3, 6));
        JButton extendN = new JButton("+");
        extendN.addActionListener(new ExtendActionListener('n', false));
        JButton extendE = new JButton("+");
        extendE.addActionListener(new ExtendActionListener('e', false));
        JButton extendS = new JButton("+");
        extendS.addActionListener(new ExtendActionListener('s', false));
        JButton extendW = new JButton("+");
        extendW.addActionListener(new ExtendActionListener('w', false));
        JButton unextendN = new JButton("-");
        unextendN.addActionListener(new ExtendActionListener('n', true));
        JButton unextendE = new JButton("-");
        unextendE.addActionListener(new ExtendActionListener('e', true));
        JButton unextendS = new JButton("-");
        unextendS.addActionListener(new ExtendActionListener('s', true));
        JButton unextendW = new JButton("-");
        unextendW.addActionListener(new ExtendActionListener('w', true));
        extendGrid.add(new Container());
        extendGrid.add(extendN);
        extendGrid.add(new Container());
        extendGrid.add(new Container());
        extendGrid.add(unextendN);
        extendGrid.add(new Container());
        extendGrid.add(extendW);
        extendGrid.add(new Container());
        extendGrid.add(extendE);
        extendGrid.add(unextendW);
        extendGrid.add(new Container());
        extendGrid.add(unextendE);
        extendGrid.add(new Container());
        extendGrid.add(extendS);
        extendGrid.add(new Container());
        extendGrid.add(new Container());
        extendGrid.add(unextendS);
        extendGrid.add(new Container());
        editPanel.add(extendGrid);
        extendGrid.setMaximumSize(extendGrid.getPreferredSize());
        rightPane = new JTabbedPane();
        rightPane.addTab("Inventory", inventory);
        rightPane.addTab("Edit", editPanel);
        Box overallBox = new Box(BoxLayout.X_AXIS);
        overallBox.add(mainPanel);
        overallBox.add(rightPane);
        return overallBox;
    }

    public void start() {
        if (login == null) return;
        setLayout(null);
        add(login);
        uname.requestFocus();
        login = null;
    }

    public String getAppletInfo() {
        return "greatest game ever, or maybe tetris is?";
    }

    public void actionPerformed(ActionEvent a) {
        String u = uname.getText();
        String p = pname.getText();
        Rectangle rect = go.getBounds();
        go.setText("Loading...");
        rect.width += 40;
        rect.x -= 20;
        go.setBounds(rect);
        go.setEnabled(false);
        worldRef = new World(this);
        commandList.add("connect " + u + " " + p);
        try {
            worldRef.connect("user.interface.org.nz", 7777);
            worldRef.login(u, p);
        } catch (IOException e) {
            System.err.println(e);
            go.setText("Login");
            rect.width -= 40;
            rect.x += 20;
            go.setBounds(rect);
            go.setEnabled(true);
            return;
        }
        if (true) {
            removeAll();
            setLayout(new BorderLayout());
            add(createGameView(), BorderLayout.CENTER);
            inputField.requestFocus();
            validate();
            TimerTask method = new TimerTask() {

                public void run() {
                    game.update();
                }
            };
            timer = new Timer();
            timer.scheduleAtFixedRate(method, delay, delay);
            worldRef.startConnectionThread();
        }
    }

    public void keyPressed(KeyEvent k) {
        if (k.getSource() == uname) {
            switch(k.getKeyCode()) {
                case KeyEvent.VK_TAB:
                case KeyEvent.VK_ENTER:
                    pname.requestFocus();
                    return;
            }
        } else if (k.getSource() == pname) {
            switch(k.getKeyCode()) {
                case KeyEvent.VK_TAB:
                    uname.requestFocus();
                    return;
                case KeyEvent.VK_ENTER:
                    actionPerformed(null);
                    return;
            }
        } else if (k.getSource() instanceof JList) {
            if (k.getKeyCode() == KeyEvent.VK_ENTER) destroyMenu(); else if (k.getKeyCode() == KeyEvent.VK_ESCAPE) inputField.setText(lastCommand);
            destroyMenu();
            return;
        }
        switch(k.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                sendCommand();
                break;
            case KeyEvent.VK_KP_LEFT:
            case KeyEvent.VK_LEFT:
                worldRef.movePlayer(-1, 0);
                k.consume();
                break;
            case KeyEvent.VK_KP_RIGHT:
            case KeyEvent.VK_RIGHT:
                worldRef.movePlayer(1, 0);
                k.consume();
                break;
            case KeyEvent.VK_KP_UP:
            case KeyEvent.VK_UP:
                worldRef.movePlayer(0, -1);
                k.consume();
                break;
            case KeyEvent.VK_KP_DOWN:
            case KeyEvent.VK_DOWN:
                worldRef.movePlayer(0, 1);
                k.consume();
                break;
            case KeyEvent.VK_HOME:
                worldRef.movePlayer(-1, -1);
                k.consume();
                break;
            case KeyEvent.VK_PAGE_UP:
                worldRef.movePlayer(1, -1);
                k.consume();
                break;
            case KeyEvent.VK_PAGE_DOWN:
                worldRef.movePlayer(1, 1);
                k.consume();
                break;
            case KeyEvent.VK_END:
                worldRef.movePlayer(-1, 1);
                k.consume();
                break;
        }
    }

    public void mouseClicked(MouseEvent m) {
        sendCommand();
    }

    public void keyReleased(KeyEvent k) {
    }

    public void keyTyped(KeyEvent k) {
    }

    public void mouseEntered(MouseEvent m) {
    }

    public void mouseExited(MouseEvent m) {
    }

    public void mousePressed(MouseEvent m) {
    }

    public void mouseReleased(MouseEvent m) {
    }

    private void sendCommand() {
        String txt = inputField.getText();
        if (txt.length() == 0) {
            showDropDown();
            return;
        }
        if (!commandList.lastElement().equals(txt)) commandList.add(txt);
        inputField.setText("");
        if (txt.charAt(0) == '%') {
            txt = txt.substring(1);
            System.out.println("Sending: \"" + txt + "\"");
            try {
                worldRef.sendLine(txt);
            } catch (Exception ex) {
                System.out.println("Error while sending this command\n" + ex.toString());
            }
        } else if (txt.charAt(0) == ':') {
            worldRef.sendEmote(txt.substring(1));
        } else {
            worldRef.sendChat(txt);
        }
    }

    private void showDropDown() {
        lastCommand = inputField.getText();
        Border border = BorderFactory.createLineBorder(Color.BLACK, 1);
        if (dropDown != null) destroyMenu();
        Vector l2 = (Vector) commandList.clone();
        Collections.reverse(l2);
        theList = new JList(l2);
        theList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        dropDown = new JScrollPane(theList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Dimension d = new Dimension(170, 100);
        dropDown.setPreferredSize(d);
        final int x = game.getWidth() / 2 - 103;
        final int y = game.getHeight() + inputField.getBounds().y + inputField.getHeight() + 5;
        Insets i = getInsets();
        dropDown.setBounds(i.left + x, i.top + y, d.width, d.height);
        theList.setBorder(border);
        theList.addListSelectionListener(this);
        theList.addKeyListener(this);
        add(dropDown, BorderLayout.SOUTH);
        validate();
        theList.setSelectedIndex(0);
        theList.requestFocus();
    }

    public void valueChanged(ListSelectionEvent s) {
        final int i = commandList.size() - 1 - theList.getSelectedIndex();
        inputField.setText(commandList.get(i));
    }

    private void destroyMenu() {
        if (dropDown != null) {
            dropDown.remove(theList);
            theList = null;
            remove(dropDown);
            dropDown = null;
        }
        validate();
        repaint();
        inputField.requestFocus();
    }

    public void forceUpdate(boolean quickMovePlayer) {
        game.forceUpdate(quickMovePlayer);
        inventory.forceUpdate(quickMovePlayer);
    }

    /**
     * Display the given message.
     * @param s the message (e.g. chat) to display
     */
    private final int maxChatWidth = 60;

    public void displayMessage(String s) {
        String use = "";
        while (s.length() > maxChatWidth) {
            use += s.substring(0, maxChatWidth) + "\n";
            s = s.substring(maxChatWidth);
        }
        use += s;
        game.displayMessage(use);
        if (true) return;
        String current = chatField.getText();
        current += "\n" + use;
        chatField.setText(current);
        chatField.setSelectionStart(current.length());
    }

    /**
     * object has been clicked, so take appropriate action (e.g. display menu).
     * @param object The object which has been clicked (must not be null).
     * @param component The Component in which the click occured. This is required to draw the menu.
     * @param x X coordinate for menu.
     * @param y Y coordinate for menu.
     */
    protected void objectClicked(PositionedObject object, Component component, int x, int y) {
        synchronized (object) {
            if (currentVerb == null) {
                clickedObject = object;
                pMenu = new PopupMenu(object.getName());
                Map<String, Verb> verbs = object.getVerbs();
                for (Map.Entry<String, Verb> verb : verbs.entrySet()) {
                    MenuItem item = new MenuItem(verb.getValue().toString());
                    item.addActionListener(new PopupActionListener());
                    item.setName(verb.getKey());
                    pMenu.add(item);
                }
                add(pMenu);
                pMenu.show(component, x, y);
            } else {
                worldRef.callVerb(currentVerb, clickedObject, object);
                clickedObject = null;
                currentVerb = null;
            }
        }
    }

    private class PopupActionListener implements ActionListener {

        public void actionPerformed(ActionEvent a) {
            remove(pMenu);
            synchronized (clickedObject) {
                MenuItem clickedItem = (MenuItem) a.getSource();
                Verb verb = clickedObject.getVerbs().get(clickedItem.getName());
                if (verb.numArgs() == 2) {
                    currentVerb = verb;
                } else {
                    worldRef.callVerb(verb, clickedObject, null);
                    clickedObject = null;
                }
            }
        }
    }

    /**
     * @return true iff we are currently editing the map (i.e. the edit tab is selected)
     */
    protected boolean isEditing() {
        return rightPane.getSelectedIndex() == 1;
    }

    protected void mapClicked(int x, int y) {
        worldRef.editMap(x, y, layerCombo.getSelectedIndex() + 1, landTypeSelector.getSelectedType());
    }

    private class ExtendActionListener implements ActionListener {

        private char direction;

        private boolean unextend;

        public ExtendActionListener(char direction, boolean unextend) {
            this.direction = direction;
            this.unextend = unextend;
        }

        public void actionPerformed(ActionEvent e) {
            if (unextend) {
                worldRef.unextendMap(direction);
            } else {
                worldRef.extendMap(direction);
            }
        }
    }
}
