package dnl.net.netclip;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;
import dnl.logging.LightLog;
import dnl.ui.FileChooserUtils;

public class GetClipProtocolHandler extends IoHandlerAdapter {

    private ClipboardManager clipboardManager = new ClipboardManager();

    private TrayUI ui;

    /**
	 * 
	 * Constructs a new <code>GetClipProtocolHandler</code>
	 * 
	 * @param ui
	 */
    public GetClipProtocolHandler(TrayUI ui) {
        this.ui = ui;
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        SessionLog.error(session, "", cause);
        session.close();
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        try {
            if (message instanceof String) {
                String theMessage = (String) message;
                LightLog.info("Received clipboard from: " + session.getRemoteAddress() + ", type=string, size = " + theMessage.length());
                setClipboardContents(theMessage);
            } else if (message instanceof FileContent) {
                handleFileMessage(session, message);
            } else if (message instanceof ImageContent) {
                handleImageMessage(session, message);
            }
            session.close();
        } finally {
            posponeClosing();
            ui.hideProgress();
        }
    }

    private void posponeClosing() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ui.setProgressText("Done");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleImageMessage(IoSession session, Object message) {
        ImageContent theMessage = (ImageContent) message;
        LightLog.info("Received clipboard from: " + session.getRemoteAddress() + ", type=image, format=" + theMessage.getFormat());
        Image image = Toolkit.getDefaultToolkit().createImage(theMessage.getBytes());
        setClipboardContents(image);
    }

    private void handleFileMessage(IoSession session, Object message) {
        FileContent theMessage = (FileContent) message;
        LightLog.info("Received clipboard from: " + session.getRemoteAddress() + ", type=file, name=" + theMessage.getFileName());
        if (SystemUtils.IS_OS_UNIX) {
            handleFileMessageUnix(session, theMessage);
        } else {
            String tmpDirPath = System.getProperty("java.io.tmpdir");
            LightLog.info("Using tmp.dir: " + tmpDirPath);
            File tmpDir = new File(tmpDirPath);
            File f = new File(tmpDir, theMessage.getFileName());
            try {
                FileUtils.writeByteArrayToFile(f, theMessage.getBytes());
                File saveAs = FileChooserUtils.saveAs(null, f.getName());
                if (saveAs != null) {
                    try {
                        FileUtils.copyFile(f, saveAs);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleFileMessageUnix(IoSession session, FileContent theMessage) {
        int option = JOptionPane.showConfirmDialog(null, "Remote clipboard refers to a file.\nDo you wish to save it locally?");
        if (option != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            File saveAs = FileChooserUtils.saveAs(null, theMessage.getFileName());
            FileOutputStream os = new FileOutputStream(saveAs);
            if (saveAs != null) {
                try {
                    IOUtils.write(theMessage.getBytes(), os);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setClipboardContents(String aString) {
        StringSelection stringSelection = new StringSelection(aString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, clipboardManager);
    }

    public void setClipboardContents(File f) {
        if (SystemUtils.IS_OS_UNIX) {
        } else {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            FileSelection fileSelection = new FileSelection(f);
            clipboard.setContents(fileSelection, clipboardManager);
        }
    }

    public void setClipboardContents(Image image) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        ImageSelection imageSelection = new ImageSelection(image);
        clipboard.setContents(imageSelection, clipboardManager);
    }
}
