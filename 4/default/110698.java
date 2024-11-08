import java.awt.TextComponent;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.StringReader;

public class CutAndPaste implements ClipboardOwner {

    public CutAndPaste() {
        sbuf = new StringBuffer();
        sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    public void setSysClip(Clipboard clipboard) {
        sysClip = clipboard;
    }

    public Clipboard getSysClip() {
        return sysClip;
    }

    public void setContent(String s) {
        StringSelection stringselection = new StringSelection(s);
        sysClip.setContents(stringselection, this);
    }

    public String getContent() {
        sbuf.setLength(0);
        char ac[] = new char[1024];
        Transferable transferable = sysClip.getContents(this);
        if (transferable == null) {
            return "";
        }
        if (!transferable.isDataFlavorSupported(DataFlavor.plainTextFlavor)) {
            return "Unsupported Flavor";
        }
        Object obj = null;
        try {
            StringReader stringreader = (StringReader) transferable.getTransferData(DataFlavor.plainTextFlavor);
            if (stringreader == null) {
                return "";
            }
            content = stringreader;
            int i;
            while ((i = stringreader.read(ac, 0, 1024)) != -1) {
                sbuf.append(ac, 0, i);
            }
        } catch (Exception _ex) {
        }
        return sbuf.toString();
    }

    public StringReader getContentReader() {
        return content;
    }

    public StringBuffer getContentBuffer() {
        return sbuf;
    }

    public void pasteText(TextComponent textcomponent) {
        String s = textcomponent.getText();
        textcomponent.getCaretPosition();
        int i = textcomponent.getSelectionStart();
        int j = textcomponent.getSelectionEnd();
        textcomponent.setText(s.substring(0, i) + getContent() + s.substring(j, s.length()));
        textcomponent.setCaretPosition(i + sbuf.length());
    }

    public void copyText(TextComponent textcomponent) {
        setContent(textcomponent.getSelectedText());
    }

    public void transferToClipboard(TextComponent textcomponent) {
        int i = textcomponent.getSelectionStart();
        int j = textcomponent.getSelectionEnd();
        if (i == j) {
            setContent(textcomponent.getText());
            return;
        } else {
            setContent(textcomponent.getSelectedText());
            return;
        }
    }

    public void transferFromClipboard(TextComponent textcomponent) {
        textcomponent.setText(getContent());
    }

    public void cutText(TextComponent textcomponent) {
        setContent(textcomponent.getSelectedText());
        int i = textcomponent.getSelectionStart();
        int j = textcomponent.getSelectionEnd();
        if (i != j) {
            String s = textcomponent.getText();
            textcomponent.setText(s.substring(0, i) + s.substring(j, s.length()));
        }
        textcomponent.setCaretPosition(i);
    }

    public String toString() {
        return sbuf.toString();
    }

    public void lostOwnership(Clipboard clipboard, Transferable transferable) {
    }

    protected Clipboard sysClip;

    protected StringReader content;

    protected StringBuffer sbuf;
}
