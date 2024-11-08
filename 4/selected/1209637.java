package fiswidgets.fiswizard;

import fiswidgets.fisgui.*;
import fiswidgets.fisutils.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;

/**
  * FisWizard is a meta-fiswidget that can be used to create a wizard from
  * FisWidgets that have already been created.  First and last text-only pages
  * are optional, but at least one FisWidget must be included.
  */
public class FisWizard extends JFrame implements ActionListener, WindowListener {

    private final String XML_desc = "XML serialized file (*.gui)";

    private final String OLD_desc = "Java serialized file (*.*)";

    private final String XML_suff = ".gui";

    private final String OLD_suff = "";

    private GridBagLayout gbl;

    private GridBagConstraints gbc;

    private JPanel holder;

    private JPanel controls;

    private int currentpage = 1;

    private Vector fisbases = new Vector();

    private boolean standalone = true;

    private JMenuItem about;

    private JMenu help;

    private boolean firstPageExists = false, lastPageExists = false;

    private JPanel firstPagePanel, lastPagePanel;

    private JButton next, back;

    /**
    * the default constructor
    */
    public FisWizard() {
        super("Wizard");
        addWindowListener(this);
        Container pane = getContentPane();
        JScrollPane scroll = new JScrollPane();
        JViewport port = scroll.getViewport();
        holder = new JPanel();
        holder.setLayout(new FlowLayout());
        port.add(holder);
        pane.setLayout(new BorderLayout());
        pane.add(scroll, BorderLayout.CENTER);
        controls = new JPanel();
        controls.setBorder(new LineBorder(Color.black));
        pane.add(controls, BorderLayout.SOUTH);
        controls.setLayout(new FlowLayout());
        back = new JButton("<- Back");
        back.setActionCommand("back");
        back.addActionListener(this);
        back.setEnabled(false);
        controls.add(back);
        JButton cancel = new JButton("Cancel");
        cancel.setActionCommand("Exit");
        cancel.addActionListener(this);
        controls.add(cancel);
        next = new JButton("Next ->");
        next.setActionCommand("next");
        next.addActionListener(this);
        controls.add(next);
        JMenuBar menubar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem save = new JMenuItem("Save");
        save.setActionCommand("save");
        save.addActionListener(this);
        file.add(save);
        JMenuItem load = new JMenuItem("Load");
        load.setActionCommand("load");
        load.addActionListener(this);
        file.add(load);
        file.addSeparator();
        JMenuItem exit = new JMenuItem("Exit");
        file.add(exit);
        exit.addActionListener(this);
        menubar.add(file);
        help = new JMenu("Help");
        JMenuItem helpitem = new JMenuItem("Help");
        helpitem.addActionListener(this);
        help.add(helpitem);
        help.addSeparator();
        help.setEnabled(false);
        about = new JMenuItem("About");
        about.addActionListener(this);
        help.add(about);
        menubar.add(Box.createHorizontalGlue());
        menubar.add(help);
        setJMenuBar(menubar);
    }

    /**
     *  A call to display must be done at the end of the addPages so the wizard will create and show itself.
     */
    public void display() {
        holder.removeAll();
        if (firstPageExists) {
            holder.add(firstPagePanel);
        } else {
            FisBase fb = (FisBase) (fisbases.elementAt(0));
            holder.add(fb.getComponentPanel());
            if (fisbases.size() == 1 && !lastPageExists) {
                next.setText("Finished");
                next.setActionCommand("finished");
            }
        }
        pack();
        show();
        setLocation(100, 100);
    }

    /**
     *  This is used by the about message dialog to get the about message for the fisapp
     */
    public String getAboutMessage() {
        return (new FisBase()).getAboutMessage();
    }

    /**
     * addFirstPage will make the message sent to it appear as the first page of the wizard.
     * @param message is the string that will appear as the first page in the wizard.  This string should contain any wanted line breaks.
     */
    public void addFirstPage(String message) {
        firstPageExists = true;
        JTextArea ta = new JTextArea(message);
        JScrollPane scroll = new JScrollPane();
        JViewport port = scroll.getViewport();
        firstPagePanel = new JPanel();
        ta.setColumns(30);
        ta.setRows(20);
        port.add(ta);
        firstPagePanel.setLayout(new BorderLayout());
        firstPagePanel.add(scroll, BorderLayout.CENTER);
    }

