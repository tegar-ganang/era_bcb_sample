import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import ar.com.jkohen.awt.TextFieldHistory;
import ar.com.jkohen.irc.Channel;

public class StatusWindow extends OutputWindow implements ActionListener {

    private EIRC eirc;

    private String title;

    private TextFieldHistory entry;

    private static final ResourceBundle msg = ResourceBundle.getBundle("message");

    protected static final MessageFormat UNMANGLED = new MessageFormat(msg.getString("unmangled"));

    public StatusWindow(EIRC eirc, String title) {
        this(eirc, title, Locale.getDefault());
    }

    public StatusWindow(EIRC eirc, String title, Locale locale) {
        super(title, locale);
        this.eirc = eirc;
        this.title = title;
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gb);
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gb.setConstraints(text_canvas, gbc);
        add(text_canvas);
        gbc.weighty = 0.0;
        gbc.gridy = 1;
        entry = new TextFieldHistory();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(entry, gbc);
        add(entry);
        entry.addActionListener(this);
    }

    public void printUnmangled(String s) {
        Object[] o = { s };
        text_canvas.append(UNMANGLED.format(o), true);
        postTextEvent();
    }

    public void requestFocus() {
        entry.requestFocus();
    }

    public void setTextBackground(Color c) {
        text_canvas.setBackground(c);
        entry.setBackground(c);
    }

    public void setTextForeground(Color c) {
        text_canvas.setForeground(c);
        entry.setForeground(c);
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

    /**
     * This method does nothing. The status window is not an actual peer,
     * so sending it text wouldn't make a lot of sense.
     *
     * @param text the text to not send.
     */
    public void sendText(String text) {
    }

    public void actionPerformed(ActionEvent ev) {
        String text = entry.getText();
        if (text.length() <= 0) {
            return;
        }
        if (text.charAt(0) == '/') {
            text = text.substring(1);
        }
        if (text.trim().length() > 0) {
            eirc.sendCommand(text, this);
        }
        entry.setText("");
    }
}
