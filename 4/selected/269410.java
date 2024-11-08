package tit.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import tit.configuration.Configurations;
import tit.gui.border.HorizontalLine;
import tit.gui.icon.Icons;
import tit.observation.io.ObservationReader;
import tit.observation.io.ObservationXMLReader;
import tit.summary.Summary;
import tit.summary.SuperSummarizer;
import tit.summary.complete.AdjustableCompleteSummary;
import tit.summary.complete.CompleteSummary;
import tit.summary.complete.CompleteSummaryRecord;
import tit.summary.complete.CompleteSummaryTableModel;
import tit.summary.complete.SummaryPersonalityCombination;
import tit.summary.io.SummaryCSVWriter;
import tit.summary.io.SummarySASWriter;
import tit.summary.io.SummaryTreeModel;
import tit.summary.io.SummaryWriter;
import tit.summary.io.SummaryXMLReader;
import tit.summary.io.SummaryXMLWriter;
import tit.summary.personality.Personality;
import tit.utility.Extensions;

/**
 * A summary-editor.
 * @author Bart Sas
 */
public class EditSummary extends JInternalFrame implements InternalFrameListener, ActionListener, TreeSelectionListener {

    /**
     * An empty summary.
     */
    private static final AdjustableCompleteSummary EMPTY_SUMMARY = new CompleteSummaryRecord();

    /**
     * The file we are editing.
     */
    private File file;

    /**
     * The dirty flag.
     */
    private boolean dirty;

    /**
     * The save-button
     */
    private JButton save;

    /**
     * The save as-button
     */
    private JButton saveas;

    /**
     * The export as csv button
     */
    private JButton exportcsv;

    /**
     * The export as sas button
     */
    private JButton exportsas;

    /**
     * The list model for the list.
     */
    private SummaryTreeModel summaries;

    /**
     * The load summary-button.
     */
    private JButton loadsummary;

    /**
     * The load observationy-button.
     */
    private JButton loadobservation;

    /**
     * The remove-button.
     */
    private JButton remove;

    /**
     * The tree in which the summaries are displayed.
     */
    private JTree tree;

    /**
     * The table model.
     */
    private CompleteSummaryTableModel model;

    /**
     * The table.
     */
    private JTable table;