    /**
     * addLastPage will make the message sent to it appear as the last page of the wizard.
     * @param message is the string that will appear as the last page in the wizard.  This string should contain any wanted line breaks.
     */
    public void addLastPage(String message) {
        lastPageExists = true;
        JTextArea ta = new JTextArea(message);
        JScrollPane scroll = new JScrollPane();
        JViewport port = scroll.getViewport();
        lastPagePanel = new JPanel();
        ta.setColumns(30);
        ta.setRows(20);
        port.add(ta);
        lastPagePanel.setLayout(new BorderLayout());
        lastPagePanel.add(scroll, BorderLayout.CENTER);
    }

    private void pageChange() {
        holder.removeAll();
        FisBase fb;
        if (firstPageExists) fb = (FisBase) (fisbases.elementAt(currentpage - 2)); else fb = (FisBase) (fisbases.elementAt(currentpage - 1));
        holder.add(fb.getComponentPanel());
        pack();
        show();
    }

    private boolean makeFisBase(String className) {
        FisBase tempBase;
        try {
            tempBase = (FisBase) ((Class.forName(className)).newInstance());
            tempBase.dispose();
            tempBase.setStandAlone(false);
            fisbases.addElement(tempBase);
            return true;
        } catch (Exception ex) {
            System.out.println("Could not find class for " + className);
            return false;
        }
    }

    /**
     * This can be used to place your own menuitem into the help menu.
     * @param helpItem a JMenuItem to be used as the Help menu item.
     */
    public void setHelpItem(JMenuItem helpItem) {
        help.removeAll();
        help.add(helpItem);
        help.add(new JSeparator());
        help.add(about);
        help.setEnabled(true);
    }

    /**
    * addPage is used to add a fiswidget to the wizard
    * @param className is the class name of the fiswidget to be added
    * @param buttonlabel is the String that will appear on the toolchest button
    */
    public void addPage(String className) {
        if (!makeFisBase(className)) System.out.println("did not find: " + className);
    }

