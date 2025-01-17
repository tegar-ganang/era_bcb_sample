package org.adempiere.webui.editor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import org.adempiere.webui.component.FilenameBox;
import org.adempiere.webui.event.ValueChangeEvent;
import org.compiere.model.GridField;
import org.compiere.util.CLogger;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Fileupload;

/**
 * 
 * @author Low Heng Sin
 *
 */
public class WFilenameEditor extends WEditor {

    private static final String[] LISTENER_EVENTS = { Events.ON_CLICK, Events.ON_CHANGE };

    private static final CLogger log = CLogger.getCLogger(WFilenameEditor.class);

    public WFilenameEditor(GridField gridField) {
        super(new FilenameBox(), gridField);
        getComponent().setButtonImage("/images/Open16.png");
        getComponent().addEventListener(Events.ON_CLICK, this);
    }

    @Override
    public FilenameBox getComponent() {
        return (FilenameBox) component;
    }

    @Override
    public void setValue(Object value) {
        if (value == null) {
            getComponent().setText("");
        } else {
            getComponent().setText(String.valueOf(value));
        }
    }

    @Override
    public Object getValue() {
        return getComponent().getText();
    }

    @Override
    public String getDisplay() {
        return getComponent().getText();
    }

    @Override
    public boolean isReadWrite() {
        return getComponent().isEnabled();
    }

    @Override
    public void setReadWrite(boolean readWrite) {
        getComponent().setEnabled(readWrite);
    }

    public void onEvent(Event event) {
        if (Events.ON_CHANGE.equals(event.getName())) {
            ValueChangeEvent changeEvent = new ValueChangeEvent(this, this.getColumnName(), getComponent().getText(), getComponent().getText());
            fireValueChange(changeEvent);
        } else if (Events.ON_CLICK.equals(event.getName())) {
            cmd_file();
        }
    }

    /**
	 *  Load file
	 */
    private void cmd_file() {
        Media file = null;
        try {
            file = Fileupload.get(true);
            if (file == null) return;
        } catch (InterruptedException e) {
            log.warning(e.getLocalizedMessage());
            return;
        }
        FileOutputStream fos = null;
        String fileName = null;
        try {
            File tempFile = File.createTempFile("compiere_", "_" + file.getName());
            fileName = tempFile.getAbsolutePath();
            fos = new FileOutputStream(tempFile);
            byte[] bytes = null;
            if (file.inMemory()) {
                bytes = file.getByteData();
            } else {
                InputStream is = file.getStreamData();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[1000];
                int byteread = 0;
                while ((byteread = is.read(buf)) != -1) baos.write(buf, 0, byteread);
                bytes = baos.toByteArray();
            }
            fos.write(bytes);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getLocalizedMessage(), e);
            return;
        } finally {
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
            }
        }
        getComponent().setText(fileName);
    }

    public String[] getEvents() {
        return LISTENER_EVENTS;
    }
}
