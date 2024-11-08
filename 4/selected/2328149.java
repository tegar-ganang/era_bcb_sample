package tit.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import tit.configuration.Configurations;
import tit.gui.border.HorizontalLine;
import tit.gui.icon.Icons;
import tit.gui.layout.VerticalLayout;
import tit.gui.widget.BehaviourDialog;
import tit.gui.widget.ScrollDownScrollPane;
import tit.observation.Observable;
import tit.observation.ObservationTableModel;
import tit.observation.behaviour.Behaviour;
import tit.observation.io.ObservationDOMWriter;
import tit.observation.io.ObservationReader;
import tit.observation.io.ObservationTITReader;
import tit.observation.io.ObservationXMLReader;
import tit.summary.AttributeNotSetException;
import tit.summary.Summary;
import tit.summary.SuperSummarizer;
import tit.summary.io.SummaryWriter;
import tit.summary.io.SummaryXMLWriter;
import tit.summary.personality.Personality;
import tit.summary.personality.PersonalityEditor;
import tit.utility.Dates;
import tit.utility.Extensions;

/**
 * An observation-editor.
 * @author Bart Sas
 */
public class EditObservation extends JInternalFrame implements ActionListener, InternalFrameListener, TableModelListener, ChangeListener, MouseListener {

    /**
     * The observation.
     */
    private ObservationTableModel observation;

    /**
     * The table in which the observation is displayed.
     */
    private JTable table;

    /**
     * The table scrollbars.
     */
    private ScrollDownScrollPane scrollbars;

    /**
     * The personality-editor.
     */
    private PersonalityEditor personality;

    /**
     * The save-button.
     */
    private JButton save;

    /**
     * The save as-button.
     */
    private JButton saveas;

    /**
     * The summarize-button.
     */
    private JButton summarize;

    /**
     * The file we are editing.
     */
    private File file;

    /**
     * The dirty flag.
     */
    private boolean dirty;

    /**
     * The popup menu.
     */
    private JPopupMenu popup;

    /**
     * The add behaviour-popup menu item.
     */
    private JMenuItem addbehaviour;

    /**
     * The remove behaviour-popup menu item.
     */
    private JMenuItem removebehaviour;

    /**
     * Constructs a new <code>EditObservation</code>.
     * @param originalobservation The observation to edit.
     * @param originalpersonality The personality to edit.
     * @throws IOException Is thrown when something goes wrong while reading the input.
     */
    public EditObservation(Observable originalobservation, Personality originalpersonality) throws IOException {
        super("Edit observation", true, true, true, true);
        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(this);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(content);
        JPanel top = new JPanel(new BorderLayout(2, 2));
        top.setBorder(new HorizontalLine());
        content.add(top, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(1, 0, 2, 2));
        top.add(buttons, BorderLayout.WEST);
        save = new JButton("Save", Icons.getSaveIcon());
        save.addActionListener(this);
        buttons.add(save);
        saveas = new JButton("Save as", Icons.getSaveIcon());
        saveas.addActionListener(this);
        buttons.add(saveas);
        summarize = new JButton("Summarize", Icons.getExportIcon());
        summarize.addActionListener(this);
        buttons.add(summarize);
        JPanel left = new JPanel(new BorderLayout());
        observation = new ObservationTableModel();
        try {
            originalobservation.produceObservation(observation);
        } catch (Exception exception) {
            throw new IOException("Couldn't read observation");
        }
        observation.addTableModelListener(this);
        table = new JTable(observation);
        table.addMouseListener(this);
        scrollbars = new ScrollDownScrollPane(table);
        scrollbars.setAutoScrollDown(false);
        left.add(scrollbars, BorderLayout.CENTER);
        JPanel right = new JPanel(new VerticalLayout(4));
        personality = new PersonalityEditor(originalpersonality);
        personality.setBorder(new TitledBorder("Personality"));
        personality.addChangeListener(this);
        right.add(personality);
        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        center.setDividerLocation(2.0 / 3.0);
        center.setResizeWeight(2.0 / 3.0);
        content.add(center, BorderLayout.CENTER);
        popup = new JPopupMenu();
        addbehaviour = new JMenuItem("Add behaviour");
        addbehaviour.addActionListener(this);
        popup.add(addbehaviour);
        removebehaviour = new JMenuItem("Remove selected behaviours");
        removebehaviour.addActionListener(this);
        popup.add(removebehaviour);
        file = null;
        dirty = false;
    }

    /**
     * Constructs a new <code>EditObservation</code>.
     * @param reader The <code>ObservationXMLReader</code> from which the input is read.
     * @throws IOException Is thrown when something goes wrong while reading the input.
     */
    public EditObservation(ObservationReader reader) throws IOException {
        this(reader.getObservation(), reader.getPersonality());
    }

