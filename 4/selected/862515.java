package be.uclouvain.gsi.smartcard.eid.swing.action;

import java.awt.event.ActionEvent;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import be.uclouvain.gsi.smartcard.eid.model.EID;
import be.uclouvain.gsi.smartcard.eid.swing.view.MainFrame;
import be.uclouvain.gsi.smartcard.util.Logging;

@SuppressWarnings("serial")
public class SaveDumpAction extends AbstractAction {

    final JFileChooser fc = new JFileChooser();

    MainFrame app;

    public SaveDumpAction(MainFrame app, String text) {
        super(text);
        this.app = app;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        DataOutputStream dos;
        EID eid = app.getEid();
        if (eid == null) {
            JOptionPane.showMessageDialog(app, "Nothing to save.");
            return;
        }
        int returnVal = fc.showSaveDialog(app);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file.exists()) {
                int confirm = JOptionPane.showConfirmDialog(app, "File already exist. Overwrite?");
                if (confirm != JOptionPane.YES_OPTION) return;
            }
            try {
                dos = new DataOutputStream(new FileOutputStream(file));
                try {
                    dos.writeUTF(eid.getData().toString());
                    dos.writeUTF(eid.getAddress().toString());
                    dos.writeInt(eid.getPicture().length);
                    dos.write(eid.getPicture());
                    dos.flush();
                    Logging.info("Dump saved to " + file.getPath());
                } finally {
                    dos.close();
                }
            } catch (IOException e) {
                Logging.severe(e);
            }
        }
    }
}
