package de.michabrandt.timeview.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.michabrandt.timeview.common.Dialog;
import de.michabrandt.timeview.renderer.StreamRenderer;

public abstract class ExportDialog extends JDialog {

    protected Preferences userPrefs = TimeLineGUI.getUserPrefs();

    protected Dialog dlg = null;

    protected JFileChooser fileExportChooser;

    protected FileFilter fileFilter;

    private JTextField fileField;

    private JButton fileButton;

    protected File file;

    protected JButton defaultButton;

    protected JButton exportButton;

    protected JButton cancelButton;

    public ExportDialog(JFrame owner) {
        super(owner);
        this.setTitle("Export");
    }

    public void init() {
        this.build();
        this.pack();
    }

    public void setDialog(Dialog dlg) {
        this.dlg = dlg;
    }

    private void initComponents() {
        fileExportChooser = new JFileChooser();
        fileExportChooser.setAcceptAllFileFilterUsed(false);
        fileField = new JTextField();
        fileField.setEditable(false);
        fileButton = new JButton("...", TimeLineGUI.readImageIcon("folder_explore.png"));
        ActionListener alFileChoose = new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                chooseFile();
            }
        };
        fileButton.addActionListener(alFileChoose);
        defaultButton = new JButton("Restore defaults", TimeLineGUI.readImageIcon("action_refresh.png"));
        exportButton = new JButton("Export", TimeLineGUI.readImageIcon("accept.png"));
        cancelButton = new JButton("Cancel", TimeLineGUI.readImageIcon("cancel.png"));
        ActionListener alDefault = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doRestoreDefaults();
            }
        };
        ActionListener alExport = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (file.exists()) {
                    int yes = JOptionPane.showConfirmDialog(null, "The file \"" + file.getName() + "\" already exists. Overwrite it?", "Configm Selected File", JOptionPane.YES_NO_OPTION);
                    if (yes == JOptionPane.YES_OPTION) doExport();
                } else {
                    doExport();
                }
            }
        };
        ActionListener alCancel = new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                doCancel();
            }
        };
        defaultButton.addActionListener(alDefault);
        exportButton.addActionListener(alExport);
        cancelButton.addActionListener(alCancel);
    }

    private void build() {
        this.initComponents();
        FormLayout layout = new FormLayout("pref:grow", "p, 9dlu, top:default:grow, 12dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        CellConstraints cc = new CellConstraints();
        builder.add(buildFileBar(), cc.xy(1, 1));
        builder.add(buildPanel(), cc.xy(1, 3));
        builder.add(buildButtons(), cc.xy(1, 5));
        this.add(builder.getPanel());
        this.setResizable(false);
    }

    private JComponent buildFileBar() {
        FormLayout layout = new FormLayout("right:pref, 3dlu, 300px:grow, 3dlu, pref", "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator("Destination", cc.xyw(1, 1, 5));
        builder.addLabel("File", cc.xy(1, 3));
        builder.add(fileField, cc.xy(3, 3));
        builder.add(fileButton, cc.xy(5, 3));
        return builder.getPanel();
    }

    private JComponent buildButtons() {
        ButtonBarBuilder builder = new ButtonBarBuilder();
        builder.addGridded(defaultButton);
        builder.addGlue();
        builder.addUnrelatedGap();
        builder.addGriddedButtons(new JButton[] { exportButton, cancelButton });
        return builder.getPanel();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = new File(file.getPath().replaceFirst("\\.?xml$", "") + this.getFileSuffix());
        this.fileField.setText(this.file.getPath());
    }

    private void doCancel() {
        this.setVisible(false);
    }

    private void chooseFile() {
        fileExportChooser.addChoosableFileFilter(fileFilter);
        fileExportChooser.setSelectedFile(getFile());
        int state = fileExportChooser.showSaveDialog(null);
        while (state == JFileChooser.APPROVE_OPTION) {
            file = fileExportChooser.getSelectedFile();
            fileField.setText(file.getPath());
        }
        fileExportChooser.removeChoosableFileFilter(fileFilter);
    }

    protected abstract Component buildPanel();

    protected abstract String getFileSuffix();

    protected abstract void doRestoreDefaults();

    protected abstract void saveUserPrefs();

    protected abstract StreamRenderer createRenderer(Dialog dlg);

    private void doExport() {
        if (this.dlg != null) {
            try {
                StreamRenderer renderer = createRenderer(dlg);
                System.out.println(file.getPath());
                FileOutputStream fos;
                fos = new FileOutputStream(file);
                renderer.render(fos);
                fos.close();
                saveUserPrefs();
                System.gc();
                Runtime.getRuntime().freeMemory();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
            this.dlg = null;
        }
        setVisible(false);
    }
}
