import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Observable;
import java.util.ResourceBundle;
import ar.com.jkohen.awt.*;
import ar.com.jkohen.irc.Channel;
import ar.com.jkohen.irc.MircMessage;
import ar.com.jkohen.util.ConfigurationProperties;
import ar.com.jkohen.util.Resource;
import com.splendid.awtchat.SmileyTextArea;

class PrivateWindow extends ChatWindow implements ActionListener, MouseListener {

    private TextFieldHistory entry;

    private ImageButton whois;

    private ImageButton close;

    private TextAttributePicker text_attr_picker;

    private boolean text_attr_memory;

    private EIRC eirc;

    private String user;

    private ResourceBundle images;

    public PrivateWindow(EIRC eirc, String u, String title) {
        this(eirc, u, title, Locale.getDefault());
    }

    public PrivateWindow(EIRC eirc, String u, String title, Locale locale) {
        super(title, locale);
        this.eirc = eirc;
        this.user = u;
        this.images = ResourceBundle.getBundle("Images", locale);
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gb);
        gbc.gridy = 0;
        whois = new ImageButton();
        whois.setImage(ImageButton.ENTERED, getResourceImage("private.whois.entered"));
        whois.setImage(ImageButton.EXITED, getResourceImage("private.whois.exited"));
        whois.setImage(ImageButton.PRESSED, getResourceImage("private.whois.pressed"));
        gb.setConstraints(whois, gbc);
        add(whois);
        close = new ImageButton();
        close.setImage(ImageButton.ENTERED, getResourceImage("private.close.entered"));
        close.setImage(ImageButton.EXITED, getResourceImage("private.close.exited"));
        close.setImage(ImageButton.PRESSED, getResourceImage("private.close.pressed"));
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(close, gbc);
        add(close);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gb.setConstraints(text_canvas, gbc);
        add(text_canvas);
        gbc.weighty = 0.0;
        gbc.gridy++;
        this.text_attr_picker = new TextAttributePicker(SmileyTextArea.getColorPalette(), 16);
        gb.setConstraints(text_attr_picker, gbc);
        add(text_attr_picker);
        IndexedColorPicker color_picker = (IndexedColorPicker) text_attr_picker.getColorPicker();
        color_picker.setDisposition(0, 1);
        color_picker.setItemSize(16, 12);
        color_picker.setGap(2, 2);
        gbc.gridy++;
        this.entry = new TextFieldHistory();
        gb.setConstraints(entry, gbc);
        add(entry);
        whois.addMouseListener(this);
        close.addMouseListener(this);
        entry.addActionListener(this);
        text_attr_picker.addActionListener(this);
    }

    public void requestFocus() {
        entry.requestFocus();
    }

    protected String getNick() {
        return eirc.getNick();
    }

    protected void visitURL(URL url) {
        eirc.visitURL(url);
    }

    protected void joinChannel(String name) {
        if (null == eirc.getChannel(name)) {
            String p[] = { name };
            eirc.sendMessage("join", p);
        } else {
            eirc.showPanel(name);
        }
    }

    public void update(Observable o, Object arg) {
        super.update(o, arg);
        ConfigurationProperties props = (ConfigurationProperties) o;
        if (null == arg || arg.equals("text_attr_memory")) {
            this.text_attr_memory = props.getBoolean("text_attr_memory");
        }
    }

    /**
     * Sends the specified text to the peer.
     *
     * @param text the text to send to the other user.
     */
    public void sendText(String text) {
        String[] p = { user, text };
        eirc.sendMessage("privmsg", p);
    }

    private Image getResourceImage(String name) {
        String resource_name = images.getString(name);
        try {
            return Resource.createImage(resource_name, this);
        } catch (IOException e) {
            System.err.println(e);
        }
        return null;
    }

    public void setTextBackground(Color c) {
        text_canvas.setBackground(c);
        entry.setBackground(c);
    }

    public void setTextForeground(Color c) {
        text_canvas.setForeground(c);
        entry.setForeground(c);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String s) {
        this.user = s;
    }

    public void actionPerformed(ActionEvent ev) {
        Object comp = ev.getSource();
        if (comp.equals(entry)) {
            String text = entry.getText();
            if (text.length() <= 0) {
                return;
            }
            if (text.charAt(0) == '/') {
                text = text.substring(1);
                if (text.trim().length() > 0) {
                    eirc.sendCommand(text, this);
                }
            } else {
                printMyPrivmsg(entry.getText());
                sendText(entry.getText());
            }
            entry.setText("");
        } else if (comp.equals(text_attr_picker)) {
            String text = entry.getText();
            String new_text = ev.getActionCommand();
            int pos = entry.getCaretPosition();
            text = text.substring(0, pos).concat(new_text).concat(text.substring(pos));
            entry.setText(text);
            entry.setCaretPosition(pos + new_text.length());
        }
    }

    public void mouseClicked(MouseEvent ev) {
        Component comp = ev.getComponent();
        if (comp.equals(whois)) {
            String p[] = { user };
            if (null != p[0]) {
                eirc.sendMessage("whois", p);
            }
        } else if (comp.equals(close)) {
            dispose();
        }
    }

    public void mousePressed(MouseEvent ev) {
    }

    public void mouseReleased(MouseEvent ev) {
    }

    public void mouseEntered(MouseEvent ev) {
    }

    public void mouseExited(MouseEvent ev) {
    }
}