    /**
     * @param fileinit The <code>File</code> from which the input is read.
     * @throws IOException Is thrown when something goes wrong while reading the input.
     * @throws SAXException Is thrown when something goes wrong while reading the input.
     * @throws ParserConfigurationException Is thrown when something goes wrong while reading the input.
     */
    public EditObservation(File fileinit) throws IOException, SAXException, ParserConfigurationException {
        this(new ObservationXMLReader(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new FileReader(fileinit)))));
        file = fileinit;
    }

    /**
     * Reads an (old) tit-file.
     * @param titfile The tit-file.
     * @return An <code>EditObservation</code> instance.
     * @throws IOException Is thrown when something goes wrong while reading the tit file.
     */
    public static EditObservation readTITFile(File titfile) throws IOException {
        return new EditObservation(new ObservationTITReader(new LineNumberReader(new FileReader(titfile))));
    }

    /**
     * Saves the observation.
     */
    private void saveObservation() {
        if (file == null) {
            saveObservationAs();
        } else {
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = builder.newDocument();
                ObservationDOMWriter domwriter = new ObservationDOMWriter(document, personality);
                observation.produceObservation(domwriter);
                Writer output = new FileWriter(file);
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                Source source = new DOMSource(document);
                Result result = new StreamResult(output);
                transformer.transform(source, result);
                dirty = false;
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, "An error occurred while saving the file: " + exception.toString());
            }
        }
    }

    /**
     * Saves the observation.
     */
    private void saveObservationAs() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(Extensions.XTIT_FILE_FILTER);
            try {
                StringBuilder titname = new StringBuilder();
                titname.append(personality.getRN());
                titname.append("_");
                titname.append(personality.getSEQ());
                titname.append("_");
                titname.append(Dates.SAVE_DATE_FORMAT.format(personality.getDATE()));
                titname.append(".xtit");
                chooser.setSelectedFile(new File(titname.toString()));
            } catch (AttributeNotSetException exception) {
            }
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedfile = chooser.getSelectedFile();
                if (!selectedfile.exists() || JOptionPane.showConfirmDialog(this, selectedfile.getName() + " already exists. Overwriter file?", "File already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document document = builder.newDocument();
                    ObservationDOMWriter domwriter = new ObservationDOMWriter(document, personality);
                    observation.produceObservation(domwriter);
                    Writer output = new FileWriter(file = selectedfile);
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    Source source = new DOMSource(document);
                    Result result = new StreamResult(output);
                    transformer.transform(source, result);
                    dirty = false;
                }
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "An error occurred while saving the file: " + exception.toString());
        }
    }

    /**
     * Summarizes the observation.
     */
    private void summarizeObservation() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(Extensions.XSUM_FILE_FILTER);
            try {
                StringBuilder titname = new StringBuilder();
                titname.append(personality.getRN());
                titname.append("_");
                titname.append(personality.getSEQ());
                titname.append("_");
                titname.append(Dates.SAVE_DATE_FORMAT.format(personality.getDATE()));
                titname.append(".xsum");
                chooser.setSelectedFile(new File(titname.toString()));
            } catch (AttributeNotSetException exception) {
            }
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedfile = chooser.getSelectedFile();
                if (!selectedfile.exists() || JOptionPane.showConfirmDialog(this, selectedfile.getName() + " already exists. Overwriter file?", "File already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    SuperSummarizer summarizer = new SuperSummarizer(Configurations.getSummaryIntervals(), Configurations.getDefaultFlyTime());
                    observation.produceObservation(summarizer);
                    SummaryWriter writer = new SummaryXMLWriter(selectedfile);
                    Summary[] summaries = summarizer.getSummaries();
                    for (int i = 0; i < summaries.length; i++) {
                        writer.writeSummary(summaries[i], personality);
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
     * Closes this window.
     */
    private void closeWindow() {
        if (dirty) {
            switch(JOptionPane.showConfirmDialog(this, "The observation has been modified. Save changes?", "Save changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                case JOptionPane.YES_OPTION:
                    saveObservation();
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
     * Adds a behaviour to the table.
     */
    private void addBehaviour() {
        try {
            Behaviour behaviour = BehaviourDialog.showBehaviourDialog(this);
            if (behaviour != null) {
                observation.addBehaviour(behaviour);
                dirty = true;
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Could not add behaviour: " + exception.toString(), "Operation failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Removes all the selected behaviours in the table.
     */
    private void removeBehaviour() {
        int[] rows = table.getSelectedRows();
        Arrays.sort(rows);
        try {
            for (int i = rows.length - 1; i >= 0; i--) {
                observation.removeBehaviour(rows[i]);
                dirty = true;
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Could not remove behaviour: " + exception.toString(), "Operation failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles <code>ActionEvent</code>s.
     * @param event The associated <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == save) {
            saveObservation();
        } else if (event.getSource() == saveas) {
            saveObservationAs();
        } else if (event.getSource() == summarize) {
            summarizeObservation();
        } else if (event.getSource() == addbehaviour) {
            addBehaviour();
        } else if (event.getSource() == removebehaviour) {
            removeBehaviour();
        }
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
     * Invoked when a table model has been changed.
     * @param event The associated <code>TableModelEvent</code>.
     */
    public void tableChanged(TableModelEvent event) {
        if (event.getSource() == observation) {
            dirty = true;
        }
    }

    /**
     * Invoked when the target of the listener has changed its state.
     * @param event The associated <code>ChangeEvent</code>.
     */
    public void stateChanged(ChangeEvent event) {
        if (event.getSource() == personality) {
            dirty = true;
        }
    }

    /**
     * Invoked when the mouse button has been clicked (pressed and released) on a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mouseClicked(MouseEvent event) {
    }

    /**
     * Invoked when the mouse enters a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mouseEntered(MouseEvent event) {
    }

    /**
     * Invoked when the mouse exits a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mouseExited(MouseEvent event) {
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mousePressed(MouseEvent event) {
        if (event.isPopupTrigger() && observation.isStopped()) {
            popup.show(table, event.getX(), event.getY());
        }
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mouseReleased(MouseEvent event) {
        if (event.isPopupTrigger() && observation.isStopped()) {
            popup.show(table, event.getX(), event.getY());
        }
    }
}
