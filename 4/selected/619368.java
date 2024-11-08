package lt.inkredibl.iit.action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import lt.inkredibl.iit.FileChooserSource;
import lt.inkredibl.iit.IITCFileFilter;
import lt.inkredibl.iit.ImgComponent;

@SuppressWarnings("serial")
public class ActSave extends AbstractAction {

    private ImgComponent _ic;

    private FileChooserSource _fcs = FileChooserSource.inst();

    private static final FileFilter _fileFilter = IITCFileFilter.inst();

    protected ActSave(ImgComponent ic) {
        _ic = ic;
        putValue(Action.NAME, "Save...");
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
    }

    public static ActSave inst(ImgComponent ic) {
        return new ActSave(ic);
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = makeFileChooser();
        if (JFileChooser.APPROVE_OPTION == fc.showSaveDialog(null)) {
            File f = fc.getSelectedFile();
            if (f != null) {
                try {
                    if (f.canWrite()) {
                        if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(sourceToComponent(e), "File already exists. Overwrite?")) {
                            return;
                        }
                    } else {
                        String fname = f.getName();
                        int pos = fname.lastIndexOf('.');
                        if (pos == -1) {
                            f = new File(f + ".iitc");
                        }
                        f.createNewFile();
                    }
                    FileOutputStream out = new FileOutputStream(f);
                    _ic.getState().store(out, "IIT Contour File");
                } catch (IOException e2) {
                    JOptionPane.showMessageDialog(sourceToComponent(e), e2.getMessage(), e2.getClass().getName(), JOptionPane.ERROR_MESSAGE);
                    ;
                }
            }
        }
    }

    private Component sourceToComponent(ActionEvent e) {
        return e.getSource() instanceof Component ? (Component) e.getSource() : null;
    }

    private JFileChooser makeFileChooser() {
        JFileChooser fc = _fcs.getFileChooser();
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setFileFilter(_fileFilter);
        return fc;
    }
}
