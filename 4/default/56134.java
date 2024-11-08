import java.io.*;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Thewinator
 */
public class FlameHandler {

    private ArrayList<Flame> flames = new ArrayList<Flame>();

    private Object[] prevFlames;

    private mainEditor main;

    private int selectedFlame = -1;

    private Renderer renderQueue;

    public FlameHandler(mainEditor main, Renderer renderQueue) {
        this.renderQueue = renderQueue;
        this.main = main;
    }

    public void importFlame() {
        try {
            Debugger.appendAction("Starting importFlame");
            JFileChooser pickFile = new JFileChooser();
            pickFile.setCurrentDirectory(new File(Settings.lastImportedFolder));
            pickFile.setFileFilter(new FlameFilter());
            pickFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int retVal = pickFile.showOpenDialog(main);
            if (retVal == JFileChooser.APPROVE_OPTION) {
                try {
                    FlameSelector selector = new FlameSelector(pickFile.getSelectedFile(), this, main, renderQueue);
                    if (selector.hasFlame()) {
                        prevFlames = flames.toArray();
                        flames.add(selector.getFlame());
                        main.refreshThumbs();
                        Settings.lastImportedFolder = pickFile.getCurrentDirectory().toString();
                        Settings.saveSettings();
                        Debugger.appendLog("Succesfull: importFlame");
                    }
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(main, "Invalid .flame file", "Error opening file", JOptionPane.ERROR_MESSAGE);
                    Debugger.appendLog("Failed: importFlame");
                    Debugger.storeException(ex);
                }
            }
            Debugger.appendAction("Ending importFlame");
        } catch (NullPointerException ex) {
            Debugger.storeException(ex);
        }
    }

