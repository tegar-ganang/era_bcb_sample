package org.iceinn.iceeditor.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import org.iceinn.iceeditor.document.IceCaretListener;
import org.iceinn.iceeditor.document.IceConstants;
import org.iceinn.iceeditor.document.IceStyledDocument;
import org.iceinn.iceeditor.document.IceTask;
import org.iceinn.tools.Logger;
import org.iceinn.tools.notification.NotificationCenter;
import org.iceinn.tools.notification.NotificationListener;

/**
 * 
 * @author Lionel FLAHAUT
 * 
 */
@SuppressWarnings("serial")
public class IceEditorPane extends JTextPane implements NotificationListener {

    public IceEditorPane(IceStyledDocument document) {
        super();
        setEditorKit(new NumberedEditorKit());
        setDocument(document);
        if (Logger.isLoggingDebug()) {
            Logger.dLog(getDocument().toString());
            Logger.dLog(getEditorKit().toString());
        }
        addCaretListener(new IceCaretListener(document));
        setEditable(true);
        NotificationCenter.current().registerNewListener(IceConstants.CARET_OFFSET, this);
    }

    public IceEditorPane(IceStyledDocument document, String pathToFile) {
        this(document);
        loadDocument(pathToFile);
    }

    /**
	 * Load a new document
	 * 
	 * @param pathToFile
	 */
    public void loadDocument(String pathToFile) {
        IceStyledDocument doc = (IceStyledDocument) getDocument();
        doc.getUndoListener().setActive(false);
        doc.getIceListener().setActive(true);
        try {
            doc.remove(0, doc.getLength());
            boolean bufferAll = false;
            if (bufferAll) {
                SwingUtilities.invokeLater(new LoadRunner(pathToFile, doc));
            } else {
                File f = new File(pathToFile);
                InputStream stream = new FileInputStream(f);
                NotificationCenter.current().notifyChange(IceConstants.NEW_TASK, this.getClass(), new IceTask("Load file", (int) f.length(), -1));
                byte[] readed = new byte[2048];
                int nbRead = 0;
                int offset = 0;
                while ((nbRead = stream.read(readed)) >= 0) {
                    doc.insertString(offset, new String(readed, 0, nbRead), null);
                    offset += nbRead;
                    NotificationCenter.current().notifyChange(IceConstants.STEP_TASK, this.getClass(), Integer.valueOf(nbRead));
                }
                NotificationCenter.current().notifyChange(IceConstants.END_TASK, this.getClass(), null);
                stream.close();
                doc.getUndoListener().setActive(true);
                doc.getDocumentParser().reset();
            }
        } catch (IOException e) {
            if (Logger.isLoggingError()) Logger.eLog("Exception while loading", e);
        } catch (BadLocationException e) {
            if (Logger.isLoggingError()) Logger.eLog("Exception while loading", e);
        }
    }

    static class LoadRunner implements Runnable {

        private byte[] data;

        private IceStyledDocument document;

        public LoadRunner(String pathToFile, IceStyledDocument document) throws IOException {
            super();
            FileInputStream input = new FileInputStream(pathToFile);
            FileChannel channel = input.getChannel();
            int fileLength = (int) channel.size();
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
            this.data = new byte[buffer.limit()];
            buffer.get(this.data);
            buffer.clear();
            buffer = null;
            this.document = document;
        }

        public void run() {
            try {
                document.insertString(0, new String(data, 0, data.length), null);
                document.getUndoListener().setActive(true);
            } catch (BadLocationException e) {
                if (Logger.isLoggingError()) Logger.eLog("cannot insert string in document.", e);
            }
        }
    }

    public void handleNotification(String key, Class notifier, Object newValue) {
        repaint();
    }
}
