package barde.t4c;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.ResourceBundle;
import barde.writers.AbstractLogWriter;
import barde.log.Message;

/**
 * @author cbonar
 */
public class T4CClientWriter extends AbstractLogWriter {

    private PrintStream out;

    private String CC_AREA;

    private String CC_SELF;

    private String CC_SYSTEM;

    private String CC_WHISP;

    private String YOU_SAID;

    private String YOURSELF;

    private String WHISP_OUT;

    private String WHISP_IN;

    public T4CClientWriter(OutputStream os, ResourceBundle rc) {
        this.out = new PrintStream(os);
        this.CC_AREA = rc.getString(T4CClientReader.RC_CHANNEL_AREA);
        this.CC_SELF = rc.getString(T4CClientReader.RC_CHANNEL_SELF);
        this.CC_SYSTEM = rc.getString(T4CClientReader.RC_CHANNEL_SYSTEM);
        this.CC_WHISP = rc.getString(T4CClientReader.RC_CHANNEL_WHISP);
        this.YOU_SAID = rc.getString(T4CClientReader.RC_CHANNEL_SELF + ".startsWith");
        this.YOURSELF = rc.getString(T4CClientReader.RC_SOURCE_YOURSELF);
        this.WHISP_OUT = rc.getString(T4CClientReader.RC_CHANNEL_WHISP_OUT + ".startsWith");
        this.WHISP_IN = rc.getString(T4CClientReader.RC_CHANNEL_WHISP_IN + ".startsWith");
    }

    /**
	 * Convenient constructor.
	 * Uses the default locale to find the ResourceBundle.
	 */
    public T4CClientWriter(OutputStream os) {
        this(os, ResourceBundle.getBundle("barde_t4c", Locale.getDefault()));
    }

    /**
	 * @see barde.writers.LogWriter#write(barde.log.Message)
	 */
    public void write(Message message) throws IOException {
        String date = T4CMessage.dateFormat.format(message.getDate());
        String channel = message.getChannel();
        String source = message.getAvatar();
        String content = message.getContent();
        String padding = content.length() > 0 ? " " : "";
        out.print(date + "-- ");
        if (channel.equals(CC_AREA)) out.print("{" + source + "}\":\"" + padding + content); else if (channel.equals(CC_SELF)) out.print(YOU_SAID + padding + content); else if (channel.equals(CC_SYSTEM)) out.print(content); else if (channel.equals(CC_WHISP)) {
            if (source.equals(YOURSELF)) out.print(WHISP_OUT + " " + content); else out.print(source + WHISP_IN + padding + content);
        } else out.print("[\"" + channel + "\"] \"" + source + "\":" + padding + content);
        out.println();
    }
}