    public void openSequence() {
        try {
            Debugger.appendAction("Starting openSequence");
            JFileChooser pickFile = new JFileChooser();
            pickFile.setCurrentDirectory(new File(Settings.lastOpenedFolder));
            pickFile.setFileFilter(new SequenceFilter());
            pickFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int retVal = pickFile.showOpenDialog(main);
            if (retVal == JFileChooser.APPROVE_OPTION) {
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                ArrayList<Flame> sequenceFlames = new Flame(docBuilder.parse(pickFile.getSelectedFile()), renderQueue).split();
                flames.clear();
                flames.addAll(sequenceFlames);
                Settings.lastOpenedFolder = pickFile.getCurrentDirectory().toString();
                Settings.saveSettings();
                Debugger.appendLog("Succesfull: openSequence");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(main, "Invalid .seq file", "Error opening file", JOptionPane.ERROR_MESSAGE);
            Debugger.appendLog("Failed: openSequence");
            Debugger.storeException(ex);
        } catch (SAXException ex) {
            JOptionPane.showMessageDialog(main, "Invalid .seq file", "Error opening file", JOptionPane.ERROR_MESSAGE);
            Debugger.appendLog("Failed: openSequence");
            Debugger.storeException(ex);
        } catch (ParserConfigurationException ex) {
            Debugger.appendLog("Failed: openSequence");
            Debugger.storeException(ex);
        } catch (NullPointerException ex) {
            Debugger.appendLog("Failed: openSequence");
            Debugger.storeException(ex);
        }
        main.refreshThumbs();
        Debugger.appendAction("Ending openSequence");
    }

    public void setSelected(FFrame frame) {
        for (int i = 0; i < flames.size(); i++) {
            if (flames.get(i).getThumb() == frame) {
                selectedFlame = i;
            }
        }
    }

    public int getFlameCount() {
        return flames.size();
    }

    public FFrame[] getThumbs() {
        FFrame[] frames = new FFrame[flames.size()];
        for (int i = 0; i < flames.size(); i++) {
            frames[i] = flames.get(i).getThumb();
            main.pBarStep();
        }
        return frames;
    }

    public ImageIcon[] getIcons() {
        main.startPBar(flames.size(), "Rendering previews");
        ImageIcon[] icons = new ImageIcon[flames.size()];
        for (int i = 0; i < flames.size(); i++) {
            icons[i] = flames.get(i).getPreview();
            main.pBarStep();
        }
        main.pBarDone();
        Debugger.appendLog("Succesfull: getIcons");
        return icons;
    }

    public boolean hasSelection() {
        if (selectedFlame > -1) {
            return true;
        }
        return false;
    }

    public void duplicate() throws IndexOutOfBoundsException {
        Debugger.appendAction("Starting duplicate");
        prevFlames = flames.toArray();
        flames.add(selectedFlame, flames.get(selectedFlame).duplicateInstance());
        Debugger.appendAction("Ending duplicate");
    }

    public void delete() {
        Debugger.appendAction("Starting delete");
        prevFlames = flames.toArray();
        flames.remove(selectedFlame);
        selectedFlame = -1;
        Debugger.appendAction("Ending delete");
    }

    public void clear() {
        Debugger.appendAction("Starting clean");
        prevFlames = null;
        for (Flame flame : flames) {
            flame.clear();
        }
        flames.clear();
        flames.trimToSize();
        selectedFlame = -1;
        Debugger.appendAction("Ending clean");
    }

    public void rotate() {
        RotateDialog rotate = new RotateDialog(main);
        if (rotate.hasRotation()) {
            ArrayList<Flame> rotated = new AnimateHandler(renderQueue).rotate(rotate, flames.get(selectedFlame).getDomTree());
            prevFlames = flames.toArray();
            flames.addAll(selectedFlame, rotated);
            flames.remove(selectedFlame + rotated.size());
            Debugger.appendLog("Succesfull: rotate");
        }
    }

    public boolean hasNext() {
        return selectedFlame < flames.size() - 1;
    }

    public boolean hasPrevious() {
        return selectedFlame > 0;
    }

    public String getSequence() {
        String sequence = "<Flames name=\"Flam3Animator\">";
        for (int i = 0; i < flames.size(); i++) {
            flames.get(i).setTime(i);
            sequence = sequence + "\r\n" + flames.get(i).getRawDomTree();
        }
        sequence += "\r\n</Flames>";
        Debugger.appendLog("Succesfull: getSequence (" + sequence.length() + " bytes)");
        return sequence;
    }

    public int getSelectedIndex() {
        return selectedFlame;
    }

    public Flame getFlameAtIndex(int index) {
        return flames.get(index);
    }

    public void saveSequence() {
        Debugger.appendAction("Starting saveSequence");
        if (flames.size() > 0) {
            JFileChooser pickFile = new JFileChooser();
            pickFile.setCurrentDirectory(new File(Settings.lastSavedFolder));
            pickFile.setFileFilter(new SequenceFilter());
            pickFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int retVal = pickFile.showSaveDialog(main);
            if (retVal == JFileChooser.APPROVE_OPTION) {
                File seq = pickFile.getSelectedFile();
                if (!(new SequenceFilter().accept(seq))) {
                    seq = new File(seq.toString() + ".seq");
                }
                if (seq.exists()) {
                    int overwrite = JOptionPane.showConfirmDialog(main, "File already exists, overwrite it?", "File exists", JOptionPane.YES_NO_OPTION);
                    if (overwrite == JOptionPane.YES_OPTION) {
                        seq.delete();
                        Debugger.appendLog("Status: saveSequence (File overwrite allowed)");
                    } else {
                        Debugger.appendLog("Canceled: saveSequence (No overwrite allowed)");
                        return;
                    }
                }
                PrintWriter pw;
                try {
                    pw = new PrintWriter(seq);
                    pw.println(getSequence());
                    pw.close();
                    Settings.lastSavedFolder = pickFile.getCurrentDirectory().toString();
                    Settings.saveSettings();
                    Debugger.appendLog("Succesfull: saveSequence");
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(main, "Error saving to file: " + seq, "Error saving sequence", JOptionPane.ERROR_MESSAGE);
                    Debugger.appendLog("Failed: saveSequence");
                    Debugger.storeException(ex);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(main, "I/O Exception while saving: " + ex.getMessage(), "Error saving sequence", JOptionPane.ERROR_MESSAGE);
                    Debugger.appendLog("Failed: saveSequence");
                    Debugger.storeException(ex);
                }
            }
        } else {
            JOptionPane.showMessageDialog(main, "Sequence is empty", "Error saving sequence", JOptionPane.ERROR_MESSAGE);
            Debugger.appendLog("Failed: openSequence");
        }
        Debugger.appendAction("Ending saveSequence");
    }

    public void tween() {
        TweenDialog td = new TweenDialog(main, this, renderQueue);
        if (td.hasFlames()) {
            prevFlames = flames.toArray();
            flames.addAll(td.getInsertIndex(), td.getFlames());
            Debugger.appendLog("Succesfull: tween");
        }
        main.refreshThumbs();
    }

    public void moveRight() {
        Debugger.appendAction("Starting moveRight");
        if (hasSelection() && hasNext()) {
            prevFlames = flames.toArray();
            Flame swapFlame = flames.get(selectedFlame);
            flames.set(selectedFlame, flames.get(selectedFlame + 1));
            flames.set(selectedFlame + 1, swapFlame);
            selectedFlame++;
            Debugger.appendLog("Succesfull: moveRight");
        }
        Debugger.appendAction("Ending moveRight");
    }

    public void moveLeft() {
        Debugger.appendAction("Starting moveLeft");
        if (hasSelection() && hasPrevious()) {
            prevFlames = flames.toArray();
            Flame swapFlame = flames.get(selectedFlame);
            flames.set(selectedFlame, flames.get(selectedFlame - 1));
            flames.set(selectedFlame - 1, swapFlame);
            selectedFlame--;
            Debugger.appendLog("Succesfull: moveLeft");
        }
        Debugger.appendAction("Ending moveLeft");
    }

    public void undo() {
        Debugger.appendAction("Starting undo");
        if (prevFlames != null) {
            Object[] swap = flames.toArray();
            flames.clear();
            for (Object f : prevFlames) {
                flames.add((Flame) f);
            }
            prevFlames = swap;
            Debugger.appendLog("Succesfull: undo");
        }
        Debugger.appendAction("Ending undo");
    }

    public boolean hasUndo() {
        return (prevFlames != null);
    }
}