    /**
     * creates a new <code>EditSummary</code>.
     */
    public EditSummary() {
        super("Edit summary", true, true, true, true);
        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(this);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(content);
        JPanel top = new JPanel(new BorderLayout(2, 2));
        top.setBorder(new HorizontalLine());
        content.add(top, BorderLayout.NORTH);
        JPanel iobuttons = new JPanel(new GridLayout(1, 0, 2, 2));
        top.add(iobuttons, BorderLayout.WEST);
        save = new JButton("Save", Icons.getSaveIcon());
        save.addActionListener(this);
        iobuttons.add(save);
        saveas = new JButton("Save as", Icons.getSaveIcon());
        saveas.addActionListener(this);
        iobuttons.add(saveas);
        exportcsv = new JButton("Export CSV", Icons.getExportIcon());
        exportcsv.addActionListener(this);
        iobuttons.add(exportcsv);
        exportsas = new JButton("Export SAS", Icons.getExportIcon());
        exportsas.addActionListener(this);
        iobuttons.add(exportsas);
        JPanel left = new JPanel(new BorderLayout(8, 8));
        summaries = new SummaryTreeModel();
        tree = new JTree(summaries);
        tree.addTreeSelectionListener(this);
        left.add(new JScrollPane(tree), BorderLayout.CENTER);
        JPanel editbuttons = new JPanel(new GridLayout(1, 0, 4, 4));
        left.add(editbuttons, BorderLayout.SOUTH);
        loadsummary = new JButton("Load summary", Icons.getAddIcon());
        loadsummary.addActionListener(this);
        editbuttons.add(loadsummary);
        loadobservation = new JButton("Load observation", Icons.getAddIcon());
        loadobservation.addActionListener(this);
        editbuttons.add(loadobservation);
        remove = new JButton("Remove from list", Icons.getRemoveIcon());
        remove.addActionListener(this);
        editbuttons.add(remove);
        model = new CompleteSummaryTableModel(EMPTY_SUMMARY);
        table = new JTable(model);
        table.setEnabled(false);
        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, new JScrollPane(table));
        center.setDividerLocation(1.0 / 2.0);
        center.setResizeWeight(1.0 / 3.0);
        content.add(center);
        file = null;
        dirty = false;
    }

    /**
     * Invoked when an internal frame is activated.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameActivated(InternalFrameEvent event) {
    }

    /**
     * Invoked when an internal frame has been closed.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameClosed(InternalFrameEvent event) {
    }

    /**
     * Saves this summary.
     */
    private void saveSummary() {
        if (file == null) {
            saveSummaryAs();
        } else {
            try {
                SummaryWriter writer = new SummaryXMLWriter(file);
                Iterator<CompleteSummary> iterator = summaries.getAllSummaries().iterator();
                while (iterator.hasNext()) {
                    writer.writeSummary(iterator.next());
                }
                writer.closeWriter();
                dirty = false;
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, "An error occurred while summarizing the file: " + exception.toString());
                exception.printStackTrace();
            }
        }
    }

    /**
     * Saves this summary.
     */
    private void saveSummaryAs() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(Extensions.XSUM_FILE_FILTER);
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedfile = chooser.getSelectedFile();
                if (!selectedfile.exists() || JOptionPane.showConfirmDialog(this, selectedfile.getName() + " already exists. Overwriter file?", "File already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    SummaryWriter writer = new SummaryXMLWriter(file = selectedfile);
                    Iterator<CompleteSummary> iterator = summaries.getAllSummaries().iterator();
                    while (iterator.hasNext()) {
                        writer.writeSummary(iterator.next());
                    }
                    writer.closeWriter();
                    dirty = false;
                }
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "An error occurred while summarizing the file: " + exception.toString());
            exception.printStackTrace();
        }
    }

    /**
     * Saves this summary.
     */
    private void exportSummaryAsCSV() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(Extensions.CSV_FILE_FILTER);
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedfile = chooser.getSelectedFile();
                if (!selectedfile.exists() || JOptionPane.showConfirmDialog(this, selectedfile.getName() + " already exists. Overwriter file?", "File already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    SummaryWriter writer = new SummaryCSVWriter(new PrintStream(selectedfile), Configurations.getCSVSeparator(), Configurations.getCSVQuotation());
                    Iterator<CompleteSummary> iterator = summaries.getAllSummaries().iterator();
                    while (iterator.hasNext()) {
                        writer.writeSummary(iterator.next());
                    }
                    writer.closeWriter();
                }
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "An error occurred while summarizing the file: " + exception.toString());
            exception.printStackTrace();
        }
    }

    /**
     * Saves this summary.
     */
    private void exportSummaryAsSAS() {
        try {
            String name = JOptionPane.showInputDialog(this, "Enter a name for the observation", "Enter a name", JOptionPane.QUESTION_MESSAGE);
            if (name != null) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(Extensions.SAS_FILE_FILTER);
                if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File selectedfile = chooser.getSelectedFile();
                    if (!selectedfile.exists() || JOptionPane.showConfirmDialog(this, selectedfile.getName() + " already exists. Overwriter file?", "File already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        SummaryWriter writer = new SummarySASWriter(new PrintStream(selectedfile), name);
                        Iterator<CompleteSummary> iterator = summaries.getAllSummaries().iterator();
                        while (iterator.hasNext()) {
                            writer.writeSummary(iterator.next());
                        }
                        writer.closeWriter();
                    }
                }
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "An error occurred while summarizing the file: " + exception.toString());
            exception.printStackTrace();
        }
    }

    /**
     * Closes this window.
     */
    private void closeWindow() {
        if (dirty) {
            switch(JOptionPane.showConfirmDialog(this, "The observation has been modified. Save changes?", "Save changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                case JOptionPane.YES_OPTION:
                    saveSummary();
                    if (!dirty) dispose();
                    break;
                case JOptionPane.NO_OPTION:
                    dispose();
                    break;
                case JOptionPane.CANCEL_OPTION:
                default:
                    break;
            }
        } else {
            dispose();
        }
    }

    /**
     * Loads a new summary and adds it to the rest.
     */
    private void loadSummary() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(Extensions.XSUM_FILE_FILTER);
            chooser.setMultiSelectionEnabled(true);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File[] selected = chooser.getSelectedFiles();
                for (int i = 0; i < selected.length; i++) {
                    summaries.addSummaries(selected[i].getAbsolutePath(), new SummaryXMLReader(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new FileReader(selected[i])))));
                }
                dirty = true;
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Couldn't open observation: " + exception.toString(), "Couldn't load message", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads a new summary and adds it to the rest.
     */
    private void loadObservation() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(Extensions.XTIT_FILE_FILTER);
            chooser.setMultiSelectionEnabled(true);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File[] selected = chooser.getSelectedFiles();
                for (int i = 0; i < selected.length; i++) {
                    ObservationReader observation = new ObservationXMLReader(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new FileReader(selected[i]))));
                    SuperSummarizer summarizer = new SuperSummarizer(Configurations.getSummaryIntervals(), Configurations.getDefaultFlyTime());
                    observation.getObservation().produceObservation(summarizer);
                    Personality personality = observation.getPersonality();
                    Summary[] summary = summarizer.getSummaries();
                    CompleteSummary[] completesummaries = new CompleteSummary[summary.length];
                    for (int j = 0; j < summary.length; j++) {
                        completesummaries[j] = new SummaryPersonalityCombination(summary[j], personality);
                    }
                    summaries.addSummaries(selected[i].getAbsolutePath(), completesummaries);
                    dirty = true;
                }
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Couldn't open observation: " + exception.toString(), "Couldn't load message", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Removes a summary from the list.
     */
    private void removeSummary() {
        try {
            TreePath[] paths = tree.getSelectionPaths();
            for (int i = 0; i < paths.length; i++) {
                summaries.removeNode(paths[i].getPathComponent(paths[i].getPathCount() - 1));
                dirty = true;
            }
        } catch (IllegalArgumentException exception) {
        }
    }

    /**
     * Handles <code>ActionEvent</code>s.
     * @param event The associated <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == save) {
            saveSummary();
        } else if (event.getSource() == saveas) {
            saveSummaryAs();
        } else if (event.getSource() == exportcsv) {
            exportSummaryAsCSV();
        } else if (event.getSource() == exportsas) {
            exportSummaryAsSAS();
        } else if (event.getSource() == loadsummary) {
            loadSummary();
        } else if (event.getSource() == loadobservation) {
            loadObservation();
        }
        if (event.getSource() == remove) {
            removeSummary();
        }
    }

    /**
     * Invoked when an internal frame is in the process of being closed.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameClosing(InternalFrameEvent event) {
        if (event.getSource() == this) {
            closeWindow();
        }
    }

    /**
     * Invoked when an internal frame is de-activated.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameDeactivated(InternalFrameEvent event) {
    }

    /**
     * Invoked when an internal frame is de-iconified.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameDeiconified(InternalFrameEvent event) {
    }

    /**
     * Invoked when an internal frame is iconified.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameIconified(InternalFrameEvent event) {
    }

    /**
     * Invoked when a internal frame has been opened.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameOpened(InternalFrameEvent event) {
    }

    /**
     * Called whenever the value of the selection changes.
     * @param event The associated <code>TreeSelectionEvent</code>.
     */
    public void valueChanged(TreeSelectionEvent event) {
        for (int i = 0; i < event.getPath().getPathCount(); i++) {
            try {
                model.loadSummary((AdjustableCompleteSummary) event.getPath().getPathComponent(event.getPath().getPathCount() - 1));
                table.setEnabled(true);
            } catch (ClassCastException exception) {
                table.setEnabled(false);
                model.loadSummary(EMPTY_SUMMARY);
            } catch (IllegalArgumentException exception) {
                table.setEnabled(false);
                model.loadSummary(EMPTY_SUMMARY);
            }
        }
    }
}
