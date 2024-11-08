package ch.comtools.servermanager.ssh.ui.terminal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import ch.comtools.jsch.Channel;
import ch.comtools.servermanager.common.resource.ResourceManager;
import ch.comtools.servermanager.ssh.SSH;

/**
 * @author Roger Dudler <roger.dudler@gmail.com>
 * @version $Id: SecureShellTerminal.java 70 2007-04-23 07:57:10Z roger.dudler $
 */
public class SecureShellTerminal {

    private StyledText styledText;

    private InputStream in;

    private PipedOutputStream out;

    private Channel channel;

    /**
	 * Create a new secure shell terminal widget.
	 * @param parent
	 */
    public SecureShellTerminal(Composite parent) {
        this.styledText = new StyledText(parent, SWT.READ_ONLY | SWT.V_SCROLL);
        this.init();
    }

    /**
	 * Initialize current {@link SecureShellTerminal}.
	 */
    private void init() {
        this.styledText.setBackground(ResourceManager.getInstance().getColor(0, 0, 0));
        this.styledText.setForeground(ResourceManager.getInstance().getColor(187, 187, 187));
        this.styledText.setFont(ResourceManager.getInstance().getFont("Courier New", 10, SWT.NORMAL));
        this.styledText.setEditable(false);
        this.styledText.setWordWrap(true);
    }

    /**
	 * Set secure {@link Channel} to use.
	 * @param channel
	 */
    public void setChannel(final Channel channel) {
        this.channel = channel;
        try {
            in = channel.getInputStream();
            out = new PipedOutputStream();
            PipedInputStream pipedInput = new PipedInputStream(out);
            BufferedInputStream input = new BufferedInputStream(pipedInput);
            channel.setInputStream(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.styledText.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                int c = e.character;
                if (c != '\n' && c != '\r') {
                    try {
                        if (c > 0) {
                            out.write(c);
                            out.flush();
                        } else {
                            System.out.println(e.keyCode);
                            if (e.keyCode == 16777217) {
                                out.write(SSH.UP);
                                out.flush();
                            }
                            if (e.keyCode == 16777218) {
                                out.write(SSH.DOWN);
                                out.flush();
                            }
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                e.doit = false;
            }
        });
        this.styledText.addTraverseListener(new TraverseListener() {

            public void keyTraversed(TraverseEvent e) {
                int c = e.character;
                try {
                    if (c > 0) {
                        out.write(c);
                        out.flush();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.doit = false;
            }
        });
        this.styledText.addMouseListener(new MouseAdapter() {

            public void mouseUp(MouseEvent e) {
                if (e.button == 3) {
                    Clipboard clipboard = new Clipboard(styledText.getDisplay());
                    TextTransfer plainTextTransfer = TextTransfer.getInstance();
                    String text = (String) clipboard.getContents(plainTextTransfer, DND.CLIPBOARD);
                    try {
                        out.write(text.getBytes());
                        out.flush();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        new RemoteConsumer(this.in, this.styledText).start();
    }

    /**
	 * Get secure {@link Channel} used in this terminal.
	 * @return
	 */
    public Channel getChannel() {
        return channel;
    }

    /**
	 * Get {@link StyledText} widget used in this terminal.
	 * @return
	 */
    public Control getControl() {
        return this.styledText;
    }
}
