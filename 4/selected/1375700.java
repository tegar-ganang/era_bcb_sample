package org.qfirst.batavia.editor;

import java.awt.Container;
import javax.swing.*;
import org.ujac.ui.editor.*;
import org.qfirst.batavia.editor.actions.*;
import org.qfirst.vfs.*;
import java.io.*;
import org.apache.log4j.*;
import org.qfirst.batavia.utils.*;
import java.awt.event.WindowAdapter;
import java.awt.event.*;
import org.qfirst.batavia.ui.docking.*;
import java.awt.Window;
import java.awt.GridLayout;
import java.awt.BorderLayout;

public class EditorFrame extends JFrame implements DockableWindow {

    private JMenuBar menuBar = new JMenuBar();

    private TextArea textArea = new TextArea();

    private JPanel top;

    private AbstractFile file = null;

    private Logger logger = Logger.getLogger(getClass());

    private String lastSaved = "";

    private boolean editable;

    public EditorFrame(boolean editable) {
        this.editable = editable;
        initComponents();
        setSize(800, 600);
    }

    private void initComponents() {
        Container container = getContentPane();
        top = new JPanel();
        container.setLayout(new GridLayout(1, 1));
        container.add(top);
        top.setLayout(new BorderLayout());
        top.add(new DockBar(this), BorderLayout.NORTH);
        textArea.setEditable(editable);
        textArea.setEOLMarkersPainted(false);
        top.add(new JScrollPane(textArea), BorderLayout.CENTER);
        setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);
        JMenuItem newMenuItem = new JMenuItem(new NewFileAction(this));
        JMenuItem saveMenuItem = new JMenuItem(new SaveFileAction(this));
        JMenuItem saveAsMenuItem = new JMenuItem(new SaveAsAction(this));
        fileMenu.add(newMenuItem);
        if (editable) {
            fileMenu.add(saveMenuItem);
            fileMenu.add(saveAsMenuItem);
        }
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent evt) {
                String text = textArea.getText();
                if (getFile() == null || !text.equals(lastSaved)) {
                    int reply = JOptionPane.showConfirmDialog(EditorFrame.this, "Save file?", "Save file?", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (reply == JOptionPane.YES_OPTION) {
                        if (getFile() != null || askFileName()) {
                            if (saveFile()) {
                                logger.debug("Saved.");
                            } else {
                                logger.debug("Failed to save");
                                return;
                            }
                        } else {
                            return;
                        }
                    } else if (reply == JOptionPane.NO_OPTION) {
                    } else {
                        return;
                    }
                }
                EditorFrame.this.dispose();
                setVisible(false);
            }
        });
    }

    public boolean askFileName() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.showSaveDialog(this);
        File f = jFileChooser.getSelectedFile();
        logger.debug(f);
        if (f != null) {
            if (f.exists()) {
                if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this, f.getName() + " already exists. Are you sure to overwrite this file?", "File exists!", JOptionPane.YES_NO_OPTION)) {
                    return false;
                }
            }
            try {
                setFile(VFS.resolveFile(f.getAbsolutePath()));
                return true;
            } catch (Exception ex) {
                logger.error(ex, ex);
            }
        }
        return false;
    }

    public boolean saveFile() {
        try {
            logger.debug("Saving: " + file.getPath());
            OutputStream out = file.getOutputStream();
            String text = textArea.getText();
            out.write(text.getBytes());
            out.close();
            lastSaved = text;
            return true;
        } catch (Exception ex) {
            logger.warn(ex, ex);
            UIUtils.showErrorDialog(this, ex, "Error saving file");
        }
        return false;
    }

    public void loadFile(AbstractFile file) throws IOException, FileSystemException {
        setFile(file);
        BufferedReader reader = null;
        try {
            String line;
            StringBuffer cnt = new StringBuffer();
            reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(file.getInputStream())));
            line = reader.readLine();
            while (line != null) {
                cnt.append(line);
                line = reader.readLine();
                if (line != null) {
                    cnt.append('\n');
                }
            }
            lastSaved = cnt.toString();
            textArea.setText("");
            textArea.setEditable(true);
            textArea.insertText(lastSaved);
            textArea.setEditable(editable);
            textArea.setCaretPosition(0);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
	* Returns the value of file.
	* @return the file value.
	*/
    public AbstractFile getFile() {
        return file;
    }

    /**
	* Sets the value of file.
	* @param file The new value for file.
	*/
    public void setFile(AbstractFile file) {
        this.file = file;
    }

    public static void main(String args[]) {
        new EditorFrame(true).show();
    }

    public JComponent getTopComponent() {
        return top;
    }

    public void addTopComponent(JComponent c) {
        getContentPane().add(c);
    }

    public String getName() {
        return file.getName();
    }

    public Window getWindow() {
        return this;
    }
}