    private void runIt() {
        next.setEnabled(false);
        WizardRun wr = new WizardRun(fisbases, this);
    }

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        if (action.equals("Exit")) {
            if (standalone) System.exit(0); else dispose();
        } else if (action.equals("next")) {
            currentpage++;
            back.setEnabled(true);
            if (lastPageExists) {
                if ((currentpage == fisbases.size() + 2 && firstPageExists) || (currentpage == fisbases.size() + 1 && !firstPageExists)) {
                    holder.removeAll();
                    holder.add(lastPagePanel);
                    pack();
                    show();
                    next.setText("Finished");
                    next.setActionCommand("finished");
                    return;
                }
            } else if ((currentpage == fisbases.size() + 1 && firstPageExists) || (currentpage == fisbases.size() && !firstPageExists)) {
                next.setText("Finished");
                next.setActionCommand("finished");
            }
            pageChange();
        } else if (action.equals("back")) {
            currentpage--;
            next.setText("Next ->");
            next.setActionCommand("next");
            next.setEnabled(true);
            if (currentpage == 1) {
                back.setEnabled(false);
                if (firstPageExists) {
                    holder.removeAll();
                    holder.add(firstPagePanel);
                    pack();
                    show();
                    return;
                }
            }
            pageChange();
        } else if (action.equals("finished")) {
            int ans = JOptionPane.showConfirmDialog(this, "All of the applications that have been set up by the wizard will now be run.\nDo you want to continue?", "Continue?", JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) runIt();
        } else if (action.equals("Help")) {
            FisProperties props = new FisProperties();
            try {
                props.loadProperties();
            } catch (Exception ex) {
                return;
            }
            if (!props.hasProperty("BROWSER") || !props.hasProperty("FISDOC_PATH")) return;
            String browser = System.getProperty("BROWSER");
            String docpath = System.getProperty("FISDOC_PATH");
            try {
                Runtime.getRuntime().exec(browser + " " + docpath + "/wizard.html");
            } catch (Exception ex) {
                return;
            }
        } else if (action.equals("About")) {
            AboutDialog ad = new AboutDialog(this);
        } else if (action.equals("save")) {
            saveIt();
        } else if (action.equals("load")) {
            loadIt();
        }
    }

    private void saveIt() {
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FisFileFilter(OLD_suff, OLD_desc));
        chooser.addChoosableFileFilter(new FisFileFilter(XML_suff, XML_desc));
        int r = chooser.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            String t = chooser.getSelectedFile().getAbsolutePath();
            File f = new File(t);
            boolean overwrite = true;
            if (f.exists()) {
                int ans = JOptionPane.showConfirmDialog(this, "The file already exists, overwrite?", "Are you sure?", JOptionPane.YES_NO_OPTION);
                if (ans == JOptionPane.YES_OPTION) overwrite = false;
            }
            if (overwrite) {
                if (chooser.getFileFilter().getDescription().equals(XML_desc)) {
                    if (!t.endsWith(XML_suff)) t = t + XML_suff;
                    FisBase[] bases = new FisBase[fisbases.size()];
                    for (int i = 0; i < bases.length; i++) bases[i] = (FisBase) fisbases.elementAt(i);
                    XmlSerializationFactory sf = new XmlSerializationFactory(this, t);
                    try {
                        sf.saveWizard(bases);
                        sf = null;
                    } catch (Exception ex) {
                        Dialogs.ShowErrorDialog(this, "Error saving file!");
                    }
                } else {
                    SerializationFactory sf = new SerializationFactory(t);
                    try {
                        sf.saveWizard(fisbases);
                        sf = null;
                    } catch (Exception ex) {
                        Dialogs.ShowErrorDialog(this, "Error saving file!");
                    }
                }
            }
        }
    }

    private void loadIt() {
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FisFileFilter(OLD_suff, OLD_desc));
        chooser.addChoosableFileFilter(new FisFileFilter(XML_suff, XML_desc));
        int r = chooser.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            String t = chooser.getSelectedFile().getAbsolutePath();
            if (chooser.getFileFilter().getDescription().equals(XML_desc)) {
                FisBase[] bases = new FisBase[fisbases.size()];
                for (int i = 0; i < bases.length; i++) bases[i] = (FisBase) fisbases.elementAt(i);
                XmlSerializationFactory sf = new XmlSerializationFactory(this, t);
                try {
                    sf.loadWizard(bases);
                    sf = null;
                } catch (Exception ex) {
                    Dialogs.ShowErrorDialog(this, "Error opening file!");
                }
            } else {
                SerializationFactory sf = new SerializationFactory(t);
                try {
                    sf.loadWizard(fisbases);
                    sf = null;
                } catch (Exception ex) {
                    Dialogs.ShowErrorDialog(this, "Error opening file!");
                }
            }
        }
    }

    /**
     * setStandAlone is used to realize if this app is a standalone app or not
     */
    public void setStandAlone(boolean alone) {
        standalone = alone;
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        if (standalone) System.exit(0); else dispose();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    class WizardRun extends Thread {

        private Vector allBases = new Vector();

        private FisBase tempbase;

        private FisWizard wizard;

        public WizardRun(Vector b, FisWizard f) {
            allBases = b;
            wizard = f;
            start();
        }

        public void run() {
            String tempstring, breakstring;
            int val;
            FisRunManager manager;
            try {
                for (int x = 0; x < allBases.size(); x++) {
                    tempbase = (FisBase) allBases.elementAt(x);
                    tempbase.doRunClick();
                    manager = tempbase.getThread();
                    manager.join();
                }
                next.setEnabled(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(wizard, "Major error encountered while running the wizard.", "Error", JOptionPane.ERROR_MESSAGE);
                next.setEnabled(true);
            }
        }
    }
}
